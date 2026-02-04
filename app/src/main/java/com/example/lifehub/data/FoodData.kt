package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** 菜品营养数据 */
data class FoodData(
        @SerializedName("name") val name: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("protein") val protein: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("carbs") val carbs: Double,
        @SerializedName("recommendation") val recommendation: String,
        @SerializedName("allergens") val allergens: List<String>? = null,
        @SerializedName("allergen_reasoning") val allergenReasoning: String? = null
)
