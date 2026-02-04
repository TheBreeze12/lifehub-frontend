package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/**
 * 餐前餐后对比相关数据模型
 * Phase 13: 餐前拍摄功能
 */

/** 餐前图片上传响应 */
data class BeforeMealResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: BeforeMealData?
)

/** 餐前图片分析数据 */
data class BeforeMealData(
    @SerializedName("comparison_id") val comparisonId: Int,
    @SerializedName("before_image_url") val beforeImageUrl: String?,
    @SerializedName("before_features") val beforeFeatures: MealFeatures?,
    @SerializedName("status") val status: String?
)

/** 餐后图片上传响应 */
data class AfterMealResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: AfterMealData?
)

/** 餐后图片对比数据 */
data class AfterMealData(
    @SerializedName("comparison_id") val comparisonId: Int,
    @SerializedName("before_image_url") val beforeImageUrl: String?,
    @SerializedName("after_image_url") val afterImageUrl: String?,
    @SerializedName("consumption_ratio") val consumptionRatio: Double,
    @SerializedName("original_calories") val originalCalories: Double,
    @SerializedName("net_calories") val netCalories: Double,
    @SerializedName("original_protein") val originalProtein: Double,
    @SerializedName("original_fat") val originalFat: Double,
    @SerializedName("original_carbs") val originalCarbs: Double,
    @SerializedName("net_protein") val netProtein: Double,
    @SerializedName("net_fat") val netFat: Double,
    @SerializedName("net_carbs") val netCarbs: Double,
    @SerializedName("comparison_analysis") val comparisonAnalysis: String?,
    @SerializedName("status") val status: String?
)

/** 菜品特征（AI识别结果） */
data class MealFeatures(
    @SerializedName("dishes") val dishes: List<DishFeature>?,
    @SerializedName("total_estimated_calories") val totalEstimatedCalories: Double?,
    @SerializedName("total_estimated_protein") val totalEstimatedProtein: Double?,
    @SerializedName("total_estimated_fat") val totalEstimatedFat: Double?,
    @SerializedName("total_estimated_carbs") val totalEstimatedCarbs: Double?
)

/** 单个菜品特征 */
data class DishFeature(
    @SerializedName("name") val name: String,
    @SerializedName("estimated_weight") val estimatedWeight: Int?,
    @SerializedName("estimated_calories") val estimatedCalories: Double?,
    @SerializedName("estimated_protein") val estimatedProtein: Double?,
    @SerializedName("estimated_fat") val estimatedFat: Double?,
    @SerializedName("estimated_carbs") val estimatedCarbs: Double?
)

/** 餐前餐后对比记录（用于本地状态管理） */
data class MealComparisonRecord(
    val comparisonId: Int,
    val beforeImageUrl: String?,
    val afterImageUrl: String? = null,
    val beforeFeatures: MealFeatures? = null,
    val consumptionRatio: Double? = null,
    val originalCalories: Double? = null,
    val netCalories: Double? = null,
    val status: String = "pending_after"
)
