package com.rahul.stocksim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: Long,
    val symbol: String?, // null for general news
    val category: String,
    val datetime: Long,
    val headline: String,
    val image: String,
    val related: String,
    val source: String,
    val summary: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
