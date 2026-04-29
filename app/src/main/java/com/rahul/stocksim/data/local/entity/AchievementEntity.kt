package com.rahul.stocksim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,
    val unlockedAt: Long? = null
)
