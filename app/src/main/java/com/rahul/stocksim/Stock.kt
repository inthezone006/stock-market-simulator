package com.rahul.stocksim

data class Stock(
    val symbol: String, // Stock symbol
    val name: String, // Stock name
    val price: Double, // Stock price
    val change: Double // Stock change (positive or negative)
)
