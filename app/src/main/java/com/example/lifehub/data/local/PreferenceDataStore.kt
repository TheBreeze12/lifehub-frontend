package com.example.lifehub.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.lifehub.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Context扩展属性：创建DataStore实例 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = PreferenceDataStore.DATASTORE_NAME
)

/**
 * DataStore配置存储管理器
 *
 * 功能：
 * - 替代SharedPreferences，使用Jetpack DataStore存储应用配置
 * - 非敏感数据直接存储在DataStore中
 * - 敏感数据（Token、API Key）通过KeystoreManager加密后存储
 * - 提供类型安全的Key定义和默认值
 * - 支持Flow响应式数据读取
 *
 * 数据分类：
 * - 普通配置：用户ID、昵称、主题模式、健康目标等
 * - 敏感配置：Access Token、Refresh Token（Keystore加密存储）
 */
class PreferenceDataStore(private val context: Context) {

    companion object {
        private const val TAG = "PreferenceDataStore"

        /** DataStore文件名 */
        const val DATASTORE_NAME = "lifehub_preferences"
    }

    /** DataStore Key定义 */
    object Keys {
        // === 用户会话相关（非敏感） ===
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val NICKNAME = stringPreferencesKey("nickname")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")

        // === 用户偏好（非敏感） ===
        val HEALTH_GOAL = stringPreferencesKey("health_goal")
        val TRAVEL_PREFERENCE = stringPreferencesKey("travel_preference")
        val DAILY_BUDGET = intPreferencesKey("daily_budget")

        // === 敏感数据（Keystore加密存储） ===
        val ENCRYPTED_ACCESS_TOKEN = stringPreferencesKey("encrypted_access_token")
        val ENCRYPTED_REFRESH_TOKEN = stringPreferencesKey("encrypted_refresh_token")

        // === 应用配置（非敏感） ===
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BASE_URL = stringPreferencesKey("base_url")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val OFFLINE_MODE_ENABLED = booleanPreferencesKey("offline_mode_enabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
    }

    /** 默认值定义 */
    object Defaults {
        const val USER_ID = -1
        const val IS_LOGGED_IN = false
        const val HEALTH_GOAL = "balanced"
        const val THEME_MODE = "system"
        const val NICKNAME = "健康达人"
        const val TRAVEL_PREFERENCE = "walking"
        const val DAILY_BUDGET = 0
        const val BASE_URL = "http://10.0.2.2:8000"
        const val LAST_SYNC_TIME = 0L
        const val OFFLINE_MODE_ENABLED = false
        const val NOTIFICATION_ENABLED = true
    }

    /** 安全Key标识（需要加密存储的Key名称） */
    object SecureKeys {
        val ALL = listOf(
            "encrypted_access_token",
            "encrypted_refresh_token"
        )
    }

    private val dataStore: DataStore<Preferences>
        get() = context.dataStore

    // ==================== 通用读写方法 ====================

