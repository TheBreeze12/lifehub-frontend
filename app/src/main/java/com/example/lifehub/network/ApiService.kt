package com.example.lifehub.network

import com.example.lifehub.data.*
import okhttp3.MultipartBody
import retrofit2.http.*

/** API服务接口定义 - MVP版本完整接口 */
interface ApiService {

        // ==================== 餐饮识别服务 ====================

        /**
         * 上传菜单图片识别 POST /api/food/recognize
         * @param image 菜单图片文件
         * @param userId 用户ID（可选）
         * @return 识别出的菜品列表
         */
        @Multipart
        @POST("/api/food/recognize")
        suspend fun recognizeMenu(
                @Part image: MultipartBody.Part,
                @Part("userId") userId: okhttp3.RequestBody?
        ): RecognizeMenuResponse

        /**
         * 获取最新的菜单识别结果 GET /api/food/latest-recognition
         * @param userId 用户ID（可选）
         * @return 最新的识别结果
         */
        @GET("/api/food/latest-recognition")
        suspend fun getLatestRecognition(
                @Query("userId") userId: Int? = null
        ): RecognizeMenuResponse

        /**
         * 分析单个菜品营养成分（文本查询） POST /api/food/analyze
         * @param request 包含菜品名称的请求体
         * @return 菜品营养分析结果
         */
        @POST("/api/food/analyze")
        suspend fun analyzeFoodNutrition(@Body request: FoodRequest): FoodResponse

        /**
         * 添加饮食记录 POST /api/food/record
         * @param request 饮食记录数据
         * @return 通用响应
         */
        @POST("/api/food/record")
        suspend fun addDietRecord(@Body request: AddDietRecordRequest): ApiResponse

        /**
         * 获取用户所有饮食记录（按日期划分） GET /api/food/records
         * @param userId 用户ID
         * @return 按日期分组的饮食记录
         */
        @GET("/api/food/records")
        suspend fun getDietRecords(@Query("userId") userId: Int): DietRecordsByDateResponse

        /**
         * 获取用户今天的饮食记录 GET /api/food/records/today
         * @param userId 用户ID
         * @return 今天的饮食记录
         */
        @GET("/api/food/records/today")
        suspend fun getTodayDietRecords(@Query("userId") userId: Int): DietRecordsByDateResponse

        // ==================== 行程规划服务 ====================

        /**
         * 生成行程 POST /api/trip/generate
         * @param request 包含用户输入和偏好的请求
         * @return 生成的行程计划
         */
        @POST("/api/trip/generate")
        suspend fun generateTrip(@Body request: GenerateTripRequest): GenerateTripResponse

        /**
         * 获取行程详情 GET /api/trip/{tripId}
         * @param tripId 行程ID
         * @return 行程详情
         */
        @GET("/api/trip/{tripId}")
        suspend fun getTripDetail(@Path("tripId") tripId: Int): TripDetailResponse

        /**
         * 获取用户所有行程列表 GET /api/trip/list
         * @param userId 用户ID
         * @return 行程列表
         */
        @GET("/api/trip/list")
        suspend fun getTripList(@Query("userId") userId: Int): TripListResponse

        /**
         * 获取用户最近行程 GET /api/trip/recent
         * @param userId 用户ID
         * @param limit 返回数量限制（默认5条）
         * @return 行程列表
         */
        @GET("/api/trip/recent")
        suspend fun getRecentTrips(
                @Query("userId") userId: Int,
                @Query("limit") limit: Int = 5
        ): TripListResponse

        /**
         * 获取首页行程 GET /api/trip/home
         * @param userId 用户ID
         * @param limit 返回数量限制（默认3条）
         * @return 行程列表
         */
        @GET("/api/trip/home")
        suspend fun getHomeTrips(
                @Query("userId") userId: Int,
                @Query("limit") limit: Int = 3
        ): TripListResponse

        // ==================== 用户中心服务 ====================

        /**
         * 获取用户偏好 GET /api/user/preferences
         * @param userId 用户ID
         * @return 用户偏好数据
         */
        @GET("/api/user/preferences")
        suspend fun getUserPreferences(@Query("userId") userId: Int): UserPreferencesResponse

        /**
         * 更新用户偏好设置 PUT /api/user/preferences
         * @param request 用户偏好数据
         * @return 用户偏好响应
         */
        @PUT("/api/user/preferences")
        suspend fun updateUserPreferences(
                @Body request: UpdatePreferencesRequest
        ): UserPreferencesResponse

        /**
         * 获取饮食历史记录 GET /api/user/diet-history
         * @param userId 用户ID
         * @param date 日期（YYYY-MM-DD格式，可选）
         * @return 饮食历史数据
         */

        suspend fun getDietHistory(
                @Query("userId") userId: Int,
                @Query("date") date: String? = null
        ): DietHistoryResponse

        @GET("/api/user/data")
        suspend fun getUserData(
            @Query("userId") userId: Int,
            @Query("password") password: String? = null
        ):UserPreferencesResponse
}
