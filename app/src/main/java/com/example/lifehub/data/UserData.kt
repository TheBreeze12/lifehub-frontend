package com.example.lifehub.data

import com.google.gson.annotations.SerializedName

/** 用户数据模型 */
data class User(
        @SerializedName("id") val id: Int,
        @SerializedName("nickname") val nickname: String,
        @SerializedName("healthGoal")
        val healthGoal: String, // reduce_fat/gain_muscle/control_sugar/balanced
        @SerializedName("allergens") val allergens: List<String>,
        @SerializedName("travelPreference") val travelPreference: String?,
        @SerializedName("dailyBudget") val dailyBudget: Int?
)

/** 用户偏好设置 */
data class UserPreferences(
        @SerializedName("healthGoal") val healthGoal: String?,
        @SerializedName("allergens") val allergens: List<String>?
)

/** 更新用户偏好请求 */
data class UpdatePreferencesRequest(
        @SerializedName("userId") val userId: Int,
        @SerializedName("healthGoal") val healthGoal: String?,
        @SerializedName("allergens") val allergens: List<String>?,
        @SerializedName("travelPreference") val travelPreference: String?,
        @SerializedName("dailyBudget") val dailyBudget: Int?,
        @SerializedName("weight") val weight: Double? = null,
        @SerializedName("height") val height: Double? = null,
        @SerializedName("age") val age: Int? = null,
        @SerializedName("gender") val gender: String? = null
)

/** 饮食记录数据模型 */
data class DietRecord(
        @SerializedName("id") val id: Int,
        @SerializedName("userId") val userId: Int,
        @SerializedName("foodName") val foodName: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("protein") val protein: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("carbs") val carbs: Double,
        @SerializedName("mealType") val mealType: String, // breakfast/lunch/dinner/snack
        @SerializedName("recordDate") val recordDate: String,
        @SerializedName("createdAt") val createdAt: String
)

/** 添加饮食记录请求 */
data class AddDietRecordRequest(
        @SerializedName("userId") val userId: Int,
        @SerializedName("foodName") val foodName: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("protein") val protein: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("carbs") val carbs: Double,
        @SerializedName("mealType") val mealType: String,
        @SerializedName("recordDate") val recordDate: String
)

/** 更新饮食记录请求 */
data class UpdateDietRecordRequest(
        @SerializedName("userId") val userId: Int,
        @SerializedName("foodName") val foodName: String? = null,
        @SerializedName("calories") val calories: Double? = null,
        @SerializedName("protein") val protein: Double? = null,
        @SerializedName("fat") val fat: Double? = null,
        @SerializedName("carbs") val carbs: Double? = null,
        @SerializedName("mealType") val mealType: String? = null,
        @SerializedName("recordDate") val recordDate: String? = null
)

/** 更新饮食记录响应 */
data class UpdateDietRecordResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: DietRecordData?
)

data class DietRecordData(
        @SerializedName("id") val id: Int,
        @SerializedName("foodName") val foodName: String,
        @SerializedName("calories") val calories: Double,
        @SerializedName("protein") val protein: Double,
        @SerializedName("fat") val fat: Double,
        @SerializedName("carbs") val carbs: Double,
        @SerializedName("mealType") val mealType: String,
        @SerializedName("recordDate") val recordDate: String
)

/** 饮食历史响应 */
data class DietHistoryResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: DietHistoryData?
)

data class DietHistoryData(
        @SerializedName("totalCalories") val totalCalories: Double,
        @SerializedName("targetCalories") val targetCalories: Double,
        @SerializedName("records") val records: List<DietRecord>
)

/** 按日期分组的饮食记录响应 */
data class DietRecordsByDateResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: Map<String, List<DietRecord>>?
)

/** 通用API响应 */
data class ApiResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: Any?
)

/** 登录请求 */
data class LoginRequest(
        @SerializedName("username") val username: String,
        @SerializedName("password") val password: String
)

/** 登录响应 */
data class LoginResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val loginData: LoginData?
)

data class LoginData(
        @SerializedName("userId") val userId: Int,
        @SerializedName("username") val username: String,
        @SerializedName("nickname") val nickname: String?
)

/** 获取用户偏好响应 */
data class UserPreferencesResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: UserPreferencesData?
)

data class UserPreferencesData(
        @SerializedName("userId") val userId: Int,
        @SerializedName("nickname") val nickname: String?,
        @SerializedName("healthGoal") val healthGoal: String?,
        @SerializedName("allergens") val allergens: List<String>?,
        @SerializedName("travelPreference") val travelPreference: String?,
        @SerializedName("dailyBudget") val dailyBudget: Int?,
        @SerializedName("weight") val weight: Double? = null,
        @SerializedName("height") val height: Double? = null,
        @SerializedName("age") val age: Int? = null,
        @SerializedName("gender") val gender: String? = null
)

// UserRegistrationRequest.kt
data class UserRegistrationRequest(val nickname: String, val password: String)

// UserRegistrationResponse.kt
data class UserRegistrationResponse(val code: Int, val message: String, val userId: Int?)

//UserLoginRequest.kt
data class UserLoginRequest(val nickname: String, val password: String)


/** Phase 55: 一键遗忘响应 */
data class DataForgetResponse(
        @SerializedName("code") val code: Int,
        @SerializedName("message") val message: String?,
        @SerializedName("data") val data: DataForgetData?
)

data class DataForgetData(
        @SerializedName("user_id") val userId: Int,
        @SerializedName("nickname") val nickname: String?,
        @SerializedName("deleted_counts") val deletedCounts: DeletedCounts?,
        @SerializedName("total_deleted") val totalDeleted: Int
)

data class DeletedCounts(
        @SerializedName("diet_records") val dietRecords: Int = 0,
        @SerializedName("exercise_records") val exerciseRecords: Int = 0,
        @SerializedName("meal_comparisons") val mealComparisons: Int = 0,
        @SerializedName("menu_recognitions") val menuRecognitions: Int = 0,
        @SerializedName("trip_plans") val tripPlans: Int = 0
)
