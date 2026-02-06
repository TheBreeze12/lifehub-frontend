package com.example.lifehub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 饮食记录本地缓存实体 - Phase 34
 * 支持离线新增记录（isSynced=false），联网后同步到云端
 */
@Entity(tableName = "diet_records")
data class DietRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "food_name")
    val foodName: String,
    val calories: Double,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    @ColumnInfo(name = "meal_type")
    val mealType: String,
    @ColumnInfo(name = "record_date")
    val recordDate: String,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)
