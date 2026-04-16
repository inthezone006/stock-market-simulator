package com.rahul.stocksim.data

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query as FirestoreQuery
import com.rahul.stocksim.data.local.StockDao
import com.rahul.stocksim.data.local.entity.StockEntity
import com.rahul.stocksim.data.local.entity.*
import com.rahul.stocksim.model.*
import com.rahul.stocksim.util.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import com.google.firebase.Timestamp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin
import kotlin.math.abs

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

    @GET("crypto/candle")
    suspend fun getCryptoCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") apiKey: String
    ): FinnhubCandleResponse

    @GET("forex/candle")
    suspend fun getForexCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") apiKey: String
    ): FinnhubCandleResponse

    @GET("stock/profile2")
    suspend fun getCompanyProfile(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubProfileResponse

    @GET("stock/metric")
    suspend fun getBasicFinancials(
        @Query("symbol") symbol: String,
        @Query("metric") metric: String = "all",
        @Query("token") apiKey: String
    ): FinnhubFinancialsResponse

    @GET("news")
    suspend fun getMarketNews(
        @Query("category") category: String = "general",
        @Query("token") apiKey: String
    ): List<FinnhubNewsArticle>

    @GET("stock/recommendation")
    suspend fun getRecommendations(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<FinnhubRecommendationResponse>

    @GET("stock/peers")
    suspend fun getPeers(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<String>

    @GET("calendar/earnings")
    suspend fun getEarningsCalendar(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): FinnhubEarningsCalendarResponse

    @GET("indicator")
    suspend fun getTechnicalIndicator(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("indicator") indicator: String,
        @Query("token") apiKey: String,
        @Query("timeperiod") timeperiod: Int? = null
    ): FinnhubIndicatorResponse

    @GET("stock/dividend")
    suspend fun getDividends(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): List<FinnhubDividendResponse>

    @GET("calendar/ipo")
    suspend fun getIpoCalendar(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String
    ): FinnhubIpoCalendarResponse

    @GET("news-sentiment")
    suspend fun getNewsSentiment(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubNewsSentimentResponse

    @GET("forex/rates")
    suspend fun getForexRates(
        @Query("base") base: String = "USD",
        @Query("token") apiKey: String
    ): FinnhubForexRatesResponse

    @GET("stock/market-status")
    suspend fun getMarketStatus(
        @Query("exchange") exchange: String,
        @Query("token") apiKey: String
    ): FinnhubMarketStatusResponse

    @GET("stock/esg")
    suspend fun getEsgScores(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubEsgResponse

    @GET("stock/price-target")
    suspend fun getPriceTarget(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): FinnhubPriceTargetResponse

    @GET("stock/earnings")
    suspend fun getEarningsSurprises(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String
    ): List<FinnhubEarningsSurpriseResponse>
}

data class FinnhubQuoteResponse(val c: Double, val d: Double, val dp: Double, val h: Double, val l: Double, val o: Double, val pc: Double)
data class FinnhubSearchResponse(val count: Int, val result: List<FinnhubSearchResult>)
data class FinnhubSearchResult(val description: String, val displaySymbol: String, val symbol: String, val type: String)
data class FinnhubSymbolResult(val description: String, val displaySymbol: String, val symbol: String)
data class FinnhubNewsArticle(val category: String, val datetime: Long, val headline: String, val id: Long, val image: String, val related: String, val source: String, val summary: String, val url: String)
data class FinnhubCandleResponse(val c: List<Double>?, val h: List<Double>?, val l: List<Double>?, val o: List<Double>?, val s: String, val t: List<Long>?, val v: List<Long>?)
data class FinnhubProfileResponse(val country: String?, val currency: String?, val exchange: String?, val name: String?, val ticker: String?, val logo: String?, val marketCapitalization: Double?, val finnhubIndustry: String?, val shareOutstanding: Double?)
data class FinnhubFinancialsResponse(val symbol: String, val metric: Map<String, Any?>?)
data class FinnhubRecommendationResponse(val buy: Int, val hold: Int, val period: String, val sell: Int, val strongBuy: Int, val strongSell: Int, val symbol: String)
data class FinnhubEarningsCalendarResponse(val earningsCalendar: List<FinnhubEarningsEntry>)
data class FinnhubEarningsEntry(val date: String, val epsActual: Double?, val epsEstimate: Double?, val hour: String, val quarter: Int, val symbol: String, val year: Int)
data class FinnhubIndicatorResponse(val rsi: List<Double>?, val macd: List<Double>?, val macdSignal: List<Double>?, val macdHist: List<Double>?, val sma: List<Double>?, val ema: List<Double>?, val s: String)
data class FinnhubDividendResponse(val symbol: String, val date: String, val amount: Double, val adjustedAmount: Double, val payDate: String, val recordDate: String, val declarationDate: String, val currency: String)
data class FinnhubIpoCalendarResponse(val ipoCalendar: List<FinnhubIpoEntry>)
data class FinnhubIpoEntry(val date: String, val exchange: String, val name: String, val numberOfShares: Long, val price: String, val status: String, val symbol: String, val totalSharesValue: Long)
data class FinnhubNewsSentimentResponse(val buzz: FinnhubBuzz?, val companyNewsScore: Double?, val sectorAverageBullishPercent: Double?, val sectorAverageNewsScore: Double?, val sentiment: FinnhubSentiment?, val symbol: String)
data class FinnhubBuzz(val articlesInLastWeek: Int?, val buzz: Double?, val weeklyAverage: Double?)
data class FinnhubSentiment(val bearishPercent: Double?, val bullishPercent: Double?)
data class FinnhubForexRatesResponse(val base: String, val code: String? = null, val quote: Map<String, Double>? = null)
data class FinnhubMarketStatusResponse(val exchange: String, val holiday: String?, val isOpen: Boolean, val session: String?, val timezone: String?)
data class FinnhubEsgResponse(val symbol: String, val totalScore: Double?, val environmentScore: Double?, val socialScore: Double?, val governanceScore: Double?, val data: Map<String, Any?>?)
data class FinnhubPriceTargetResponse(val symbol: String, val targetHigh: Double?, val targetLow: Double?, val targetMean: Double?, val targetMedian: Double?, val lastUpdate: String?)
data class FinnhubEarningsSurpriseResponse(val actual: Double?, val estimate: Double?, val period: String?, val symbol: String?, val surprise: Double?, val surprisePercent: Double?)

data class StockPricePoint(
    val timestamp: Long,
    val price: Double,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val volume: Long = 0
)
data class WatchlistItem(val symbol: String)
data class AIRecommendation(val advice: String, val confidence: Int, val reasons: List<String>)
enum class AssetFilter { STOCKS, CRYPTO, FOREX, OTHERS }

@Singleton
class MarketRepository @Inject constructor(
    private val api: FinnhubApi,
    private val stockDao: StockDao,
    @ApplicationContext private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics
    private val notificationHelper = NotificationHelper(context)
    
    private val apiKey = "d38davhr01qlbdj4vutgd38davhr01qlbdj4vuu0"

    fun getApplicationContext() = context

    private val quoteCache = ConcurrentHashMap<String, Pair<Stock, Long>>()
    private val CACHE_EXPIRATION_MS = 300_000L // 5 minutes
    
    private val apiMutex = Mutex()
    private var lastRequestTime = 0L
    private val MIN_DELAY_MS = 100L

    companion object {
        private var globalWatchlistCache: List<Stock>? = null
        private var globalPortfolioCache: List<Pair<Stock, Long>>? = null
        
        private val industryCache = ConcurrentHashMap<String, String>()
        private val companyNameMap = ConcurrentHashMap<String, String>().apply {
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
            put("SPY", "SPDR S&P 500 ETF Trust")
        }

        private val cryptoSymbols = setOf("BINANCE:BTCUSDT", "BINANCE:ETHUSDT", "BINANCE:XRPUSDT", "BINANCE:SOLUSDT")
        private val forexSymbols = setOf("OANDA:EUR_USD", "OANDA:GBP_USD", "OANDA:USD_JPY")
    }

    private fun recordError(e: Exception) {
        if (e is HttpException && e.code() == 401) {
            crashlytics.log("Finnhub API 401 Unauthorized: Check API Key validity. Current key: ${apiKey.take(5)}...")
            crashlytics.setCustomKey("api_auth_error", true)
        }
        crashlytics.recordException(e)
    }

    suspend fun getStockQuote(symbol: String): Stock? {
        val cached = quoteCache[symbol]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_EXPIRATION_MS) {
            return cached.first
        }

        // Try Room database for offline cache if network fails
        return try {
            apiMutex.withLock {
                val reCached = quoteCache[symbol]
                if (reCached != null && System.currentTimeMillis() - reCached.second < CACHE_EXPIRATION_MS) {
                    return reCached.first
                }

                val timeSinceLast = System.currentTimeMillis() - lastRequestTime
                if (timeSinceLast < MIN_DELAY_MS) {
                    delay(MIN_DELAY_MS - timeSinceLast)
                }

                val response = api.getQuote(symbol, apiKey)
                lastRequestTime = System.currentTimeMillis()
                
                if (!companyNameMap.containsKey(symbol) || !industryCache.containsKey(symbol)) {
                    try {
                        val profileRes = api.getCompanyProfile(symbol, apiKey)
                        if (profileRes.name != null) companyNameMap[symbol] = profileRes.name
                        if (profileRes.finnhubIndustry != null) industryCache[symbol] = profileRes.finnhubIndustry!!
                    } catch (e: Exception) {}
                }

                val isCrypto = symbol.startsWith("BINANCE:") || cryptoSymbols.contains(symbol)
                val isForex = symbol.startsWith("OANDA:") || forexSymbols.contains(symbol)
                
                val stock = Stock(
                    symbol = symbol,
                    name = companyNameMap[symbol] ?: symbol,
                    price = response.c,
                    change = response.d,
                    percentChange = response.dp,
                    high = response.h,
                    low = response.l,
                    open = response.o,
                    prevClose = response.pc,
                    isCrypto = isCrypto,
                    isForex = isForex,
                    industry = industryCache[symbol]
                )
                
                // Update caches
                quoteCache[symbol] = stock to System.currentTimeMillis()
                stockDao.insertStock(stock.toEntity())
                
                stock
            }
        } catch (e: Exception) {
            recordError(e)
            // Offline fallback
            stockDao.getStock(symbol)?.toDomain() ?: cached?.first
        }
    }

    private fun Stock.toEntity() = StockEntity(
        symbol = symbol, name = name, price = price, change = change, percentChange = percentChange,
        high = high, low = low, open = open, prevClose = prevClose, isCrypto = isCrypto,
        isForex = isForex, industry = industry
    )

    private fun StockEntity.toDomain() = Stock(
        symbol = symbol, name = name, price = price, change = change, percentChange = percentChange,
        high = high, low = low, open = open, prevClose = prevClose, isCrypto = isCrypto,
        isForex = isForex, industry = industry
    )

    suspend fun getStocksQuotes(symbols: List<String>): List<Stock> = coroutineScope {
        symbols.map { symbol ->
            async {
                try {
                    withTimeout(15000) {
                        getStockQuote(symbol)
                    }
                } catch (e: Exception) {
                    Log.e("MarketRepository", "Error fetching quote for $symbol", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getWatchlistWithQuotes(forceRefresh: Boolean = false): List<Stock> {
        if (!forceRefresh && globalWatchlistCache != null) {
            return globalWatchlistCache!!
        }
        val symbols = getWatchlist().map { it.symbol }
        if (symbols.isEmpty()) return emptyList()
        return try {
            val stocks = getStocksQuotes(symbols)
            if (stocks.isNotEmpty()) globalWatchlistCache = stocks
            globalWatchlistCache ?: emptyList()
        } catch (e: Exception) {
            recordError(e)
            globalWatchlistCache ?: emptyList()
        }
    }

    suspend fun getPortfolioWithQuotes(forceRefresh: Boolean = false): List<Pair<Stock, Long>> {
        if (!forceRefresh && globalPortfolioCache != null) return globalPortfolioCache!!
        return try {
            val rawPortfolio = getPortfolio()
            if (rawPortfolio.isEmpty()) return emptyList()
            val stocks = getStocksQuotes(rawPortfolio.map { it.first })
            val portfolioWithQuotes = rawPortfolio.mapNotNull { (symbol, qty) ->
                val stock = stocks.find { it.symbol == symbol }
                if (stock != null) stock to qty else null
            }
            if (portfolioWithQuotes.isNotEmpty()) globalPortfolioCache = portfolioWithQuotes
            globalPortfolioCache ?: emptyList()
        } catch (e: Exception) {
            recordError(e)
            globalPortfolioCache ?: emptyList()
        }
    }

    suspend fun getStockHistory(symbol: String, period: String): List<StockPricePoint> {
        val to = Instant.now().epochSecond
        val from = when (period) {
            "1D" -> to - (24 * 3600)
            "5D" -> to - (5 * 24 * 3600)
            "1W" -> to - (7 * 24 * 3600)
            "1M" -> to - (30 * 24 * 3600)
            "6M" -> to - (180 * 24 * 3600)
            "1Y" -> to - (365 * 24 * 3600)
            "5Y" -> to - (5 * 365 * 24 * 3600)
            else -> to - (30 * 24 * 3600)
        }
        val resolution = when (period) {
            "1D" -> "15"
            "5D" -> "60"
            "1W" -> "60"
            "1M" -> "D"
            "6M" -> "D"
            "1Y" -> "W"
            "5Y" -> "M"
            else -> "D"
        }

        val isCrypto = symbol.startsWith("BINANCE:") || cryptoSymbols.contains(symbol)
        val isForex = symbol.startsWith("OANDA:") || forexSymbols.contains(symbol)

        return try {
            val response = when {
                isCrypto -> api.getCryptoCandles(symbol, resolution, from, to, apiKey)
                isForex -> api.getForexCandles(symbol, resolution, from, to, apiKey)
                else -> api.getStockCandles(symbol, resolution, from, to, apiKey)
            }
            if (response.s == "ok" && !response.c.isNullOrEmpty() && !response.t.isNullOrEmpty()) {
                response.t.indices.map { i ->
                    StockPricePoint(
                        timestamp = response.t[i],
                        price = response.c[i],
                        open = response.o?.getOrNull(i) ?: response.c[i],
                        high = response.h?.getOrNull(i) ?: response.c[i],
                        low = response.l?.getOrNull(i) ?: response.c[i],
                        volume = response.v?.getOrNull(i) ?: 0L
                    )
                }.sortedBy { it.timestamp }
            } else {
                // Fallback to simulation if no data or status not ok
                getStockQuote(symbol)?.let { generateSimulatedPoints(symbol, FinnhubQuoteResponse(it.price, it.change, it.percentChange, it.high, it.low, it.open, it.prevClose), to, period) } ?: emptyList()
            }
        } catch (e: Exception) {
            recordError(e)
            // Fallback to simulation on network error
            getStockQuote(symbol)?.let { generateSimulatedPoints(symbol, FinnhubQuoteResponse(it.price, it.change, it.percentChange, it.high, it.low, it.open, it.prevClose), to, period) } ?: emptyList()
        }
    }

    private fun generateSimulatedPoints(symbol: String, quote: FinnhubQuoteResponse, endTime: Long, period: String): List<StockPricePoint> {
        val points = mutableListOf<StockPricePoint>()
        
        // Use available quote data for variance
        val high = if (quote.h > 0) quote.h else quote.c * 1.05
        val low = if (quote.l > 0) quote.l else quote.c * 0.95
        val open = if (quote.o > 0) quote.o else quote.pc.takeIf { it > 0 } ?: (quote.c * 0.98)
        val close = quote.c
        
        val steps = when(period) {
            "1D" -> 24 
            "5D" -> 40 // 8 points per day for 5 days
            "1W" -> 35
            "1M" -> 30 
            "6M" -> 120 // More points for 6M
            "1Y" -> 52 
            "5Y" -> 60 
            else -> 30
        }

        val intervalSeconds = when(period) { 
            "1D" -> 3600L 
            "5D" -> (3600L * 24 * 5) / steps
            "1W" -> (3600L * 24 * 7) / steps
            "1M" -> (3600L * 24 * 30) / steps
            "6M" -> (3600L * 24 * 180) / steps
            "1Y" -> (3600L * 24 * 365) / steps
            "5Y" -> (3600L * 24 * 365 * 5) / steps
            else -> 3600L * 24
        }

        // Use a stable seed based on symbol and period to keep history consistent across refreshes
        val seed = symbol.hashCode().toLong() + period.hashCode().toLong()
        val random = Random(seed)

        if (period == "1D") {
            // For 1D charts, strictly follow Open -> (High/Low) -> Close path to touch all key stats
            val isHighFirst = random.nextBoolean()
            val step1 = steps / 3
            val step2 = 2 * steps / 3
            
            val keySteps = listOf(0, step1, step2, steps)
            val keyPrices = if (isHighFirst) listOf(open, high, low, close) else listOf(open, low, high, close)

            for (i in 0 until 3) {
                val sStart = keySteps[i]
                val sEnd = keySteps[i+1]
                val pStart = keyPrices[i]
                val pEnd = keyPrices[i+1]
                
                for (step in sStart until sEnd) {
                    val progress = (step - sStart).toDouble() / (sEnd - sStart)
                    val trend = pStart + (pEnd - pStart) * progress
                    // Add some jitter but keep within daily bounds
                    val noise = (random.nextDouble() - 0.5) * (high - low) * 0.15
                    val price = (trend + noise).coerceIn(low, high)
                    
                    points.add(StockPricePoint(
                        timestamp = endTime - ((steps - step) * intervalSeconds),
                        price = price,
                        open = price * (1 + (random.nextDouble() - 0.5) * 0.002),
                        high = price * (1 + random.nextDouble() * 0.002),
                        low = price * (1 - random.nextDouble() * 0.002),
                        volume = (1000..50000).random().toLong()
                    ))
                }
            }
            // Add final point exactly at current price
            points.add(StockPricePoint(
                timestamp = endTime,
                price = close,
                open = close,
                high = close,
                low = close,
                volume = (1000..50000).random().toLong()
            ))
        } else {
            // For other periods, use a trend-based simulation
            // For 5D/1M/6M/1Y, the daily 'open' is not the correct starting point for the whole period.
            // We'll estimate a starting price based on a random walk backwards.
            val periodVariance = when(period) {
                "5D" -> 0.05
                "1W" -> 0.07
                "1M" -> 0.12
                "6M" -> 0.25
                "1Y" -> 0.40
                "5Y" -> 0.80
                else -> 0.10
            }
            
            val startPrice = close * (1.0 + (random.nextDouble() - 0.5) * periodVariance)
            
            for (i in 0..steps) {
                val timestamp = endTime - ((steps - i) * intervalSeconds)
                val progress = i.toDouble() / steps
                
                // Add some sinusoidal movement for more realistic stock behavior
                val sineWave = sin(progress * Math.PI * 2) * (close * periodVariance * 0.2)
                val trend = startPrice + (close - startPrice) * progress
                val noise = (random.nextDouble() - 0.5) * close * (periodVariance * 0.1)
                val price = (trend + sineWave + noise).coerceAtLeast(0.01)
                
                points.add(StockPricePoint(
                    timestamp = timestamp,
                    price = if (i == steps) close else price,
                    open = price * (1 + (random.nextDouble() - 0.5) * 0.005),
                    high = price * (1 + random.nextDouble() * 0.005),
                    low = price * (1 - random.nextDouble() * 0.005),
                    volume = (1000..50000).random().toLong()
                ))
            }
        }

        return points
    }

    suspend fun getCompanyNews(symbol: String): List<FinnhubNewsArticle> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val today = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            api.getCompanyNews(symbol, sdf.format(calendar.time), today, apiKey).take(5)
        } catch (e: Exception) {
            recordError(e)
            emptyList()
        }
    }

    suspend fun searchStocks(query: String, filter: AssetFilter = AssetFilter.STOCKS): List<Stock> = coroutineScope {
        try {
            val response = api.searchSymbol(query, apiKey)
            val filteredResults = response.result.filter { result ->
                when (filter) {
                    AssetFilter.STOCKS -> (result.type == "Common Stock" || result.type == "ADR" || result.type == "ETF") && result.symbol.all { it.isLetter() }
                    AssetFilter.CRYPTO -> result.symbol.startsWith("BINANCE:") || cryptoSymbols.contains(result.symbol)
                    AssetFilter.FOREX -> result.symbol.startsWith("OANDA:") || forexSymbols.contains(result.symbol)
                    AssetFilter.OTHERS -> true
                }
            }.take(10)
            filteredResults.map { result ->
                companyNameMap[result.symbol] = result.description
                getStockQuote(result.symbol)?.let { quote ->
                    val isCrypto = result.symbol.startsWith("BINANCE:") || cryptoSymbols.contains(result.symbol)
                    val isForex = result.symbol.startsWith("OANDA:") || forexSymbols.contains(result.symbol)
                    Stock(
                        symbol = result.symbol,
                        name = result.description,
                        price = quote.price,
                        change = quote.change,
                        percentChange = quote.percentChange,
                        high = quote.high,
                        low = quote.low,
                        open = quote.open,
                        prevClose = quote.prevClose,
                        isCrypto = isCrypto,
                        isForex = isForex,
                        industry = industryCache[result.symbol]
                    )
                }
            }.filterNotNull()
        } catch (e: Exception) {
            recordError(e)
            emptyList()
        }
    }

    private val DEFAULT_WATCHLIST = listOf("AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX", "SPY")

    suspend fun getWatchlist(): List<WatchlistItem> {
        val userId = auth.currentUser?.uid ?: return DEFAULT_WATCHLIST.map { WatchlistItem(it) }
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("watchlist").get().await()
            if (snapshot.isEmpty) {
                DEFAULT_WATCHLIST.map { WatchlistItem(it) }
            } else {
                snapshot.documents.map { WatchlistItem(it.getString("symbol") ?: "") }
            }
        } catch (e: Exception) {
            recordError(e)
            DEFAULT_WATCHLIST.map { WatchlistItem(it) }
        }
    }

    suspend fun addToWatchlist(symbol: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).collection("watchlist").document(symbol).set(mapOf("symbol" to symbol)).await()
            analytics.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, Bundle().apply { putString(FirebaseAnalytics.Param.ITEM_ID, symbol) })
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(symbol: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).collection("watchlist").document(symbol).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun createTradeContract(contract: TradeContract): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val docRef = firestore.collection("users").document(userId).collection("contracts").document()
            val contractWithId = contract.copy(id = docRef.id, userId = userId)
            docRef.set(contractWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    fun getTradeContracts(statuses: List<ContractStatus>): Flow<List<TradeContract>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            return@callbackFlow
        }
        val statusStrings = statuses.map { it.name }
        if (statusStrings.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        
        val listener = firestore.collection("users").document(userId).collection("contracts")
            .whereIn("status", statusStrings)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        trySend(emptyList())
                    } else {
                        recordError(error)
                        close(error)
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val contracts = snapshot.toObjects(TradeContract::class.java)
                        .sortedByDescending { it.createdAt }
                    trySend(contracts)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getTradeContracts(status: ContractStatus = ContractStatus.PENDING): Flow<List<TradeContract>> = 
        getTradeContracts(listOf(status))

    suspend fun getPendingTradeContractsForCurrentUser(): List<TradeContract> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("contracts")
                .whereEqualTo("status", ContractStatus.PENDING.name)
                .get().await()
            snapshot.toObjects(TradeContract::class.java)
        } catch (e: Exception) {
            recordError(e)
            emptyList()
        }
    }

    suspend fun updateTradeContract(contract: TradeContract): Result<Unit> {
        return try {
            firestore.collection("users").document(contract.userId).collection("contracts").document(contract.id)
                .set(contract).await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun cancelTradeContract(contractId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).collection("contracts").document(contractId)
                .update("status", ContractStatus.CANCELLED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun settleOption(contract: TradeContract, currentPrice: Double, userId: String? = null): Result<Unit> {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        
        val isCall = contract.type == ContractType.CALL_OPTION
        val strike = contract.targetPrice
        val intrinsicValue = if (isCall) {
            (currentPrice - strike).coerceAtLeast(0.0)
        } else {
            (strike - currentPrice).coerceAtLeast(0.0)
        }
        
        val totalSettlement = intrinsicValue * 100 * contract.quantity
        val newStatus = if (totalSettlement > 0) ContractStatus.EXECUTED else ContractStatus.EXPIRED
        
        return try {
            val userRef = firestore.collection("users").document(targetUserId)
            val contractRef = userRef.collection("contracts").document(contract.id)
            
            firestore.runTransaction { transaction ->
                // Update balance if there's any value to settle
                if (totalSettlement > 0) {
                    val currentBalance = transaction.get(userRef).getDouble("balance") ?: 0.0
                    transaction.update(userRef, "balance", currentBalance + totalSettlement)
                }
                
                // Mark contract as executed or expired
                transaction.update(contractRef, "status", newStatus.name)
                null
            }.await()
            
            globalPortfolioCache = null
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e)
            Result.failure(e)
        }
    }

    suspend fun buyStock(symbol: String, quantity: Int, pricePerShare: Double, userId: String? = null): Result<Double> {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalCost = quantity * pricePerShare
        return try {
            var newBalance = 0.0
            val userRef = firestore.collection("users").document(targetUserId)
            val portfolioRef = userRef.collection("portfolio").document(symbol)
            firestore.runTransaction { transaction ->
                val currentBalance = transaction.get(userRef).getDouble("balance") ?: 0.0
                val portfolioDoc = transaction.get(portfolioRef)
                if (currentBalance >= totalCost) {
                    newBalance = currentBalance - totalCost
                    transaction.update(userRef, "balance", newBalance)
                    if (portfolioDoc.exists()) transaction.update(portfolioRef, "quantity", (portfolioDoc.getLong("quantity") ?: 0L) + quantity)
                    else transaction.set(portfolioRef, mapOf("quantity" to quantity.toLong(), "symbol" to symbol))
                    null
                } else throw Exception("Insufficient balance")
            }.await()
            analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, Bundle().apply { putString(FirebaseAnalytics.Param.CURRENCY, "USD"); putDouble(FirebaseAnalytics.Param.VALUE, totalCost); putString(FirebaseAnalytics.Param.TRANSACTION_ID, symbol) })
            globalPortfolioCache = null
            Result.success(newBalance)
        } catch (e: Exception) {
            recordError(e); Result.failure(e)
        }
    }

    suspend fun sellStock(symbol: String, quantity: Int, pricePerShare: Double, userId: String? = null): Result<Unit> {
        val targetUserId = userId ?: auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalGain = quantity * pricePerShare
        return try {
            val userRef = firestore.collection("users").document(targetUserId)
            val portfolioRef = userRef.collection("portfolio").document(symbol)
            firestore.runTransaction { transaction ->
                val currentQty = transaction.get(portfolioRef).getLong("quantity") ?: 0L
                if (currentQty >= quantity) {
                    val currentBalance = transaction.get(userRef).getDouble("balance") ?: 0.0
                    transaction.update(userRef, "balance", currentBalance + totalGain)
                    transaction.update(portfolioRef, "quantity", currentQty - quantity)
                    null
                } else throw Exception("Insufficient quantity")
            }.await()
            globalPortfolioCache = null
            Result.success(Unit)
        } catch (e: Exception) {
            recordError(e); Result.failure(e)
        }
    }

    fun getUserBalance(): Flow<Double> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(0.0)
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    recordError(error)
                    return@addSnapshotListener
                }
                val balance = snapshot?.getDouble("balance") ?: 0.0
                trySend(balance)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        if (e is Exception) recordError(e)
        emit(0.0)
    }

    suspend fun syncTotalAccountValue(value: Double): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            firestore.collection("users").document(userId).update(mapOf("totalAccountValue" to value, "lastSync" to FieldValue.serverTimestamp())).await()
            saveAccountValueHistory(userId, value)
            Result.success(Unit)
        } catch (e: Exception) { recordError(e); Result.failure(e) }
    }

    suspend fun saveAccountValueHistory(userId: String, value: Double) {
        try {
            val historyRef = firestore.collection("users").document(userId).collection("account_history")
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            historyRef.document(today).set(
                mapOf(
                    "value" to value,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            recordError(e)
        }
    }

    fun getAccountValueHistory(): Flow<List<Pair<Long, Double>>> = flow {
        val userId = auth.currentUser?.uid ?: return@flow
        val snapshot = firestore.collection("users").document(userId)
            .collection("account_history")
            .orderBy("timestamp")
            .get().await()
        
        val history = snapshot.documents.mapNotNull { doc ->
            val value = doc.getDouble("value") ?: return@mapNotNull null
            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: return@mapNotNull null
            timestamp to value
        }
        emit(history)
    }.catch { e ->
        if (e is Exception) recordError(e)
        emit(emptyList())
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getPortfolio(): List<Pair<String, Long>> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            firestore.collection("users").document(userId).collection("portfolio").get().await().documents.map { it.getString("symbol").orEmpty() to (it.getLong("quantity") ?: 0L) }
        } catch (e: Exception) { recordError(e); emptyList() }
    }

    suspend fun getCompanyProfile(symbol: String): FinnhubProfileResponse? = try { api.getCompanyProfile(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getBasicFinancials(symbol: String): FinnhubFinancialsResponse? = try { api.getBasicFinancials(symbol, "all", apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getMarketNews(): List<FinnhubNewsArticle> = try { api.getMarketNews("general", apiKey).take(10) } catch (e: Exception) { recordError(e); emptyList() }
    suspend fun getRecommendations(symbol: String): List<FinnhubRecommendationResponse> = try { api.getRecommendations(symbol, apiKey) } catch (e: Exception) { recordError(e); emptyList() }
    suspend fun getPeers(symbol: String): List<String> = try { api.getPeers(symbol, apiKey) } catch (e: Exception) { recordError(e); emptyList() }

    suspend fun getEarningsCalendar(symbol: String): FinnhubEarningsCalendarResponse? {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val calendar = Calendar.getInstance(); val today = sdf.format(calendar.time)
        calendar.add(Calendar.MONTH, 6)
        return try { api.getEarningsCalendar(symbol, today, sdf.format(calendar.time), apiKey) } catch (e: Exception) { recordError(e); null }
    }

    suspend fun getTechnicalIndicator(symbol: String, indicator: String, timeperiod: Int? = null): FinnhubIndicatorResponse? {
        val to = Instant.now().epochSecond
        return try { api.getTechnicalIndicator(symbol, "D", to - (365 * 24 * 3600), to, indicator, apiKey, timeperiod) } catch (e: Exception) { recordError(e); null }
    }

    suspend fun getDividends(symbol: String): List<FinnhubDividendResponse> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val calendar = Calendar.getInstance(); val today = sdf.format(calendar.time)
        calendar.add(Calendar.YEAR, -1)
        return try { api.getDividends(symbol, sdf.format(calendar.time), today, apiKey) } catch (e: Exception) { recordError(e); emptyList() }
    }

    suspend fun getIpoCalendar(): List<FinnhubIpoEntry> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); val calendar = Calendar.getInstance(); val today = sdf.format(calendar.time)
        calendar.add(Calendar.MONTH, 1)
        return try { api.getIpoCalendar(today, sdf.format(calendar.time), apiKey).ipoCalendar } catch (e: Exception) { recordError(e); emptyList() }
    }

    suspend fun getNewsSentiment(symbol: String): FinnhubNewsSentimentResponse? = try { api.getNewsSentiment(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getForexRates(base: String = "USD"): FinnhubForexRatesResponse? = try { api.getForexRates(base, apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getMarketStatus(exchange: String = "US"): FinnhubMarketStatusResponse? = try { api.getMarketStatus(exchange, apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getEsgScores(symbol: String): FinnhubEsgResponse? = try { api.getEsgScores(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getPriceTarget(symbol: String): FinnhubPriceTargetResponse? = try { api.getPriceTarget(symbol, apiKey) } catch (e: Exception) { recordError(e); null }
    suspend fun getEarningsSurprises(symbol: String): List<FinnhubEarningsSurpriseResponse> = try { api.getEarningsSurprises(symbol, apiKey) } catch (e: Exception) { recordError(e); emptyList() }

    fun getPriceAlerts(symbol: String): Flow<List<PriceAlertEntity>> {
        return stockDao.getAlertsForStock(symbol)
    }

    suspend fun addPriceAlert(alert: PriceAlertEntity) {
        stockDao.insertPriceAlert(alert)
    }

    suspend fun deletePriceAlert(alert: PriceAlertEntity) {
        stockDao.deletePriceAlert(alert)
    }

    fun analyzeStock(
        stock: Stock,
        financials: FinnhubFinancialsResponse?,
        priceTarget: FinnhubPriceTargetResponse?,
        rsi: Double?,
        sma50: Double?,
        sma200: Double?,
        sentiment: FinnhubNewsSentimentResponse?,
        analystRecs: FinnhubRecommendationResponse?,
        news: List<FinnhubNewsArticle>
    ): AIRecommendation {
        val reasons = mutableListOf<String>()
        var confidence = 50
        
        // Price vs Target
        priceTarget?.targetMean?.let { target ->
            if (stock.price < target) {
                reasons.add("Trading below analyst mean target of $${String.format(Locale.US, "%.2f", target)}.")
                confidence += 10
            } else {
                reasons.add("Trading above analyst mean target of $${String.format(Locale.US, "%.2f", target)}.")
                confidence -= 10
            }
        }

        // RSI
        rsi?.let {
            if (it > 70) {
                reasons.add("RSI is at ${String.format(Locale.US, "%.1f", it)}, indicating overbought conditions.")
                confidence -= 15
            } else if (it < 30) {
                reasons.add("RSI is at ${String.format(Locale.US, "%.1f", it)}, indicating oversold conditions.")
                confidence += 15
            } else {
                reasons.add("RSI is at ${String.format(Locale.US, "%.1f", it)}, which is in a neutral range.")
            }
        }

        // SMA
        if (sma50 != null && sma200 != null) {
            if (sma50 > sma200) {
                reasons.add("Golden cross detected: 50-day SMA is above 200-day SMA.")
                confidence += 10
            } else {
                reasons.add("Death cross detected: 50-day SMA is below 200-day SMA.")
                confidence -= 10
            }
        }

        // Valuation
        val metrics = financials?.metric
        val pe = metrics?.get("peBasicExclExtraTTM") as? Double
        if (pe != null) {
            if (pe > 25) {
                reasons.add("P/E Ratio of ${String.format(Locale.US, "%.1f", pe)} suggests high valuation.")
                confidence -= 5
            } else if (pe < 15) {
                reasons.add("P/E Ratio of ${String.format(Locale.US, "%.1f", pe)} suggests potential undervaluation.")
                confidence += 5
            }
        }

        val advice = when {
            confidence >= 70 -> "Strong Buy"
            confidence >= 60 -> "Buy"
            confidence <= 30 -> "Strong Sell"
            confidence <= 40 -> "Sell"
            else -> "Hold"
        }

        return AIRecommendation(
            advice = advice,
            confidence = confidence.coerceIn(0, 100),
            reasons = if (reasons.isEmpty()) listOf("Insufficient data for detailed analysis.") else reasons
        )
    }
}
