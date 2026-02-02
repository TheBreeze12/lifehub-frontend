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
