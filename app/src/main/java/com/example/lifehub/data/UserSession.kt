package com.example.lifehub.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.lifehub.data.local.PreferenceDataStore
import com.example.lifehub.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 用户会话管理
 *
 * Phase 53 迁移：SharedPreferences -> DataStore + Keystore
 * - 非敏感数据（userId, username, nickname, isLoggedIn）迁移到DataStore
 * - 敏感数据（Token）通过KeystoreManager加密后存储在DataStore中
 * - 保留同步API兼容性（内部使用runBlocking桥接DataStore的协程API）
 * - 新增Flow响应式API供Compose UI使用
 */
object UserSession {
    private const val TAG = "UserSession"

    // === 旧版SharedPreferences（用于迁移） ===
    private const val LEGACY_PREFS_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var preferenceDataStore: PreferenceDataStore
    private var legacyPrefs: SharedPreferences? = null
    private var isInitialized = false

    /**
     * 初始化UserSession
     * - 创建PreferenceDataStore实例
     * - 检查并执行旧版SharedPreferences数据迁移
     */
    fun init(context: Context) {
        preferenceDataStore = PreferenceDataStore(context)
        legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        isInitialized = true

        // 执行迁移（如果旧数据存在）
        migrateFromSharedPreferences()
    }

    /**
     * 从旧版SharedPreferences迁移数据到DataStore
     * 迁移后清除旧数据，只执行一次
     */
    private fun migrateFromSharedPreferences() {
        val prefs = legacyPrefs ?: return
        val wasLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

        if (!wasLoggedIn && !prefs.contains(KEY_USER_ID)) {
            // 旧数据不存在，无需迁移
            return
        }

        try {
            val userId = prefs.getInt(KEY_USER_ID, -1)
            val username = prefs.getString(KEY_USERNAME, null)
            val nickname = prefs.getString(KEY_NICKNAME, null)

            if (userId > 0 && username != null) {
                runBlocking {
                    preferenceDataStore.saveLogin(userId, username, nickname)
                }
                Log.i(TAG, "SharedPreferences数据迁移到DataStore完成")
            }

            // 清除旧数据
            prefs.edit().clear().apply()
            Log.i(TAG, "旧版SharedPreferences数据已清除")
        } catch (e: Exception) {
            Log.e(TAG, "数据迁移失败", e)
        }
    }

    // ==================== 同步API（向后兼容） ====================

    /** 保存登录信息 */
    fun saveLogin(userId: Int, username: String, nickname: String?) {
        runBlocking {
            preferenceDataStore.saveLogin(userId, username, nickname)
        }
    }

    /** 获取当前用户ID */
    fun getUserId(): Int? {
        return runBlocking {
            val id = preferenceDataStore.getUserId()
            if (id > 0 && isLoggedIn()) id else null
        }
    }

    /** 获取当前用户名 */
    fun getUsername(): String? {
        return runBlocking {
            preferenceDataStore.getUsernameFlow().first().ifEmpty { null }
        }
    }

    /** 获取当前用户昵称 */
    fun getNickname(): String? {
        return runBlocking {
            preferenceDataStore.getNicknameFlow().first()
        }
    }

    /** 检查是否已登录 */
    fun isLoggedIn(): Boolean {
        return runBlocking {
            preferenceDataStore.isLoggedIn()
        }
    }

    /** 退出登录 */
    fun logout() {
        runBlocking {
            preferenceDataStore.logout()
        }
    }

    // ==================== Token安全存储 ====================

    /** 保存Access Token（Keystore加密存储） */
    fun saveAccessToken(token: String) {
        runBlocking {
            preferenceDataStore.saveAccessToken(token)
        }
    }

    /** 获取Access Token（Keystore解密读取） */
    fun getAccessToken(): String? {
        return runBlocking {
            preferenceDataStore.getAccessToken()
        }
    }

    /** 保存Refresh Token（Keystore加密存储） */
    fun saveRefreshToken(token: String) {
        runBlocking {
            preferenceDataStore.saveRefreshToken(token)
        }
    }

    /** 获取Refresh Token（Keystore解密读取） */
    fun getRefreshToken(): String? {
        return runBlocking {
            preferenceDataStore.getRefreshToken()
        }
    }

    // ==================== Flow API（新增，供Compose使用） ====================

    /** 用户ID Flow */
    fun getUserIdFlow(): Flow<Int> = preferenceDataStore.getUserIdFlow()

    /** 昵称 Flow */
    fun getNicknameFlow(): Flow<String> = preferenceDataStore.getNicknameFlow()

    /** 登录状态 Flow */
    fun isLoggedInFlow(): Flow<Boolean> = preferenceDataStore.isLoggedInFlow()

    /** Access Token Flow */
    fun getAccessTokenFlow(): Flow<String?> = preferenceDataStore.getAccessTokenFlow()

    // ==================== DataStore直接访问（高级用法） ====================

    /** 获取PreferenceDataStore实例 */
    fun getDataStore(): PreferenceDataStore {
        check(isInitialized) { "UserSession未初始化，请先调用init(context)" }
        return preferenceDataStore
    }
}
