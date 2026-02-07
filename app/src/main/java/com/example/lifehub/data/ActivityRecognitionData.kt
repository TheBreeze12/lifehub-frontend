package com.example.lifehub.data

/**
 * 用户运动状态类型枚举 - Phase 43
 * 对应 Android Activity Recognition API 的 DetectedActivity 类型
 *
 * @param label 中文标签
 * @param labelEn 英文标签
 */
enum class DetectedActivityType(val label: String, val labelEn: String) {
    STILL("静止", "Still"),
    WALKING("步行", "Walking"),
    RUNNING("跑步", "Running"),
    CYCLING("骑行", "Cycling"),
    IN_VEHICLE("乘车", "In Vehicle"),
    ON_FOOT("步行中", "On Foot"),
    TILTING("倾斜", "Tilting"),
    UNKNOWN("未知", "Unknown")
}

/**
 * 活动识别结果数据模型 - Phase 43
 *
 * @param activityType 检测到的活动类型
 * @param confidence 置信度，范围0-100
 * @param timestamp 检测时间戳（毫秒）
 */
data class ActivityRecognitionResult(
    val activityType: DetectedActivityType,
    val confidence: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 活动识别服务状态 - Phase 43
 */
sealed class ActivityRecognitionState {
    /** 空闲（未启动识别） */
    object Idle : ActivityRecognitionState()
    /** 监听中（正在识别用户活动） */
    object Monitoring : ActivityRecognitionState()
    /** 错误（识别启动失败） */
    data class Error(val message: String) : ActivityRecognitionState()
}

/**
 * 活动识别工具类 - Phase 43
 *
 * 提供活动类型映射、置信度判断、METs估算等工具方法。
 * 所有方法均为纯函数，方便单元测试。
 */
object ActivityRecognitionUtils {

    /** 最低置信度阈值（低于此值视为不可靠） */
    const val MIN_CONFIDENCE_THRESHOLD = 50

    /** 高置信度阈值（高于此值视为高可信） */
    const val HIGH_CONFIDENCE_THRESHOLD = 75

    /**
     * 将Google Play Services的DetectedActivity类型常量映射为本地枚举
     *
     * GMS DetectedActivity 常量值：
     * - 0: IN_VEHICLE
     * - 1: ON_BICYCLE
     * - 2: ON_FOOT
     * - 3: STILL
     * - 4: UNKNOWN
     * - 5: TILTING
     * - 7: WALKING
     * - 8: RUNNING
     */
    fun mapFromGmsActivityType(gmsType: Int): DetectedActivityType {
        return when (gmsType) {
            0 -> DetectedActivityType.IN_VEHICLE
            1 -> DetectedActivityType.CYCLING
            2 -> DetectedActivityType.ON_FOOT
            3 -> DetectedActivityType.STILL
            4 -> DetectedActivityType.UNKNOWN
            5 -> DetectedActivityType.TILTING
            7 -> DetectedActivityType.WALKING
            8 -> DetectedActivityType.RUNNING
            else -> DetectedActivityType.UNKNOWN
        }
    }

    /**
     * 将检测到的活动类型映射为运动追踪的exerciseType字符串
     * 与 ExerciseTrackingUtils.estimateCalories 中的类型对齐
     */
    fun mapToExerciseType(activityType: DetectedActivityType): String {
        return when (activityType) {
            DetectedActivityType.WALKING -> "walking"
            DetectedActivityType.RUNNING -> "running"
            DetectedActivityType.CYCLING -> "cycling"
            DetectedActivityType.ON_FOOT -> "walking"
            DetectedActivityType.STILL -> "still"
            DetectedActivityType.IN_VEHICLE -> "in_vehicle"
            DetectedActivityType.TILTING -> "unknown"
            DetectedActivityType.UNKNOWN -> "unknown"
        }
    }

    /**
     * 判断置信度是否为高可信度（>= HIGH_CONFIDENCE_THRESHOLD）
     */
    fun isHighConfidence(confidence: Int): Boolean {
        return confidence >= HIGH_CONFIDENCE_THRESHOLD
    }

    /**
     * 判断置信度是否可接受（>= MIN_CONFIDENCE_THRESHOLD）
     */
    fun isAcceptableConfidence(confidence: Int): Boolean {
        return confidence >= MIN_CONFIDENCE_THRESHOLD
    }

    /**
     * 判断活动类型是否为有效运动（步行/跑步/骑行/步行中）
     * 排除：静止、乘车、倾斜、未知
     */
    fun isActiveExercise(activityType: DetectedActivityType): Boolean {
        return activityType in listOf(
            DetectedActivityType.WALKING,
            DetectedActivityType.RUNNING,
            DetectedActivityType.CYCLING,
            DetectedActivityType.ON_FOOT
        )
    }

    /**
     * 从多个检测结果中选择最可能的活动（置信度最高的）
     * 若列表为空返回null
     */
    fun getMostProbableActivity(results: List<ActivityRecognitionResult>): ActivityRecognitionResult? {
        return results.maxByOrNull { it.confidence }
    }

    /**
     * 从多个检测结果中过滤出可信的活动（置信度 >= minConfidence）
     */
    fun filterReliableActivities(
        results: List<ActivityRecognitionResult>,
        minConfidence: Int = MIN_CONFIDENCE_THRESHOLD
    ): List<ActivityRecognitionResult> {
        return results.filter { it.confidence >= minConfidence }
    }

    /**
     * 判断是否应自动开始运动追踪
     * 条件：检测到有效运动类型 且 置信度 >= 最低阈值
     */
    fun shouldAutoStartTracking(result: ActivityRecognitionResult): Boolean {
        return isActiveExercise(result.activityType) && isAcceptableConfidence(result.confidence)
    }

    /**
     * 获取活动类型对应的METs值估算
     * 用于粗略热量计算
     */
    fun getEstimatedMets(activityType: DetectedActivityType): Double {
        return when (activityType) {
            DetectedActivityType.STILL -> 1.0
            DetectedActivityType.WALKING -> 3.5
            DetectedActivityType.ON_FOOT -> 3.5
            DetectedActivityType.RUNNING -> 8.0
            DetectedActivityType.CYCLING -> 6.0
            DetectedActivityType.IN_VEHICLE -> 1.0
            DetectedActivityType.TILTING -> 1.0
            DetectedActivityType.UNKNOWN -> 1.5
        }
    }

    /**
     * 估算基于活动识别结果的热量消耗
     *
     * @param activityType 活动类型
     * @param durationMinutes 持续时长（分钟）
     * @param weightKg 体重（kg）
     * @return 估算消耗热量（kcal）
     */
    fun estimateCaloriesFromActivity(
        activityType: DetectedActivityType,
        durationMinutes: Double,
        weightKg: Double = 70.0
    ): Double {
        if (durationMinutes <= 0 || weightKg <= 0) return 0.0
        val mets = getEstimatedMets(activityType)
        return mets * weightKg * (durationMinutes / 60.0)
    }

    /**
     * 判断两个活动识别结果是否表示活动发生了变化
     * 用于状态转换检测
     */
    fun hasActivityChanged(
        previous: ActivityRecognitionResult?,
        current: ActivityRecognitionResult
    ): Boolean {
        if (previous == null) return true
        return previous.activityType != current.activityType
    }

    /**
     * 获取活动类型的显示图标名称（Material Icons名称）
     */
    fun getActivityIcon(activityType: DetectedActivityType): String {
        return when (activityType) {
            DetectedActivityType.STILL -> "accessibility_new"
            DetectedActivityType.WALKING -> "directions_walk"
            DetectedActivityType.ON_FOOT -> "directions_walk"
            DetectedActivityType.RUNNING -> "directions_run"
            DetectedActivityType.CYCLING -> "directions_bike"
            DetectedActivityType.IN_VEHICLE -> "directions_car"
            DetectedActivityType.TILTING -> "screen_rotation"
            DetectedActivityType.UNKNOWN -> "help_outline"
        }
    }
}
