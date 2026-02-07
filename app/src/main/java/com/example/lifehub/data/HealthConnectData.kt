package com.example.lifehub.data

/**
 * Health Connect 数据模型 - Phase 44
 *
 * 定义与Google Health Connect交互所需的数据结构。
 * 包含步数、心率、卡路里、运动会话等健康数据的本地表示。
 */

/**
 * Health Connect 可用性状态
 */
enum class HealthConnectAvailabilityStatus {
    /** Health Connect 已安装且可用 */
    AVAILABLE,
    /** Health Connect 未安装 */
    NOT_INSTALLED,
    /** 设备不支持 Health Connect（API < 26） */
    NOT_SUPPORTED
}

/**
 * Health Connect 数据同步状态
 */
sealed class HealthConnectSyncState {
    /** 空闲 */
    object Idle : HealthConnectSyncState()
    /** 同步中 */
    object Syncing : HealthConnectSyncState()
    /** 同步成功 */
    data class Success(val message: String) : HealthConnectSyncState()
    /** 同步失败 */
    data class Error(val message: String) : HealthConnectSyncState()
}

/**
 * Health Connect 权限类型枚举
 */
enum class HealthConnectPermissionType {
    READ_STEPS,
    WRITE_STEPS,
    READ_HEART_RATE,
    WRITE_HEART_RATE,
    READ_CALORIES,
    WRITE_CALORIES,
    READ_EXERCISE,
    WRITE_EXERCISE
}

/**
 * 健康数据类型枚举
 */
enum class HealthDataType {
    STEPS,
    HEART_RATE,
    CALORIES,
    EXERCISE_SESSION
}

/**
 * 步数记录
 *
 * @param startTime 开始时间戳（毫秒）
 * @param endTime 结束时间戳（毫秒）
 * @param count 步数
 */
data class StepsRecord(
    val startTime: Long,
    val endTime: Long,
    val count: Int
)

/**
 * 心率记录
 *
 * @param time 测量时间戳（毫秒）
 * @param beatsPerMinute 每分钟心跳次数
 */
data class HeartRateRecord(
    val time: Long,
    val beatsPerMinute: Int
)

/**
 * 卡路里消耗记录
 *
 * @param startTime 开始时间戳（毫秒）
 * @param endTime 结束时间戳（毫秒）
 * @param totalCalories 总消耗卡路里（kcal）
 */
data class CaloriesRecord(
    val startTime: Long,
    val endTime: Long,
    val totalCalories: Double
)

/**
 * 运动会话记录
 *
 * @param startTime 开始时间戳（毫秒）
 * @param endTime 结束时间戳（毫秒）
 * @param exerciseType 运动类型（如 walking, running, cycling）
 * @param title 运动标题（可选）
 * @param calories 消耗卡路里（可选）
 * @param distance 运动距离（米，可选）
 * @param steps 步数（可选）
 */
data class ExerciseSessionRecord(
    val startTime: Long,
    val endTime: Long,
    val exerciseType: String,
    val title: String? = null,
    val calories: Double? = null,
    val distance: Double? = null,
    val steps: Int? = null
)

/**
 * Health Connect 聚合数据（当日汇总）
 *
 * @param todaySteps 今日总步数
 * @param todayCalories 今日总消耗卡路里
 * @param latestHeartRate 最新心率
 * @param exerciseSessions 今日运动会话列表
 * @param stepsRecords 步数记录列表
 * @param heartRateRecords 心率记录列表
 * @param caloriesRecords 卡路里记录列表
 */
data class HealthConnectData(
    val todaySteps: Long = 0L,
    val todayCalories: Double = 0.0,
    val latestHeartRate: Int? = null,
    val exerciseSessions: List<ExerciseSessionRecord> = emptyList(),
    val stepsRecords: List<StepsRecord> = emptyList(),
    val heartRateRecords: List<HeartRateRecord> = emptyList(),
    val caloriesRecords: List<CaloriesRecord> = emptyList()
)

/**
 * Health Connect 每日摘要
 *
 * @param date 日期（YYYY-MM-DD）
 * @param totalSteps 总步数
 * @param totalCalories 总卡路里
 * @param averageHeartRate 平均心率（可选）
 * @param exerciseMinutes 运动分钟数
 * @param exerciseCount 运动次数
 */
