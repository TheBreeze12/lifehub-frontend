package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** 单个推荐菜品 */
data class RecommendedFood(
    @SerializedName("food_name") val foodName: String,
    @SerializedName("calories") val calories: Double,
    @SerializedName("protein") val protein: Double,
    @SerializedName("fat") val fat: Double,
    @SerializedName("carbs") val carbs: Double,
    @SerializedName("score") val score: Double,
    @SerializedName("reason") val reason: String,
    @SerializedName("tags") val tags: List<String> = emptyList()
)

/** 推荐结果数据 */
data class RecommendationResultData(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("remaining_calories") val remainingCalories: Double,
    @SerializedName("daily_calorie_target") val dailyCalorieTarget: Double,
    @SerializedName("health_goal") val healthGoal: String,
    @SerializedName("health_goal_label") val healthGoalLabel: String,
    @SerializedName("recommendations") val recommendations: List<RecommendedFood>
)

/** 个性化菜品推荐响应 */
data class RecommendationResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: RecommendationResultData?
)
