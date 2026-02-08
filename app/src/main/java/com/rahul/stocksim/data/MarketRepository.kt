package com.rahul.stocksim.data

import android.os.Bundle
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

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

data class WatchlistItem(val symbol: String)

class MarketRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics
    
    private val apiKey = "d38davhr01qlbdj4vutgd38davhr01qlbdj4vuu0"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        // Log everything to Crashlytics and Logcat always
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
        return try {
            val response = api.getQuote(symbol, apiKey)
            Stock(
                symbol = symbol,
                name = symbol,
                price = response.c,
                change = response.d,
                percentChange = response.dp,
                high = response.h,
                low = response.l,
                open = response.o,
                prevClose = response.pc
            )
        } catch (e: Exception) {
            crashlytics.recordException(e)
            null
        }
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
                async {
                    val quote = getStockQuote(result.symbol)
                    Stock(
                        symbol = result.symbol,
                        name = result.description,
                        price = quote?.price ?: 0.0,
                        change = quote?.change ?: 0.0,
                        percentChange = quote?.percentChange ?: 0.0,
                        high = quote?.high ?: 0.0,
                        low = quote?.low ?: 0.0,
                        open = quote?.open ?: 0.0,
                        prevClose = quote?.prevClose ?: 0.0
                    )
                }
            }.awaitAll()
        } catch (e: Exception) {
            crashlytics.recordException(e)
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
            crashlytics.recordException(e)
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
            crashlytics.recordException(e)
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
            crashlytics.recordException(e)
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
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordException(e)
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
            crashlytics.recordException(e)
            emptyList()
        }
    }
}
