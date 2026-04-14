package com.rahul.stocksim.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rahul.stocksim.data.local.entity.PriceAlertEntity
import com.rahul.stocksim.data.local.entity.StockEntity

@Database(entities = [StockEntity::class, PriceAlertEntity::class], version = 2, exportSchema = false)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
}
