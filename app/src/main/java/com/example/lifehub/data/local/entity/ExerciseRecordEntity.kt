package com.example.lifehub.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 运动记录本地缓存实体 - Phase 34
 * 支持离线记录运动数据，联网后同步到云端
 */
@Entity(tableName = "exercise_records")
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "plan_id")
    val planId: Int? = null,
    @ColumnInfo(name = "exercise_type")
    val exerciseType: String = "walking",
    @ColumnInfo(name = "actual_calories")
    val actualCalories: Double,
    @ColumnInfo(name = "actual_duration")
    val actualDuration: Int,
    val distance: Double? = null,
    @ColumnInfo(name = "exercise_date")
    val exerciseDate: String,
    @ColumnInfo(name = "started_at")
    val startedAt: String? = null,
    @ColumnInfo(name = "ended_at")
    val endedAt: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)
