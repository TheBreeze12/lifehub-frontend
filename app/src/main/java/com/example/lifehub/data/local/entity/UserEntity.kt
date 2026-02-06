package com.example.lifehub.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户本地缓存实体 - Phase 34
 * 缓存用户偏好设置，支持离线查看
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int,
    val nickname: String? = null,
    val healthGoal: String? = null,
    val allergens: List<String>? = null,
    val travelPreference: String? = null,
    val dailyBudget: Int? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val age: Int? = null,
    val gender: String? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
