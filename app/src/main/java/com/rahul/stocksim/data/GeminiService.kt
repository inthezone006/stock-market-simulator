package com.rahul.stocksim.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {
    // Free tier API Key should be handled securely, but for a sim project we might use a placeholder or the same as Finnhub if it were a proxy
    private val apiKey = "AIzaSyDWFHtxhxx3zVIVfnVFalMGiv6dcSz-YF0" // User needs to provide this or I'll use a placeholder
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
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
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            response.text ?: "Could not generate analysis."
        } catch (e: Exception) {
            "AI Analysis unavailable: ${e.message}"
        }
    }
}
