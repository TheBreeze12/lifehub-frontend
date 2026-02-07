package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/**
 * Phase 56: AI调用日志数据模型
 */

/** 单条AI调用日志 */
data class AiCallLogItem(
        @SerializedName("id") val id: Int,
        @SerializedName("user_id") val userId: Int?,
        @SerializedName("call_type") val callType: String,
        @SerializedName("model_name") val modelName: String,
        @SerializedName("input_summary") val inputSummary: String?,
        @SerializedName("output_summary") val outputSummary: String?,
        @SerializedName("success") val success: Boolean,
        @SerializedName("error_message") val errorMessage: String?,
        @SerializedName("latency_ms") val latencyMs: Int?,
        @SerializedName("token_usage") val tokenUsage: Int?,
        @SerializedName("created_at") val createdAt: String?
)

/** AI调用日志列表数据 */
data class AiCallLogListData(
        @SerializedName("total") val total: Int,
        @SerializedName("logs") val logs: List<AiCallLogItem>
)

/** AI调用日志列表API响应 */
data class AiCallLogResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String,
        @SerializedName("data") val data: AiCallLogListData?
)

/** AI调用统计数据 */
data class AiCallLogStatsData(
        @SerializedName("total_calls") val totalCalls: Int,
        @SerializedName("success_count") val successCount: Int,
        @SerializedName("failure_count") val failureCount: Int,
        @SerializedName("success_rate") val successRate: Double,
        @SerializedName("avg_latency_ms") val avgLatencyMs: Double,
        @SerializedName("call_type_distribution") val callTypeDistribution: Map<String, Int>?,
        @SerializedName("model_distribution") val modelDistribution: Map<String, Int>?,
        @SerializedName("recent_7days_count") val recent7daysCount: Int
)

/** AI调用统计API响应 */
data class AiCallLogStatsResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String,
        @SerializedName("data") val data: AiCallLogStatsData?
)

/** AI调用类型中文标签映射 */
object AiCallTypeLabels {
    val labels = mapOf(
            "food_analysis" to "菜品营养分析",
            "menu_recognition" to "菜单图片识别",
            "trip_generation" to "运动计划生成",
            "exercise_intent" to "运动意图提取",
            "allergen_check" to "过敏原检测",
            "meal_comparison" to "餐前餐后对比"
    )

    fun getLabel(callType: String): String = labels[callType] ?: callType
}
