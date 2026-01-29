package com.rahul.stocksim.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.tasks.await
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
}

data class FinnhubQuoteResponse(
    val c: Double, // Current price
    val d: Double, // Change
    val dp: Double, // Percent change
    val h: Double, // High price of the day
    val l: Double, // Low price of the day
    val o: Double, // Open price of the day
    val pc: Double // Previous close price
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

class MarketRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val apiKey = "d38davhr01qlbdj4vutgd38davhr01qlbdj4vuu0"

    private val api = Retrofit.Builder()
        .baseUrl("https://finnhub.io/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FinnhubApi::class.java)

    suspend fun getStockQuote(symbol: String): Stock? {
        return try {
            val response = api.getQuote(symbol, apiKey)
            Stock(
                symbol = symbol,
                name = symbol, // Finnhub quote doesn't provide name, need to fetch from search or profile
                price = response.c,
                change = response.d
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchStocks(query: String): List<Stock> {
        return try {
            val response = api.searchSymbol(query, apiKey)
            response.result
                .filter { it.type == "Common Stock" }
                .take(10)
                .map { result ->
                    // For each search result, we ideally want the current price.
                    // However, fetching quotes for all would be many API calls.
                    // For now, we'll return them with 0.0 price or fetch if needed.
                    val quote = getStockQuote(result.symbol)
                    Stock(
                        symbol = result.symbol,
                        name = result.description,
                        price = quote?.price ?: 0.0,
                        change = quote?.change ?: 0.0
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun buyStock(symbol: String, quantity: Int, pricePerShare: Double): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
        val totalCost = quantity * pricePerShare

        return try {
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0

                if (currentBalance >= totalCost) {
                    transaction.update(userRef, "balance", currentBalance - totalCost)
                    
                    val portfolioRef = userRef.collection("portfolio").document(symbol)
                    val portfolioDoc = transaction.get(portfolioRef)
                    
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
            Result.success(Unit)
        } catch (e: Exception) {
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
                val currentQty = portfolioDoc.getLong("quantity") ?: 0L

                if (currentQty >= quantity) {
                    val snapshot = transaction.get(userRef)
                    val currentBalance = snapshot.getDouble("balance") ?: 0.0
                    
                    transaction.update(userRef, "balance", currentBalance + totalGain)
                    
                    if (currentQty == quantity.toLong()) {
                        transaction.delete(portfolioRef)
                    } else {
                        transaction.update(portfolioRef, "quantity", currentQty - quantity)
                    }
                    null
                } else {
                    throw Exception("Insufficient quantity")
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}