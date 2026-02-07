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

        /**
         * 更新饮食记录 PUT /api/food/diet/{record_id}
         * @param recordId 记录ID
         * @param request 更新请求
         * @return 更新后的记录
         */
        @PUT("/api/food/diet/{record_id}")
        suspend fun updateDietRecord(
                @Path("record_id") recordId: Int,
                @Body request: UpdateDietRecordRequest
        ): UpdateDietRecordResponse

        /**
         * 删除饮食记录 DELETE /api/food/diet/{record_id}
         * @param recordId 记录ID
         * @param userId 用户ID（权限校验）
         * @return 删除结果
         */
        @DELETE("/api/food/diet/{record_id}")
        suspend fun deleteDietRecord(
                @Path("record_id") recordId: Int,
                @Query("userId") userId: Int
        ): ApiResponse

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
        @GET("/api/user/diet-history")
        suspend fun getDietHistory(
                @Query("userId") userId: Int,
                @Query("date") date: String? = null
        ): DietHistoryResponse

        @GET("/api/user/data")
        suspend fun getUserData(
                @Query("nickname") nickname: String? = null,
                @Query("password") password: String? = null
        ): UserPreferencesResponse

    /**
     * 用户注册 POST /api/user/register
     * @param request 注册请求（昵称和密码）
     * @return 注册响应（包含用户ID）
     */
    @POST("/api/user/register")
    suspend fun registerUser(@Body request: UserRegistrationRequest): UserRegistrationResponse

                // ==================== 天气服务 ====================

                /**
                 * 根据计划ID查询天气 GET /api/weather/by-plan
                 * @param planId 行程计划ID
                 * @return 天气数据
                 */
                @GET("/api/weather/by-plan")
                suspend fun getWeatherByPlan(@Query("planId") planId: Int): WeatherResponse

        // ==================== 天气动态调整 Plan B 服务 (Phase 33) ====================

        /**
         * 获取运动计划的Plan B（天气动态调整方案） GET /api/trip/plan-b/{plan_id}
         * @param planId 运动计划ID
         * @return Plan B数据（含天气评估和室内替代方案）
         */
        @GET("/api/trip/plan-b/{plan_id}")
        suspend fun getPlanB(@Path("plan_id") planId: Int): PlanBResponse

        // ==================== 餐前餐后对比服务 ====================

        /**
         * 上传餐前图片 POST /api/food/meal/before
         * @param image 餐前食物图片
         * @param userId 用户ID
         * @return 餐前分析结果，包含comparison_id
         */
        @Multipart
        @POST("/api/food/meal/before")
        suspend fun uploadBeforeMealImage(
                @Part image: MultipartBody.Part,
                @Part("user_id") userId: okhttp3.RequestBody
        ): BeforeMealResponse

        /**
         * 上传餐后图片并计算净摄入 POST /api/food/meal/after/{comparison_id}
         * @param comparisonId 对比记录ID（餐前上传时返回）
         * @param image 餐后食物图片
         * @return 对比结果，包含净摄入热量
         */
        @Multipart
        @POST("/api/food/meal/after/{comparison_id}")
        suspend fun uploadAfterMealImage(
                @Path("comparison_id") comparisonId: Int,
                @Part image: MultipartBody.Part
        ): AfterMealResponse

        // ==================== 个性化菜品推荐服务 (Phase 42) ====================

        /**
         * 获取个性化菜品推荐 GET /api/food/recommend
         * @param userId 用户ID
         * @param mealType 餐次（breakfast/lunch/dinner/snack）
         * @param limit 返回推荐数量
         * @return 推荐菜品列表
         */
        @GET("/api/food/recommend")
        suspend fun getFoodRecommendations(
                @Query("user_id") userId: Int,
                @Query("meal_type") mealType: String = "lunch",
                @Query("limit") limit: Int = 5
        ): RecommendationResponse

        // ==================== 数据统计服务 (Phase 17) ====================

        /**
         * 获取每日热量统计 GET /api/stats/calories/daily
         * @param userId 用户ID
         * @param date 统计日期（YYYY-MM-DD格式）
         * @return 每日热量统计数据
         */
        @GET("/api/stats/calories/daily")
        suspend fun getDailyCalorieStats(
                @Query("userId") userId: Int,
                @Query("date") date: String
        ): DailyCalorieStatsResponse

        /**
         * 获取每周热量统计 GET /api/stats/calories/weekly
         * @param userId 用户ID
         * @param weekStart 周起始日期（YYYY-MM-DD格式，应为周一）
         * @return 每周热量统计数据
         */
        @GET("/api/stats/calories/weekly")
        suspend fun getWeeklyCalorieStats(
                @Query("userId") userId: Int,
                @Query("week_start") weekStart: String
        ): WeeklyCalorieStatsResponse

        // ==================== 营养素统计服务 (Phase 18) ====================

        /**
         * 获取每日营养素统计 GET /api/stats/nutrients/daily
         * @param userId 用户ID
         * @param date 统计日期（YYYY-MM-DD格式）
         * @return 每日营养素统计数据（包含与膳食指南对比）
         */
        @GET("/api/stats/nutrients/daily")
        suspend fun getDailyNutrientStats(
                @Query("userId") userId: Int,
                @Query("date") date: String
        ): DailyNutrientStatsResponse

        // ==================== 帕累托路径规划服务 (Phase 24) ====================

        /**
         * 生成帕累托最优运动路径 POST /api/trip/routes
         * @param request 路径生成请求（起点坐标、目标热量等）
         * @return 2-3条帕累托最优路径
         */
        @POST("/api/trip/routes")
        suspend fun generateRoutes(@Body request: GenerateRoutesRequest): GenerateRoutesResponse

        // ==================== 运动记录服务 (Phase 28) ====================

        /**
         * 新增运动记录 POST /api/exercise/record
         * @param request 运动记录数据
         * @return 创建结果（包含达成率）
         */
        @POST("/api/exercise/record")
        suspend fun createExerciseRecord(
                @Body request: CreateExerciseRecordRequest
        ): CreateExerciseRecordResponse

        // ==================== 运动历史记录服务 (Phase 49) ====================

        /**
         * 查询运动记录列表 GET /api/exercise/records
         * @param userId 用户ID
         * @param exerciseDate 按日期筛选（YYYY-MM-DD格式，可选）
         * @param exerciseType 按运动类型筛选（可选）
         * @param limit 返回数量限制（默认50）
         * @param offset 偏移量（默认0）
         * @return 运动记录列表
         */
        @GET("/api/exercise/records")
        suspend fun getExerciseRecords(
                @Query("userId") userId: Int,
                @Query("exercise_date") exerciseDate: String? = null,
                @Query("exercise_type") exerciseType: String? = null,
                @Query("limit") limit: Int = 50,
                @Query("offset") offset: Int = 0
        ): ExerciseRecordListResponse

        /**
         * 查询运动记录详情 GET /api/exercise/record/{record_id}
         * @param recordId 运动记录ID
         * @param userId 用户ID（用于权限校验）
         * @return 运动记录详情
         */
        @GET("/api/exercise/record/{record_id}")
        suspend fun getExerciseRecordDetail(
                @Path("record_id") recordId: Int,
                @Query("userId") userId: Int
        ): ExerciseRecordDetailResponse

        // ==================== 健康目标达成率服务 (Phase 48) ====================

        /**
         * 获取健康目标达成率 GET /api/stats/goal-progress
         * @param userId 用户ID
         * @param days 统计天数（1-90，默认7）
         * @return 多维度达成率、综合得分、个性化建议、连续记录天数
         */
        @GET("/api/stats/goal-progress")
        suspend fun getGoalProgress(
                @Query("userId") userId: Int,
                @Query("days") days: Int = 7
        ): GoalProgressResponse

        // ==================== 运动频率分析服务 (Phase 51) ====================

        /**
         * 获取运动频率分析 GET /api/stats/exercise-frequency
         * @param userId 用户ID
         * @param period 统计周期：week=最近一周，month=最近一个月
         * @return 运动频率分析数据（含每日明细、类型分布、评级建议）
         */
        @GET("/api/stats/exercise-frequency")
        suspend fun getExerciseFrequency(
                @Query("user_id") userId: Int,
                @Query("period") period: String = "week"
        ): ExerciseFrequencyResponse

        // ==================== 一键遗忘服务 (Phase 55) ====================

        /**
         * 一键遗忘 - 删除用户所有数据 DELETE /api/user/data
         * @param userId 用户ID
         * @return 删除结果（含各表删除数量统计）
         */
        @DELETE("/api/user/data")
        suspend fun deleteUserData(@Query("userId") userId: Int): DataForgetResponse

        // ==================== 离线运动包服务 (Phase 47) ====================

        /**
         * 生成离线运动包 POST /api/trip/offline-package
         * @param request 包含plan_id的请求
         * @return 离线包生成结果（含package_id、文件大小等）
         */
        @POST("/api/trip/offline-package")
        suspend fun generateOfflinePackage(
                @Body request: OfflinePackageRequest
        ): OfflinePackageResponse

        /**
         * 下载离线运动包 GET /api/trip/offline-package/{package_id}
         * @param packageId 离线包唯一标识
         * @return 离线包ZIP文件（二进制流）
         */
        @Streaming
        @GET("/api/trip/offline-package/{package_id}")
        suspend fun downloadOfflinePackage(
                @Path("package_id") packageId: String
        ): okhttp3.ResponseBody

        // ==================== Phase 56: AI调用日志 ====================

        /**
         * 获取用户AI调用日志列表 GET /api/user/ai-logs
         * @param userId 用户ID
         * @param callType 调用类型过滤（可选）
         * @param limit 返回数量限制
         * @param offset 偏移量
         * @return AI调用日志列表
         */
        @GET("/api/user/ai-logs")
        suspend fun getAiCallLogs(
                @Query("user_id") userId: Int,
                @Query("call_type") callType: String? = null,
                @Query("limit") limit: Int = 50,
                @Query("offset") offset: Int = 0
        ): AiCallLogResponse

        /**
         * 获取用户AI调用统计 GET /api/user/ai-logs/stats
         * @param userId 用户ID
         * @return AI调用统计数据
         */
        @GET("/api/user/ai-logs/stats")
        suspend fun getAiCallLogStats(
                @Query("user_id") userId: Int
        ): AiCallLogStatsResponse
}


