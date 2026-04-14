package com.rahul.stocksim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val percentChange: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val prevClose: Double,
    val isCrypto: Boolean,
    val isForex: Boolean,
    val industry: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
