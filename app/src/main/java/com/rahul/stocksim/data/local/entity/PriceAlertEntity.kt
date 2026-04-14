package com.rahul.stocksim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val symbol: String,
    val targetPrice: Double,
    val isAbove: Boolean, // true if alert triggers when price goes above target, false if below
    val isEnabled: Boolean = true
)
