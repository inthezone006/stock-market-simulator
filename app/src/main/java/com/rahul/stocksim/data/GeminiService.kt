package com.rahul.stocksim.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rahul.stocksim.BuildConfig
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    
    // Ordered list of models to try. 3.1 Flash Lite has the highest RPD (500).
    private val models = listOf(
        GenerativeModel(modelName = "gemini-3.1-flash-lite-preview", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-2.5-flash-lite", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey),
        GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = apiKey),
        GenerativeModel(modelName = "gemma-3-1b-it", apiKey = apiKey),
        GenerativeModel(modelName = "gemma-3-4b-it", apiKey = apiKey)
    )

    suspend fun generateStockAnalysis(
        stock: Stock,
        news: List<FinnhubNewsArticle>,
        financials: FinnhubFinancialsResponse?
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext "AI Analysis unavailable: Gemini API Key is missing. Please add it to your local.properties or gradle.properties."
        }

        val prompt = """
            Analyze the stock ${stock.symbol} (${stock.name}).
            Current Price: ${'$'}${stock.price} (${stock.percentChange}% change today).
            
            Latest News Headlines:
            ${news.take(5).joinToString("\n") { "- " + it.headline }}
            
            Financial Metrics:
            ${financials?.metric?.entries?.take(10)?.joinToString("\n") { "${it.key}: ${it.value}" }}
            
            Provide a concise summary (max 150 words) including:
            1. Current Sentiment.
            2. Key Risks.
            3. Outlook for the next month.
            
            Use Markdown formatting like **bold** for emphasis on key terms.
        """.trimIndent()

        var lastException: Exception? = null

        for (model in models) {
            try {
                val response = model.generateContent(prompt)
                val text = response.text
                if (text != null) return@withContext text
            } catch (e: Exception) {
                lastException = e
                // If it's a fatal key error, stop trying
                if (e.message?.contains("API_KEY_INVALID") == true) break
                // Log the attempt failure to help debugging which models are failing
                FirebaseCrashlytics.getInstance().log("Model ${model.modelName} failed: ${e.message}")
                continue
            }
        }

        // If we reach here, all models failed
        val e = lastException ?: return@withContext "The AI was unable to generate a response."
        
        // Log the final exception to Firebase
        if (e !is kotlinx.coroutines.CancellationException && e !is java.net.SocketTimeoutException) {
            FirebaseCrashlytics.getInstance().apply {
                recordException(e)
                setCustomKey("gemini_stock", stock.symbol)
                setCustomKey("gemini_error_msg", e.message ?: "unknown")
            }
        }

        val errorMsg = e.message ?: ""
        when {
            errorMsg.contains("404") -> "AI Insights are currently unavailable (Model not found)."
            errorMsg.contains("429") -> "Quota exceeded on all available models. Please try again later."
            errorMsg.contains("API_KEY_INVALID") -> "Invalid API Key. Please check your Gemini configuration."
            else -> "AI Analysis is temporarily unavailable. ${e.message}"
        }
    }
}