    /**
     * 读取Int值
     */
    fun getInt(key: Preferences.Key<Int>, default: Int): Flow<Int> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "读取Int失败", e)
                    emit(emptyPreferences())
                } else throw e
            }
            .map { prefs -> prefs[key] ?: default }
    }

    /**
     * 读取String值
     */
    fun getString(key: Preferences.Key<String>, default: String): Flow<String> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "读取String失败", e)
                    emit(emptyPreferences())
                } else throw e
            }
            .map { prefs -> prefs[key] ?: default }
    }

    /**
     * 读取Boolean值
     */
    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "读取Boolean失败", e)
                    emit(emptyPreferences())
                } else throw e
            }
            .map { prefs -> prefs[key] ?: default }
    }

    /**
     * 读取Long值
     */
    fun getLong(key: Preferences.Key<Long>, default: Long): Flow<Long> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "读取Long失败", e)
                    emit(emptyPreferences())
                } else throw e
            }
            .map { prefs -> prefs[key] ?: default }
    }

    /**
     * 写入Int值
     */
    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * 写入String值
     */
    suspend fun putString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * 写入Boolean值
     */
    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * 写入Long值
     */
    suspend fun putLong(key: Preferences.Key<Long>, value: Long) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    /**
     * 移除指定Key
     */
    suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { prefs -> prefs.remove(key) }
    }

    /**
     * 清除所有数据
     */
    suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    // ==================== 用户会话快捷方法 ====================

    /** 保存登录信息 */
    suspend fun saveLogin(userId: Int, username: String, nickname: String?) {
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.NICKNAME] = nickname ?: Defaults.NICKNAME
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    /** 获取当前用户ID（同步版本，阻塞读取） */
    suspend fun getUserId(): Int {
        return dataStore.data.first()[Keys.USER_ID] ?: Defaults.USER_ID
    }

    /** 获取用户ID Flow */
    fun getUserIdFlow(): Flow<Int> = getInt(Keys.USER_ID, Defaults.USER_ID)

    /** 获取用户名 Flow */
    fun getUsernameFlow(): Flow<String> = getString(Keys.USERNAME, "")

    /** 获取昵称 Flow */
    fun getNicknameFlow(): Flow<String> = getString(Keys.NICKNAME, Defaults.NICKNAME)

    /** 获取登录状态 Flow */
    fun isLoggedInFlow(): Flow<Boolean> = getBoolean(Keys.IS_LOGGED_IN, Defaults.IS_LOGGED_IN)

    /** 同步获取登录状态 */
    suspend fun isLoggedIn(): Boolean {
        return dataStore.data.first()[Keys.IS_LOGGED_IN] ?: Defaults.IS_LOGGED_IN
    }

    /** 退出登录：清除会话数据和加密Token */
    suspend fun logout() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.NICKNAME)
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.ENCRYPTED_ACCESS_TOKEN)
            prefs.remove(Keys.ENCRYPTED_REFRESH_TOKEN)
        }
    }

    // ==================== 敏感数据加密存储 ====================

    /** 加密存储Access Token */
    suspend fun saveAccessToken(token: String) {
        val encrypted = KeystoreManager.encryptString(token)
        if (encrypted != null) {
            putString(Keys.ENCRYPTED_ACCESS_TOKEN, encrypted)
        } else {
            Log.e(TAG, "Access Token加密失败")
        }
    }

    /** 解密读取Access Token */
    suspend fun getAccessToken(): String? {
        val encrypted = dataStore.data.first()[Keys.ENCRYPTED_ACCESS_TOKEN] ?: return null
        return KeystoreManager.decryptString(encrypted)
    }

    /** Access Token Flow（解密后） */
    fun getAccessTokenFlow(): Flow<String?> {
        return dataStore.data
            .catch { e ->
                if (e is IOException) {
                    Log.e(TAG, "读取Access Token失败", e)
                    emit(emptyPreferences())
                } else throw e
            }
            .map { prefs ->
                val encrypted = prefs[Keys.ENCRYPTED_ACCESS_TOKEN]
                if (encrypted != null) KeystoreManager.decryptString(encrypted) else null
            }
    }

    /** 加密存储Refresh Token */
    suspend fun saveRefreshToken(token: String) {
        val encrypted = KeystoreManager.encryptString(token)
        if (encrypted != null) {
            putString(Keys.ENCRYPTED_REFRESH_TOKEN, encrypted)
        } else {
            Log.e(TAG, "Refresh Token加密失败")
        }
    }

    /** 解密读取Refresh Token */
    suspend fun getRefreshToken(): String? {
        val encrypted = dataStore.data.first()[Keys.ENCRYPTED_REFRESH_TOKEN] ?: return null
        return KeystoreManager.decryptString(encrypted)
    }

    // ==================== 应用配置快捷方法 ====================

    /** 获取主题模式 Flow */
    fun getThemeModeFlow(): Flow<String> = getString(Keys.THEME_MODE, Defaults.THEME_MODE)

    /** 设置主题模式 */
    suspend fun setThemeMode(mode: String) {
        putString(Keys.THEME_MODE, mode)
    }

    /** 获取健康目标 Flow */
    fun getHealthGoalFlow(): Flow<String> = getString(Keys.HEALTH_GOAL, Defaults.HEALTH_GOAL)

    /** 设置健康目标 */
    suspend fun setHealthGoal(goal: String) {
        putString(Keys.HEALTH_GOAL, goal)
    }

    /** 获取Base URL Flow */
    fun getBaseUrlFlow(): Flow<String> = getString(Keys.BASE_URL, Defaults.BASE_URL)

    /** 设置Base URL */
    suspend fun setBaseUrl(url: String) {
        putString(Keys.BASE_URL, url)
    }

    /** 获取上次同步时间 Flow */
    fun getLastSyncTimeFlow(): Flow<Long> = getLong(Keys.LAST_SYNC_TIME, Defaults.LAST_SYNC_TIME)

    /** 设置上次同步时间 */
    suspend fun setLastSyncTime(time: Long) {
        putLong(Keys.LAST_SYNC_TIME, time)
    }
}
