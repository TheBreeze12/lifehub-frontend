package com.example.lifehub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 运动计划本地缓存实体 - Phase 34
 * items以JSON字符串存储，避免过度拆分表结构
 */
@Entity(tableName = "trip_plans")
data class TripPlanEntity(
    @PrimaryKey
    @ColumnInfo(name = "trip_id")
    val tripId: Int,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    val title: String,
    val destination: String? = null,
    @ColumnInfo(name = "start_date")
    val startDate: String,
    @ColumnInfo(name = "end_date")
    val endDate: String,
    @ColumnInfo(name = "items_json")
    val itemsJson: String = "[]",
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = System.currentTimeMillis()
)
