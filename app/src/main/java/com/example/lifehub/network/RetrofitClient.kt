package com.example.lifehub.network

import android.util.Log
import com.example.lifehub.data.UserSession
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Retrofit客户端单例 */
object RetrofitClient {

    private const val TAG = "RetrofitClient"

    // 后端API基础URL
    // 10.0.2.2 是Android模拟器访问本地主机的特殊IP地址
    // 如果使用真机测试，需要改为电脑的局域网IP地址，如：http://192.168.1.100:8000
    private const val BASE_URL = "http://192.168.1.19:8000"
//    private const val BASE_URL = "http://10.49.52.252:8000"

    /**
     * Auth Token拦截器 - Phase 53
     * 自动从Keystore安全存储中读取Access Token，添加到请求头
     * 跳过不需要认证的接口（登录、注册、健康检查等）
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // 不需要认证的接口路径
        val publicPaths = listOf(
            "/api/user/register",
            "/api/user/data",
            "/api/user/login",
            "/api/user/refresh",
            "/health",
            "/docs",
            "/redoc",
            "/openapi.json",
            "/"
        )

        // 检查是否为公开接口
        val isPublicPath = publicPaths.any { path.equals(it, ignoreCase = true) }

        if (isPublicPath) {
            chain.proceed(originalRequest)
        } else {
            try {
                val token = UserSession.getAccessToken()
                if (token != null) {
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(authenticatedRequest)
                } else {
                    chain.proceed(originalRequest)
                }
            } catch (e: Exception) {
                Log.w(TAG, "读取Auth Token失败，发送无认证请求", e)
                chain.proceed(originalRequest)
            }
        }
    }

    /** 配置OkHttp客户端 */
    private val okHttpClient: OkHttpClient by lazy  {
        val loggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS) // 增加读取超时到90秒，因为需要分析多个菜品
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
    }

    /** Retrofit实例 */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    /** API服务实例 */
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
