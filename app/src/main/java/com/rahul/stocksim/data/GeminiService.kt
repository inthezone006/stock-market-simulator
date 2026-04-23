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
    
    // Stable models for Free Tier
    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey
    )

    private val fallbackModel = GenerativeModel(
        modelName = "gemini-3-flash-lite",
        apiKey = apiKey
    )

    suspend fun generateStockAnalysis(
        stock: Stock,
        news: List<FinnhubNewsArticle>,
        financials: FinnhubFinancialsResponse?
    ): String = withContext(Dispatchers.IO) {
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

        if (apiKey.isEmpty()) {
            return@withContext "AI Analysis unavailable: Gemini API Key is missing. Please add it to your local.properties or gradle.properties."
        }

        try {
            val response = try {
                model.generateContent(prompt)
            } catch (e: Exception) {
                // If the primary model fails (e.g., 404 or transient error), try the fallback
                fallbackModel.generateContent(prompt)
            }
            response.text ?: "The AI was unable to generate a response. Please try again in a few moments."
        } catch (e: Exception) {
            // Log the exception to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                recordException(e)
                setCustomKey("gemini_stock", stock.symbol)
                setCustomKey("gemini_error_msg", e.message ?: "unknown")
            }

            val errorMsg = e.message ?: ""
            when {
                errorMsg.contains("404") -> "AI Insights are currently unavailable."
                errorMsg.contains("429") -> "Rate limit reached. Please wait a minute before requesting another analysis."
                errorMsg.contains("API_KEY_INVALID") -> "Invalid API Key. Please check your Gemini configuration."
                else -> "AI Analysis is temporarily unavailable. (${errorMsg.take(60)}...)"
            }
        }
    }
}
