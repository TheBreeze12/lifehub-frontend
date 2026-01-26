package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** 行程计划数据模型（完整数据，包含items） */
data class TripPlan(
        @SerializedName("tripId") val tripId: Int,
        @SerializedName("title") val title: String,
        @SerializedName("destination") val destination: String?,
        @SerializedName("startDate") val startDate: String,
        @SerializedName("endDate") val endDate: String,
        @SerializedName("items") val items: List<TripItem> = emptyList()
)

/** 行程摘要数据模型（用于列表展示） */
data class TripSummary(
        @SerializedName("tripId") val tripId: Int,
        @SerializedName("title") val title: String,
        @SerializedName("destination") val destination: String?,
        @SerializedName("startDate") val startDate: String,
        @SerializedName("endDate") val endDate: String,
        @SerializedName("status") val status: String?,
        @SerializedName("itemCount") val itemCount: Int = 0
)

/** 行程节点数据模型 */
data class TripItem(
        @SerializedName("dayIndex") val dayIndex: Int,
        @SerializedName("startTime") val startTime: String?,
        @SerializedName("placeName") val placeName: String,
        @SerializedName("placeType")
        val placeType: String?, // attraction/dining/transport/accommodation
        @SerializedName("duration") val duration: Int?, // 分钟
        @SerializedName("cost") val cost: Double?,
        @SerializedName("notes") val notes: String?
)

/** 生成行程请求 */
data class GenerateTripRequest(
        @SerializedName("userId") val userId: Int,
        @SerializedName("query") val query: String,
        @SerializedName("preferences") val preferences: UserPreferences?
)

/** 生成行程响应 */
data class GenerateTripResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: TripPlan?
)

/** 获取行程详情响应 */
data class TripDetailResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: TripPlan?
)

/** 行程列表响应 */
data class TripListResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: List<TripSummary>?
)
