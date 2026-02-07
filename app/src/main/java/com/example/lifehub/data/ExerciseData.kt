package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/**
 * 运动轨迹点数据模型 - Phase 27
 * 记录GPS位置和时间戳
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 运动追踪状态 - Phase 27
 */
sealed class ExerciseTrackingState {
    /** 未开始 */
    object Idle : ExerciseTrackingState()
    /** 追踪中 */
    object Tracking : ExerciseTrackingState()
    /** 暂停 */
    object Paused : ExerciseTrackingState()
    /** 已完成 */
    data class Completed(
        val totalDistance: Double,
        val totalDuration: Long,
        val averagePace: Double,
        val trackPoints: List<TrackPoint>
    ) : ExerciseTrackingState()
}

/**
 * 运动追踪实时数据 - Phase 27
 * 用于UI展示实时配速、距离、时间
 */
data class ExerciseTrackingData(
    val trackPoints: List<TrackPoint> = emptyList(),
    val totalDistance: Double = 0.0,          // 总距离（米）
    val elapsedTime: Long = 0L,              // 已用时间（毫秒）
    val currentPace: Double = 0.0,           // 当前配速（分钟/公里）
    val averagePace: Double = 0.0,           // 平均配速（分钟/公里）
    val currentSpeed: Double = 0.0,          // 当前速度（km/h）
    val caloriesBurned: Double = 0.0,        // 估算消耗热量（kcal）
    val planId: Int? = null,                 // 关联的运动计划ID
    val exerciseType: String = "walking"     // 运动类型
)

/**
 * 运动追踪工具类 - Phase 27
 * 计算距离、配速等
 */
// ==================== Phase 28: 运动结算数据模型 ====================

/**
 * 保存运动记录的状态 - Phase 28
 */
sealed class SaveExerciseState {
    /** 空闲 */
    object Idle : SaveExerciseState()
    /** 保存中 */
    object Saving : SaveExerciseState()
    /** 保存成功 */
    data class Success(val recordId: Int) : SaveExerciseState()
    /** 保存失败 */
    data class Error(val message: String) : SaveExerciseState()
}

/**
 * 新增运动记录请求 - Phase 28
 * 与后端 POST /api/exercise/record 对应
 */
data class CreateExerciseRecordRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("plan_id") val planId: Int? = null,
    @SerializedName("exercise_type") val exerciseType: String = "walking",
    @SerializedName("actual_calories") val actualCalories: Double,
    @SerializedName("actual_duration") val actualDuration: Int,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("route_data") val routeData: String? = null,
    @SerializedName("planned_calories") val plannedCalories: Double? = null,
    @SerializedName("planned_duration") val plannedDuration: Int? = null,
    @SerializedName("exercise_date") val exerciseDate: String,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("ended_at") val endedAt: String? = null,
    @SerializedName("notes") val notes: String? = null
)

/**
 * 运动记录响应数据 - Phase 28
 * 与后端 ExerciseRecordData 对应
 */
data class ExerciseRecordResponseData(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("plan_id") val planId: Int? = null,
    @SerializedName("exercise_type") val exerciseType: String,
    @SerializedName("actual_calories") val actualCalories: Double,
    @SerializedName("actual_duration") val actualDuration: Int,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("route_data") val routeData: String? = null,
    @SerializedName("planned_calories") val plannedCalories: Double? = null,
    @SerializedName("planned_duration") val plannedDuration: Int? = null,
    @SerializedName("exercise_date") val exerciseDate: String,
    @SerializedName("started_at") val startedAt: String? = null,
    @SerializedName("ended_at") val endedAt: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("calories_achievement") val caloriesAchievement: Double? = null,
    @SerializedName("duration_achievement") val durationAchievement: Double? = null
)

/**
 * 新增运动记录响应 - Phase 28
 * 与后端 CreateExerciseRecordResponse 对应
 */
data class CreateExerciseRecordResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ExerciseRecordResponseData? = null
)

// ==================== Phase 49: 运动历史记录数据模型 ====================

/**
 * 运动记录列表响应 - Phase 49
 * 与后端 GET /api/exercise/records 对应
 */
data class ExerciseRecordListResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: List<ExerciseRecordResponseData> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

/**
 * 运动记录详情响应 - Phase 49
 * 与后端 GET /api/exercise/record/{record_id} 对应
 */
data class ExerciseRecordDetailResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: ExerciseRecordResponseData? = null
)

/**
 * 运动历史加载状态 - Phase 49
 */
