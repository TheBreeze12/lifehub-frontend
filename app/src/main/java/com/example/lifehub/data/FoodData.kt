package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** 烹饪方式对比项（Phase 50） */
data class CookingMethodItem(
        @SerializedName("method") val method: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("description") val description: String
)

/** 菜品营养数据 */
data class FoodData(
        @SerializedName("name") val name: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("protein") val protein: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("carbs") val carbs: Double,
        @SerializedName("recommendation") val recommendation: String,
        @SerializedName("allergens") val allergens: List<String>? = null,
        @SerializedName("allergen_reasoning") val allergenReasoning: String? = null,
        // Phase 50: 烹饪方式热量差异对比
        @SerializedName("cooking_method_comparisons") val cookingMethodComparisons: List<CookingMethodItem>? = null
)
