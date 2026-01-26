package com.example.lifehub.data

import android.content.Context
import android.content.SharedPreferences

/** 用户会话管理 - 使用SharedPreferences存储登录状态 */
object UserSession {
    private const val PREFS_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 保存登录信息 */
    fun saveLogin(userId: Int, username: String, nickname: String?) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_NICKNAME, nickname)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /** 获取当前用户ID */
    fun getUserId(): Int? {
        return if (isLoggedIn()) {
            prefs.getInt(KEY_USER_ID, -1).takeIf { it > 0 }
        } else {
            null
        }
    }

    /** 获取当前用户名 */
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    /** 获取当前用户昵称 */
    fun getNickname(): String? {
        return prefs.getString(KEY_NICKNAME, "健康达人")
    }

    /** 检查是否已登录 */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /** 退出登录 */
    fun logout() {
        prefs.edit().clear().apply()
    }
}
