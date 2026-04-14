package com.rahul.stocksim.data.local

import androidx.room.*
import com.rahul.stocksim.data.local.entity.PriceAlertEntity
import com.rahul.stocksim.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStock(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Query("DELETE FROM stocks")
    suspend fun clearAll()

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1")
    suspend fun getActivePriceAlerts(): List<PriceAlertEntity>

    @Query("SELECT * FROM price_alerts WHERE symbol = :symbol")
    fun getAlertsForStock(symbol: String): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlertEntity)

    @Update
    suspend fun updatePriceAlert(alert: PriceAlertEntity)

    @Delete
    suspend fun deletePriceAlert(alert: PriceAlertEntity)
}
