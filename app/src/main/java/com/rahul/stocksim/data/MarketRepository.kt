package com.rahul.stocksim.data

import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin

// Interface for Finnhub API
interface FinnhubApi {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubQuoteResponse

    @GET("search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("token") apiKey: String
    ): FinnhubSearchResponse

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): List<FinnhubNewsArticle>

    @GET("stock/candle")
    suspend fun getStockCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") apiKey: String
    ): FinnhubCandleResponse
}

data class FinnhubQuoteResponse(
    val c: Double, // Current price
    val d: Double, // Change
    val dp: Double, // Percent change
    val h: Double,
    val l: Double,
    val o: Double,
    val pc: Double
)

data class FinnhubSearchResponse(
    val count: Int,
    val result: List<FinnhubSearchResult>
)

data class FinnhubSearchResult(
    val description: String,
    val displaySymbol: String,
    val symbol: String,
    val type: String
)

data class FinnhubNewsArticle(
    val category: String,
    val datetime: Long,
    val headline: String,
    val id: Long,
    val image: String,
    val related: String,
    val source: String,
    val summary: String,
    val url: String
)

data class FinnhubCandleResponse(
    val c: List<Double>?, // Close prices
    val h: List<Double>?, // High prices
    val l: List<Double>?, // Low prices
    val o: List<Double>?, // Open prices
    val s: String,        // Status
    val t: List<Long>?,   // Timestamps
    val v: List<Long>?    // Volume
)

data class StockPricePoint(val timestamp: Long, val price: Double)

data class WatchlistItem(val symbol: String)

class MarketRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics
    
    private val apiKey = "d38davhr01qlbdj4vutgd38davhr01qlbdj4vuu0"

    // Simple in-memory cache to stay within Finnhub free tier limits (60 calls/min)
    private val quoteCache = ConcurrentHashMap<String, Pair<Stock, Long>>()
    private val CACHE_EXPIRATION_MS = 300_000L // 5 minutes
    
    // Mutex to throttle outgoing requests
    private val apiMutex = Mutex()
    private var lastRequestTime = 0L
    private val MIN_DELAY_MS = 100L

    companion object {
        private var globalWatchlistCache: List<Stock>? = null
        private var globalPortfolioCache: List<Pair<Stock, Long>>? = null
        
        // Persist company names to ensure symbols show full names instead of ticker duplicates
        private val companyNameMap = ConcurrentHashMap<String, String>().apply {
            // Seed with common symbols to ensure they show up correctly even before first search
            put("AAPL", "Apple Inc.")
            put("GOOGL", "Alphabet Inc.")
            put("MSFT", "Microsoft Corp.")
            put("AMZN", "Amazon.com Inc.")
            put("TSLA", "Tesla Inc.")
            put("META", "Meta Platforms Inc.")
            put("NVDA", "NVIDIA Corp.")
            put("NFLX", "Netflix Inc.")
            put("AMD", "Advanced Micro Devices Inc.")
            put("PYPL", "PayPal Holdings Inc.")
            put("INTC", "Intel Corp.")
            put("CSCO", "Cisco Systems Inc.")
            put("ADBE", "Adobe Inc.")
            put("CRM", "Salesforce Inc.")
            put("QCOM", "Qualcomm Inc.")
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        crashlytics.log(message)
        Log.d("API_LOG", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://finnhub.io/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FinnhubApi::class.java)

    suspend fun getStockQuote(symbol: String): Stock? {
        val cached = quoteCache[symbol]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRATION_MS) {
            return cached.first
        }

        return apiMutex.withLock {
            val reCached = quoteCache[symbol]
            if (reCached != null && System.currentTimeMillis() - reCached.second < CACHE_EXPIRATION_MS) {
                return reCached.first
            }

            val timeSinceLast = System.currentTimeMillis() - lastRequestTime
            if (timeSinceLast < MIN_DELAY_MS) {
                delay(MIN_DELAY_MS - timeSinceLast)
            }

            try {
                val response = api.getQuote(symbol, apiKey)
                lastRequestTime = System.currentTimeMillis()
                
                // If name is missing in map, try a quick search to find it (don't block for long)
                if (!companyNameMap.containsKey(symbol)) {
                    coroutineScope {
                        launch {
                            try {
                                val searchRes = api.searchSymbol(symbol, apiKey)
                                val match = searchRes.result.find { it.symbol == symbol }
                                if (match != null) companyNameMap[symbol] = match.description
                            } catch (e: Exception) {}
                        }
                    }
                }

                val stock = Stock(
                    symbol = symbol,
                    name = companyNameMap[symbol] ?: symbol,
                    price = response.c,
                    change = response.d,
                    percentChange = response.dp,
                    high = response.h,
                    low = response.l,
                    open = response.o,
                    prevClose = response.pc
                )
                quoteCache[symbol] = stock to System.currentTimeMillis()
                stock
            } catch (e: Exception) {
                crashlytics.recordException(e)
                cached?.first
            }
        }
    }

    suspend fun getStocksQuotes(symbols: List<String>): List<Stock> = coroutineScope {
        symbols.map { symbol ->
            async { getStockQuote(symbol) }
        }.awaitAll().filterNotNull()
    }

    suspend fun getWatchlistWithQuotes(forceRefresh: Boolean = false): List<Stock> {
        if (!forceRefresh && globalWatchlistCache != null) {
            return globalWatchlistCache!!
        }

        val symbols = getWatchlist().map { it.symbol }
        if (symbols.isEmpty()) return emptyList()

        val stocks = getStocksQuotes(symbols)
        
        if (stocks.isNotEmpty()) {
            globalWatchlistCache = stocks
        }
        return globalWatchlistCache ?: emptyList()
    }

    suspend fun getPortfolioWithQuotes(forceRefresh: Boolean = false): List<Pair<Stock, Long>> {
        if (!forceRefresh && globalPortfolioCache != null) {
            return globalPortfolioCache!!
        }

        val rawPortfolio = getPortfolio()
        if (rawPortfolio.isEmpty()) return emptyList()

        val symbols = rawPortfolio.map { it.first }
        val stocks = getStocksQuotes(symbols)
        
        val portfolioWithQuotes = rawPortfolio.mapNotNull { (symbol, qty) ->
            val stock = stocks.find { it.symbol == symbol }
            if (stock != null) stock to qty else null
        }
        
        if (portfolioWithQuotes.isNotEmpty()) {
            globalPortfolioCache = portfolioWithQuotes
        }
        return globalPortfolioCache ?: emptyList()
    }

    suspend fun getStockHistory(symbol: String, period: String): List<StockPricePoint> {
        val to = Instant.now().epochSecond
        val from = when (period) {
            "1D" -> to - (24 * 3600)
            "1W" -> to - (7 * 24 * 3600)
            "1M" -> to - (30 * 24 * 3600)
            "1Y" -> to - (365 * 24 * 3600)
            else -> to - (30 * 24 * 3600)
        }
        
        val resolution = when (period) {
            "1D" -> "D"
            "1W" -> "D"
            "1M" -> "D"
            "1Y" -> "W"
            else -> "D"
        }

        return try {
            val response = api.getStockCandles(symbol, resolution, from, to, apiKey)
            if (response.s == "ok" && response.c != null && response.t != null) {
                response.t.zip(response.c).map { StockPricePoint(it.first, it.second) }
            } else {
                val quote = getStockQuote(symbol)
                if (quote != null) {
                    val simulatedQuote = FinnhubQuoteResponse(
                        c = quote.price, d = quote.change, dp = quote.percentChange,
                        h = quote.high, l = quote.low, o = quote.open, pc = quote.prevClose
                    )
                    generateSimulatedPoints(simulatedQuote, to)
                } else emptyList()
            }
        } catch (e: Exception) {
            try {
                val quote = getStockQuote(symbol)
                if (quote != null) {
                    val simulatedQuote = FinnhubQuoteResponse(
                        c = quote.price, d = quote.change, dp = quote.percentChange,
                        h = quote.high, l = quote.low, o = quote.open, pc = quote.prevClose
                    )
                    generateSimulatedPoints(simulatedQuote, to)
                } else emptyList()
            } catch (innerEx: Exception) {
                crashlytics.recordException(innerEx)
                emptyList()
            }
        }
    }

    private fun generateSimulatedPoints(quote: FinnhubQuoteResponse, endTime: Long): List<StockPricePoint> {
        val points = mutableListOf<StockPricePoint>()
        val startPrice = quote.o
        val endPrice = quote.c
        val high = quote.h
        val low = quote.l
        
        for (i in 0..10) {
            val timestamp = endTime - ((10 - i) * 3600)
            val noise = (sin(i.toDouble() * 1.5) * (high - low) * 0.15)
            
            val basePrice = when {
                i == 0 -> startPrice
                i == 10 -> endPrice
                i < 4 -> startPrice + (low - startPrice) * (i / 4.0)
                i < 8 -> low + (high - low) * ((i - 4) / 4.0)
                else -> high + (endPrice - high) * ((i - 8) / 2.0)
            }
            
            points.add(StockPricePoint(timestamp, basePrice + noise))
        }
        return points
    }

    suspend fun getCompanyNews(symbol: String): List<FinnhubNewsArticle> {
        return try {
            val today = java.time.LocalDate.now().toString()
            val weekAgo = java.time.LocalDate.now().minusDays(7).toString()
            api.getCompanyNews(symbol, weekAgo, today, apiKey).take(5)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            emptyList()
        }
    }

    suspend fun searchStocks(query: String, nasdaqOnly: Boolean = false): List<Stock> = coroutineScope {
        try {
            val response = api.searchSymbol(query, apiKey)
            
            val filteredResults = response.result
                .filter { result ->
                    val isStock = result.type == "Common Stock" || result.type == "ADR"
                    val isNasdaq = !nasdaqOnly || result.symbol.all { it.isLetter() }
                    isStock && isNasdaq
                }
                .take(10)

            filteredResults.map { result ->
                companyNameMap[result.symbol] = result.description
                
                getStockQuote(result.symbol)?.let { quote ->
                    Stock(
                        symbol = result.symbol,
                        name = result.description,
                        price = quote.price,
                        change = quote.change,
                        percentChange = quote.percentChange,
                        high = quote.high,
                        low = quote.low,
                        open = quote.open,
                        prevClose = quote.prevClose
                    )
                }
            }.filterNotNull()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWatchlist(): List<WatchlistItem> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("watchlist").get().await()
            snapshot.documents.map { WatchlistItem(it.getString("symbol") ?: "") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addToWatchlist(symbol: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId)
                .collection("watchlist").document(symbol)
                .set(mapOf("symbol" to symbol)).await()
            
            val bundle = Bundle().apply { putString(FirebaseAnalytics.Param.ITEM_ID, symbol) }
            analytics.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, bundle)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(symbol: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId)
                .collection("watchlist").document(symbol)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buyStock(symbol: String, quantity: Int, pricePerShare: Double): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalCost = quantity * pricePerShare

        return try {
            val userRef = firestore.collection("users").document(userId)
            val portfolioRef = userRef.collection("portfolio").document(symbol)
            
            firestore.runTransaction { transaction ->
                val userSnapshot = transaction.get(userRef)
                val portfolioDoc = transaction.get(portfolioRef)
                
                val currentBalance = userSnapshot.getDouble("balance") ?: 0.0

                if (currentBalance >= totalCost) {
                    transaction.update(userRef, "balance", currentBalance - totalCost)
                    
                    if (portfolioDoc.exists()) {
                        val currentQty = portfolioDoc.getLong("quantity") ?: 0L
                        transaction.update(portfolioRef, "quantity", currentQty + quantity)
                    } else {
                        transaction.set(portfolioRef, mapOf("quantity" to quantity.toLong(), "symbol" to symbol))
                    }
                    null
                } else {
                    throw Exception("Insufficient balance")
                }
            }.await()
            
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.CURRENCY, "USD")
                putDouble(FirebaseAnalytics.Param.VALUE, totalCost)
                putString(FirebaseAnalytics.Param.TRANSACTION_ID, symbol)
            }
            analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
            
            globalPortfolioCache = null
            
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun sellStock(symbol: String, quantity: Int, pricePerShare: Double): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalGain = quantity * pricePerShare

        return try {
            val userRef = firestore.collection("users").document(userId)
            val portfolioRef = userRef.collection("portfolio").document(symbol)

            firestore.runTransaction { transaction ->
                val portfolioDoc = transaction.get(portfolioRef)
                val userSnapshot = transaction.get(userRef)
                
                val currentQty = portfolioDoc.getLong("quantity") ?: 0L

                if (currentQty >= quantity) {
                    val currentBalance = userSnapshot.getDouble("balance") ?: 0.0
                    transaction.update(userRef, "balance", currentBalance + totalGain)
                    
                    transaction.update(portfolioRef, "quantity", currentQty - quantity)
                    null
                } else {
                    throw Exception("Insufficient quantity")
                }
            }.await()
            
            globalPortfolioCache = null

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserBalance(): Flow<Double> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        while(true) {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                emit(snapshot.getDouble("balance") ?: 0.0)
            } catch (e: Exception) {
                crashlytics.recordException(e)
                emit(0.0)
            }
            kotlinx.coroutines.delay(5000)
        }
    }

    suspend fun getPortfolio(): List<Pair<String, Long>> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("portfolio").get().await()
            snapshot.documents.map { 
                it.getString("symbol").orEmpty() to (it.getLong("quantity") ?: 0L)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
