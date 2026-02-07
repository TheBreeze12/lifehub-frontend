package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/**
 * Phase 48: 健康目标达成情况数据模型
 * 对应后端 Phase 36 的 GET /api/stats/goal-progress 接口
 */

/** 单个维度的达成情况 */
data class GoalDimension(
    @SerializedName("name") val name: String,
    @SerializedName("score") val score: Double = 0.0,
    @SerializedName("status") val status: String = "fair",
    @SerializedName("current_value") val currentValue: Double = 0.0,
    @SerializedName("target_value") val targetValue: Double = 0.0,
    @SerializedName("unit") val unit: String = "",
    @SerializedName("description") val description: String = ""
) {
    /** 达成率百分比（0-100，超出目标时cap到100） */
    val achievementRate: Double
        get() = if (targetValue > 0) (currentValue / targetValue * 100).coerceIn(0.0, 150.0) else 0.0

    /** 进度条比例（0.0-1.0） */
    val progressFraction: Float
        get() = if (targetValue > 0) (currentValue / targetValue).coerceIn(0.0, 1.0).toFloat() else 0f
}

/** 健康目标达成率完整数据 */
data class GoalProgressData(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("health_goal") val healthGoal: String,
    @SerializedName("health_goal_label") val healthGoalLabel: String,
    @SerializedName("period_days") val periodDays: Int = 7,
    @SerializedName("start_date") val startDate: String = "",
    @SerializedName("end_date") val endDate: String = "",
    @SerializedName("overall_score") val overallScore: Double = 0.0,
    @SerializedName("overall_status") val overallStatus: String = "fair",
    @SerializedName("dimensions") val dimensions: List<GoalDimension> = emptyList(),
    @SerializedName("suggestions") val suggestions: List<String> = emptyList(),
    @SerializedName("streak_days") val streakDays: Int = 0
)

/** 健康目标达成率响应 */
data class GoalProgressResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: GoalProgressData?
)

/** 状态枚举工具 */
object GoalStatusUtil {
    /** 状态转中文标签 */
    fun getStatusLabel(status: String): String = when (status) {
        "excellent" -> "优秀"
        "good" -> "良好"
        "fair" -> "一般"
        "poor" -> "待改善"
        else -> "未知"
    }

    /** 状态对应评分范围说明 */
    fun getStatusDescription(status: String): String = when (status) {
        "excellent" -> "90-100分"
        "good" -> "70-89分"
        "fair" -> "50-69分"
        "poor" -> "0-49分"
        else -> ""
    }

    /** 健康目标代码转中文 */
    fun getGoalLabel(goal: String): String = when (goal) {
        "reduce_fat" -> "减脂"
        "gain_muscle" -> "增肌"
        "control_sugar" -> "控糖"
        "balanced" -> "均衡"
        else -> goal
    }

    /** 统计天数选项 */
    val periodOptions = listOf(7, 14, 30)

    /** 统计天数选项标签 */
    fun getPeriodLabel(days: Int): String = when (days) {
        7 -> "近7天"
        14 -> "近14天"
        30 -> "近30天"
        else -> "近${days}天"
    }
}
