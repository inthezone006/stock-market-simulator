package com.rahul.stocksim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_details")
data class CompanyDetailsEntity(
    @PrimaryKey val symbol: String,
    val profileJson: String? = null,
    val financialsJson: String? = null,
    val recommendationsJson: String? = null,
    val peersJson: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
