package com.example.lifehub.data

data class WeatherResponse(
    val code: Int,
    val message: String,
    val data: WeatherData?
)

data class WeatherData(
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val temperature: Double?,
    val windspeed: Double?,
    val winddirection: Double?,
    val weathercode: Int?,
    val time: String?,
    val hourly: HourlyWeather?
)

data class HourlyWeather(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val precipitation: List<Double>?
)

// ==================== Phase 33: Plan B 天气动态调整数据模型 ====================

/** 天气评估结果（对应后端 WeatherAssessment） */
data class WeatherAssessment(
    val is_bad_weather: Boolean,
    val severity: String,       // good / mild / moderate / severe / unknown
    val description: String,
    val temperature: Double?,
    val windspeed: Double?,
    val weathercode: Int?,
    val recommendation: String,
    val warnings: List<String>?
)

/** Plan B 室内替代运动项（对应后端 PlanBAlternative） */
data class PlanBAlternative(
    val exercise_name: String,
    val exercise_type: String,
    val duration: Int,
    val calories: Double,
    val is_indoor: Boolean,
    val description: String,
    val mets_value: Double?
)

/** Plan B 响应数据（对应后端 PlanBData） */
data class PlanBData(
    val plan_id: Int,
    val weather: WeatherAssessment,
    val need_plan_b: Boolean,
    val original_calories: Double,
    val alternatives: List<PlanBAlternative>,
    val plan_b_total_calories: Double,
    val reason: String
)

/** Plan B 响应（对应后端 PlanBResponse） */
data class PlanBResponse(
    val code: Int,
    val message: String,
    val data: PlanBData?
)
