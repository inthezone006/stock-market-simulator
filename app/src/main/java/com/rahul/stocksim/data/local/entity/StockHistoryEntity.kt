package com.rahul.stocksim.data.local.entity

import androidx.room.Entity

@Entity(tableName = "stock_history", primaryKeys = ["symbol", "period"])
data class StockHistoryEntity(
    val symbol: String,
    val period: String,
    val historyJson: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
