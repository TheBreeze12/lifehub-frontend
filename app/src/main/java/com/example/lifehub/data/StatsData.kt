package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/**
 * Phase 17: 热量收支统计数据模型
 * 对应后端 Phase 15/16 的统计API
 */

/** 每日热量统计数据 */
data class DailyCalorieStats(
    @SerializedName("date") val date: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("intake_calories") val intakeCalories: Double = 0.0,
    @SerializedName("meal_count") val mealCount: Int = 0,
    @SerializedName("burn_calories") val burnCalories: Double = 0.0,
    @SerializedName("exercise_count") val exerciseCount: Int = 0,
    @SerializedName("exercise_duration") val exerciseDuration: Int = 0,
    @SerializedName("net_calories") val netCalories: Double = 0.0,
    @SerializedName("meal_breakdown") val mealBreakdown: Map<String, Double>? = null
)

/** 每日统计明细（用于周统计） */
data class DailyBreakdown(
    @SerializedName("date") val date: String,
    @SerializedName("intake_calories") val intakeCalories: Double = 0.0,
    @SerializedName("burn_calories") val burnCalories: Double = 0.0,
    @SerializedName("net_calories") val netCalories: Double = 0.0
)

/** 每周热量统计数据 */
data class WeeklyCalorieStats(
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("week_end") val weekEnd: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("total_intake") val totalIntake: Double = 0.0,
    @SerializedName("total_burn") val totalBurn: Double = 0.0,
    @SerializedName("total_net") val totalNet: Double = 0.0,
    @SerializedName("avg_intake") val avgIntake: Double = 0.0,
    @SerializedName("avg_burn") val avgBurn: Double = 0.0,
    @SerializedName("avg_net") val avgNet: Double = 0.0,
    @SerializedName("total_meals") val totalMeals: Int = 0,
    @SerializedName("total_exercises") val totalExercises: Int = 0,
    @SerializedName("active_days") val activeDays: Int = 0,
    @SerializedName("daily_breakdown") val dailyBreakdown: List<DailyBreakdown> = emptyList()
)

/** 每日热量统计响应 */
data class DailyCalorieStatsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: DailyCalorieStats?
)

/** 每周热量统计响应 */
data class WeeklyCalorieStatsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: WeeklyCalorieStats?
)

/** 图表数据点（用于Vico图表） */
data class ChartDataPoint(
    val label: String,
    val intake: Float,
    val burn: Float
) {
    val net: Float get() = intake - burn
}

/** 图表视图模式 */
enum class StatsViewMode {
    DAILY,  // 日视图
    WEEKLY  // 周视图
}
