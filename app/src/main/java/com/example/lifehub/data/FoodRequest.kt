package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** 菜品分析请求（文本） */
data class FoodRequest(@SerializedName("food_name") val foodName: String)

/** 菜单识别响应 */
data class RecognizeMenuResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: RecognizeMenuData?
)

data class RecognizeMenuData(@SerializedName("dishes") val dishes: List<DishItem>)

/** 菜品项数据 */
data class DishItem(
        @SerializedName("name") val name: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("protein") val protein: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("carbs") val carbs: Double,
        @SerializedName("isRecommended") val isRecommended: Boolean,
        @SerializedName("reason") val reason: String?
)
