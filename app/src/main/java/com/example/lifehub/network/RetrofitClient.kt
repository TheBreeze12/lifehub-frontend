package com.example.lifehub.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Retrofit客户端单例 */
object RetrofitClient {

    // 后端API基础URL
    // 10.0.2.2 是Android模拟器访问本地主机的特殊IP地址
    // 如果使用真机测试，需要改为电脑的局域网IP地址，如：http://192.168.1.100:8000
    private const val BASE_URL = "http://192.168.1.19:8000"

    /** 配置OkHttp客户端 */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
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
