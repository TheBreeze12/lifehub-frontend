package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** API响应数据结构 */
data class FoodResponse(
        @SerializedName("success") val success: Boolean,
        @SerializedName("message") val message: String? = null,
        @SerializedName("data") val data: FoodData? = null
)

/** API请求数据结构 */
//data class FoodRequest(@SerializedName("food_name") val foodName: String)
