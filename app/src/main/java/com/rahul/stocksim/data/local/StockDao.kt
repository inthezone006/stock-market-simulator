package com.rahul.stocksim.data.local

import androidx.room.*
import com.rahul.stocksim.data.local.entity.AchievementEntity
import com.rahul.stocksim.data.local.entity.PriceAlertEntity
import com.rahul.stocksim.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Query("SELECT * FROM stocks WHERE symbol = :symbol")
    suspend fun getStock(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>): List<Long>

    @Query("DELETE FROM stocks")
    suspend fun clearAll(): Int

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1")
    suspend fun getActivePriceAlerts(): List<PriceAlertEntity>

    @Query("SELECT * FROM price_alerts WHERE symbol = :symbol")
    fun getAlertsForStock(symbol: String): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlertEntity): Long

    @Update
    suspend fun updatePriceAlert(alert: PriceAlertEntity): Int

    @Delete
    suspend fun deletePriceAlert(alert: PriceAlertEntity): Int
}
