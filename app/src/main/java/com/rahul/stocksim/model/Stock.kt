package com.rahul.stocksim.model

data class Stock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val percentChange: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val prevClose: Double = 0.0,
    val isCrypto: Boolean = false,
    val isForex: Boolean = false
)