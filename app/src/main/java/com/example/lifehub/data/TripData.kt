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

/** 生成行程请求（现用于运动计划） */
data class GenerateTripRequest(
        @SerializedName("userId") val userId: Int,
        @SerializedName("query") val query: String,
        @SerializedName("preferences") val preferences: UserPreferences?,
        @SerializedName("latitude") val latitude: Double? = null,
        @SerializedName("longitude") val longitude: Double? = null
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

// ==================== Phase 24: 帕累托最优路径数据模型 ====================

/** 路径点数据模型 */
data class RouteWaypoint(
        @SerializedName("lat") val lat: Double,
        @SerializedName("lng") val lng: Double,
        @SerializedName("order") val order: Int = 0,
        @SerializedName("type") val type: String = "waypoint" // start/waypoint/end
)

/** 帕累托最优路径数据模型 */
data class ParetoRoute(
        @SerializedName("route_id") val routeId: Int,
        @SerializedName("route_name") val routeName: String,
        @SerializedName("time_minutes") val timeMinutes: Double,
        @SerializedName("calories_burn") val caloriesBurn: Double,
        @SerializedName("greenery_score") val greeneryScore: Double,
        @SerializedName("distance_meters") val distanceMeters: Double,
        @SerializedName("waypoints") val waypoints: List<RouteWaypoint> = emptyList(),
        @SerializedName("exercise_type") val exerciseType: String? = null,
        @SerializedName("intensity") val intensity: Double? = null
)

/** 生成帕累托路径请求 */
data class GenerateRoutesRequest(
        @SerializedName("start_lat") val startLat: Double,
        @SerializedName("start_lng") val startLng: Double,
        @SerializedName("target_calories") val targetCalories: Double,
        @SerializedName("max_time_minutes") val maxTimeMinutes: Int? = 60,
        @SerializedName("exercise_type") val exerciseType: String? = "walking",
        @SerializedName("weight_kg") val weightKg: Double? = 70.0
)

/** 路径响应数据 */
data class RoutesResponseData(
        @SerializedName("routes") val routes: List<ParetoRoute> = emptyList(),
        @SerializedName("start_point") val startPoint: RouteWaypoint,
        @SerializedName("target_calories") val targetCalories: Double,
        @SerializedName("max_time_minutes") val maxTimeMinutes: Int,
        @SerializedName("exercise_type") val exerciseType: String,
        @SerializedName("weight_kg") val weightKg: Double,
        @SerializedName("n_routes") val nRoutes: Int
)

/** 生成帕累托路径响应 */
data class GenerateRoutesResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: RoutesResponseData?
)

