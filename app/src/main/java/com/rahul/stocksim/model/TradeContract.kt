package com.rahul.stocksim.model

import com.google.firebase.Timestamp

enum class ContractType {
    BUY_AT, SELL_AT, CALL_OPTION, PUT_OPTION
}

enum class ContractStatus {
    PENDING, EXECUTED, CANCELLED, EXPIRED
}

data class TradeContract(
    val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val type: ContractType = ContractType.BUY_AT,
    val targetPrice: Double = 0.0, // Used as Strike Price for Options
    val quantity: Long = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val status: ContractStatus = ContractStatus.PENDING,
    val premium: Double = 0.0,
    val expirationDate: Timestamp? = null
)