sealed class ExerciseHistoryState {
    /** 空闲 */
    object Idle : ExerciseHistoryState()
    /** 加载中 */
    object Loading : ExerciseHistoryState()
    /** 加载成功 */
    data class Success(
        val records: List<ExerciseRecordResponseData>,
        val total: Int
    ) : ExerciseHistoryState()
    /** 加载失败 */
    data class Error(val message: String) : ExerciseHistoryState()
}

/**
 * 运动记录详情加载状态 - Phase 49
 */
sealed class ExerciseDetailState {
    /** 空闲 */
    object Idle : ExerciseDetailState()
    /** 加载中 */
    object Loading : ExerciseDetailState()
    /** 加载成功 */
    data class Success(val record: ExerciseRecordResponseData) : ExerciseDetailState()
    /** 加载失败 */
    data class Error(val message: String) : ExerciseDetailState()
}

/**
 * 运动类型工具类 - Phase 49
 * 提供运动类型的中文名称和图标映射
 */
object ExerciseTypeUtils {
    /** 运动类型中文名称映射 */
    fun getTypeLabel(type: String): String = when (type) {
        "walking" -> "散步"
        "running" -> "跑步"
        "cycling" -> "骑行"
        "jogging" -> "慢跑"
        "hiking" -> "徒步"
        "swimming" -> "游泳"
        "gym" -> "健身房"
        "indoor" -> "室内运动"
        "outdoor" -> "户外运动"
        else -> type
    }

    /** 运动类型对应的emoji */
    fun getTypeEmoji(type: String): String = when (type) {
        "walking" -> "🚶"
        "running" -> "🏃"
        "cycling" -> "🚴"
        "jogging" -> "🏃"
        "hiking" -> "🥾"
        "swimming" -> "🏊"
        "gym" -> "🏋️"
        "indoor" -> "🏠"
        "outdoor" -> "🌳"
        else -> "🏅"
    }
}

object ExerciseTrackingUtils {

    /**
     * 使用Haversine公式计算两点间距离
     * @return 距离（米）
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // 地球半径（米）
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * 计算轨迹点列表的总距离
     * @return 总距离（米）
     */
    fun calculateTotalDistance(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            totalDistance += calculateDistance(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
        }
        return totalDistance
    }

    /**
     * 计算配速（分钟/公里）
     * @param distanceMeters 距离（米）
     * @param durationMillis 时间（毫秒）
     * @return 配速（分钟/公里），如果距离为0返回0
     */
    fun calculatePace(distanceMeters: Double, durationMillis: Long): Double {
        if (distanceMeters <= 0 || durationMillis <= 0) return 0.0
        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationMillis / 60000.0
        return durationMinutes / distanceKm
    }

    /**
     * 计算速度（km/h）
     * @param distanceMeters 距离（米）
     * @param durationMillis 时间（毫秒）
     * @return 速度（km/h）
     */
    fun calculateSpeed(distanceMeters: Double, durationMillis: Long): Double {
        if (distanceMeters <= 0 || durationMillis <= 0) return 0.0
        val distanceKm = distanceMeters / 1000.0
        val durationHours = durationMillis / 3600000.0
        return distanceKm / durationHours
    }

    /**
     * 估算消耗热量（简化METs计算）
     * @param exerciseType 运动类型
     * @param durationMinutes 运动时长（分钟）
     * @param weightKg 体重（kg）
     * @return 消耗热量（kcal）
     */
    fun estimateCalories(
        exerciseType: String,
        durationMinutes: Double,
        weightKg: Double = 70.0
    ): Double {
        val mets = when (exerciseType) {
            "walking" -> 3.5
            "running" -> 8.0
            "cycling" -> 6.0
            "hiking" -> 5.5
            else -> 4.0
        }
        // 公式：消耗(kcal) = METs × 体重(kg) × 时间(h)
        return mets * weightKg * (durationMinutes / 60.0)
    }

    /**
     * 格式化时长为 HH:MM:SS
     */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 格式化配速为 M'SS"
     */
    fun formatPace(paceMinPerKm: Double): String {
        if (paceMinPerKm <= 0 || paceMinPerKm > 60) return "--'--\""
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return String.format("%d'%02d\"", minutes, seconds)
    }

    /**
     * 格式化距离
     * @param meters 距离（米）
     * @return 格式化后的字符串
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            String.format("%.0f m", meters)
        } else {
            String.format("%.2f km", meters / 1000.0)
        }
    }
}