data class HealthConnectDailySummary(
    val date: String,
    val totalSteps: Long = 0L,
    val totalCalories: Double = 0.0,
    val averageHeartRate: Int? = null,
    val exerciseMinutes: Int = 0,
    val exerciseCount: Int = 0
)

/**
 * 通用健康数据记录（多态密封类）
 * 用于统一处理不同类型的健康数据写入
 */
sealed class HealthDataRecord {
    data class Steps(
        val startTime: Long,
        val endTime: Long,
        val count: Int
    ) : HealthDataRecord()

    data class HeartRate(
        val time: Long,
        val bpm: Int
    ) : HealthDataRecord()

    data class Calories(
        val startTime: Long,
        val endTime: Long,
        val kcal: Double
    ) : HealthDataRecord()

    data class Exercise(
        val startTime: Long,
        val endTime: Long,
        val type: String,
        val title: String? = null,
        val calories: Double? = null,
        val distance: Double? = null
    ) : HealthDataRecord()
}

/**
 * Health Connect 工具类 - Phase 44
 *
 * 提供格式化、验证、转换等纯函数工具方法。
 */
object HealthConnectUtils {

    /** 默认每日步数目标 */
    const val DEFAULT_STEP_GOAL = 10000L

    /** 默认每日卡路里消耗目标（kcal） */
    const val DEFAULT_CALORIE_GOAL = 500.0

    /**
     * 格式化步数（千分位分隔）
     */
    fun formatSteps(steps: Long): String {
        return String.format("%,d", steps)
    }

    /**
     * 格式化卡路里（保留1位小数）
     */
    fun formatCalories(calories: Double): String {
        return String.format("%.1f", calories)
    }

    /**
     * 格式化心率
     */
    fun formatHeartRate(bpm: Int): String {
        return "$bpm bpm"
    }

    /**
     * 格式化时长（毫秒 → 中文描述）
     */
    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0分钟"
        val totalMinutes = (millis / 60_000).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}小时${minutes}分钟"
        } else {
            "${minutes}分钟"
        }
    }

    /**
     * 将运动类型代码映射为中文标签
     */
    fun mapExerciseTypeToLabel(type: String): String {
        return when (type.lowercase()) {
            "walking" -> "步行"
            "running" -> "跑步"
            "cycling" -> "骑行"
            "hiking" -> "徒步"
            "swimming" -> "游泳"
            "yoga" -> "瑜伽"
            "strength_training" -> "力量训练"
            "dancing" -> "舞蹈"
            "badminton" -> "羽毛球"
            "basketball" -> "篮球"
            "football" -> "足球"
            "tennis" -> "网球"
            "table_tennis" -> "乒乓球"
            else -> type
        }
    }

    /**
     * 验证心率值是否在合理范围内（20-250 bpm）
     */
    fun isValidHeartRate(bpm: Int): Boolean {
        return bpm in 20..250
    }

    /**
     * 验证步数是否在合理范围内（0-100000）
     */
    fun isValidStepCount(count: Long): Boolean {
        return count in 0..100000
    }

    /**
     * 计算步数目标完成进度（0.0-1.0）
     * 负数步数视为0，超过目标上限为1.0
     */
    fun calculateStepGoalProgress(steps: Long, goal: Long): Double {
        if (goal <= 0 || steps <= 0) return 0.0
        return (steps.toDouble() / goal.toDouble()).coerceIn(0.0, 1.0)
    }

    /**
     * 计算卡路里目标完成进度（0.0-1.0）
     */
    fun calculateCalorieGoalProgress(calories: Double, goal: Double): Double {
        if (goal <= 0.0 || calories <= 0.0) return 0.0
        return (calories / goal).coerceIn(0.0, 1.0)
    }

    /**
     * 根据步数估算距离（米）
     *
     * @param steps 步数
     * @param strideLength 步长（米），默认0.75m
     * @return 估算距离（米）
     */
    fun estimateDistanceFromSteps(steps: Long, strideLength: Double = 0.75): Double {
        if (steps <= 0) return 0.0
        return steps * strideLength
    }

    /**
     * 根据步数估算消耗卡路里
     * 简化公式：每步约消耗 0.04 * 体重(kg) / 70 卡路里
     *
     * @param steps 步数
     * @param weightKg 体重（kg），默认70
     * @return 估算卡路里（kcal）
     */
    fun estimateCaloriesFromSteps(steps: Long, weightKg: Double = 70.0): Double {
        if (steps <= 0) return 0.0
        return steps * 0.04 * (weightKg / 70.0)
    }
}
