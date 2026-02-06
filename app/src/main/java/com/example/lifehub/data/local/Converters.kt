package com.example.lifehub.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverter - Phase 34
 * 用于将复杂类型（如List<String>）与Room支持的基本类型互相转换
 */
class Converters {
    private val gson = Gson()

    /** List<String> -> JSON字符串（用于过敏原列表等） */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    /** JSON字符串 -> List<String> */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
}
