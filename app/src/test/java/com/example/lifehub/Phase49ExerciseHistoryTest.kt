package com.example.lifehub

import com.example.lifehub.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 49: 运动历史记录查询 - 综合单元测试
 *
 * 测试覆盖：
 * 1. ExerciseRecordListResponse JSON反序列化（完整响应、空列表、分页）
 * 2. ExerciseRecordDetailResponse JSON反序列化（完整详情、null data）
 * 3. ExerciseRecordResponseData 字段完整性（所有字段、可选字段、达成率）
 * 4. ExerciseHistoryState 状态机（Idle/Loading/Success/Error）
 * 5. ExerciseDetailState 状态机（Idle/Loading/Success/Error）
 * 6. ExerciseTypeUtils 工具类（类型标签、emoji映射）
 * 7. ExerciseTrackingUtils 格式化工具（距离、配速、时长）
 * 8. 边界条件（零值、超大值、空字段、特殊运动类型）
 * 9. 多运动类型场景（walking/running/cycling/hiking/swimming/gym等）
 * 10. JSON snake_case字段映射验证
 */
class Phase49ExerciseHistoryTest {

    private lateinit var gson: Gson

    @Before
    fun setUp() {
        gson = GsonBuilder().create()
    }

    // ==================== ExerciseRecordListResponse 反序列化测试 ====================

    @Test
    fun `ExerciseRecordListResponse - 完整JSON反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": [
                {
                    "id": 1,
                    "user_id": 1,
                    "plan_id": 10,
                    "exercise_type": "running",
                    "actual_calories": 280.0,
                    "actual_duration": 35,
                    "distance": 4500.0,
                    "route_data": null,
                    "planned_calories": 300.0,
                    "planned_duration": 30,
                    "exercise_date": "2026-02-07",
                    "started_at": "2026-02-07T18:00:00",
                    "ended_at": "2026-02-07T18:35:00",
                    "notes": "沿河跑步，感觉不错",
                    "created_at": "2026-02-07T18:40:00",
                    "calories_achievement": 93.3,
                    "duration_achievement": 116.7
                },
                {
                    "id": 2,
                    "user_id": 1,
                    "plan_id": null,
                    "exercise_type": "walking",
                    "actual_calories": 120.0,
                    "actual_duration": 45,
                    "distance": 3200.0,
                    "route_data": null,
                    "planned_calories": null,
                    "planned_duration": null,
                    "exercise_date": "2026-02-06",
                    "started_at": null,
                    "ended_at": null,
                    "notes": null,
                    "created_at": "2026-02-06T19:00:00",
                    "calories_achievement": null,
                    "duration_achievement": null
                }
            ],
            "total": 2
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertEquals(200, response.code)
        assertEquals("获取成功", response.message)
        assertEquals(2, response.data.size)
        assertEquals(2, response.total)

        // 验证第一条记录（完整字段）
        val record1 = response.data[0]
        assertEquals(1, record1.id)
        assertEquals(1, record1.userId)
        assertEquals(10, record1.planId)
        assertEquals("running", record1.exerciseType)
        assertEquals(280.0, record1.actualCalories, 0.001)
        assertEquals(35, record1.actualDuration)
        assertEquals(4500.0, record1.distance!!, 0.001)
        assertNull(record1.routeData)
        assertEquals(300.0, record1.plannedCalories!!, 0.001)
        assertEquals(30, record1.plannedDuration)
        assertEquals("2026-02-07", record1.exerciseDate)
        assertEquals("2026-02-07T18:00:00", record1.startedAt)
        assertEquals("2026-02-07T18:35:00", record1.endedAt)
        assertEquals("沿河跑步，感觉不错", record1.notes)
        assertEquals("2026-02-07T18:40:00", record1.createdAt)
        assertEquals(93.3, record1.caloriesAchievement!!, 0.001)
        assertEquals(116.7, record1.durationAchievement!!, 0.001)

        // 验证第二条记录（可选字段为null）
        val record2 = response.data[1]
        assertEquals(2, record2.id)
        assertNull(record2.planId)
        assertEquals("walking", record2.exerciseType)
        assertEquals(120.0, record2.actualCalories, 0.001)
        assertNull(record2.plannedCalories)
        assertNull(record2.plannedDuration)
        assertNull(record2.startedAt)
        assertNull(record2.endedAt)
        assertNull(record2.notes)
        assertNull(record2.caloriesAchievement)
        assertNull(record2.durationAchievement)
    }

    @Test
    fun `ExerciseRecordListResponse - 空列表反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": [],
            "total": 0
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertEquals(200, response.code)
        assertTrue(response.data.isEmpty())
        assertEquals(0, response.total)
    }

    @Test
    fun `ExerciseRecordListResponse - 分页场景total大于data长度`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": [
                {
                    "id": 1,
                    "user_id": 1,
                    "exercise_type": "walking",
                    "actual_calories": 100.0,
                    "actual_duration": 30,
                    "exercise_date": "2026-02-07",
                    "created_at": "2026-02-07T10:00:00"
                }
            ],
            "total": 50
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertEquals(1, response.data.size)
        assertEquals(50, response.total)
    }

    @Test
    fun `ExerciseRecordListResponse - 错误响应反序列化`() {
        val json = """
        {
            "code": 400,
            "message": "日期格式错误，请使用 YYYY-MM-DD 格式",
            "data": [],
            "total": 0
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertEquals(400, response.code)
        assertEquals("日期格式错误，请使用 YYYY-MM-DD 格式", response.message)
        assertTrue(response.data.isEmpty())
    }

    // ==================== ExerciseRecordDetailResponse 反序列化测试 ====================

    @Test
    fun `ExerciseRecordDetailResponse - 完整JSON反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": {
                "id": 1,
                "user_id": 1,
                "plan_id": 5,
                "exercise_type": "cycling",
                "actual_calories": 450.0,
                "actual_duration": 60,
                "distance": 15000.0,
                "route_data": "[{\"lat\":39.99,\"lng\":116.47}]",
                "planned_calories": 400.0,
                "planned_duration": 50,
                "exercise_date": "2026-02-07",
                "started_at": "2026-02-07T07:00:00",
                "ended_at": "2026-02-07T08:00:00",
                "notes": "晨骑，天气很好",
                "created_at": "2026-02-07T08:05:00",
                "calories_achievement": 112.5,
                "duration_achievement": 120.0
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordDetailResponse::class.java)
        assertEquals(200, response.code)
        assertNotNull(response.data)

        val record = response.data!!
        assertEquals(1, record.id)
        assertEquals("cycling", record.exerciseType)
        assertEquals(450.0, record.actualCalories, 0.001)
        assertEquals(60, record.actualDuration)
        assertEquals(15000.0, record.distance!!, 0.001)
        assertNotNull(record.routeData)
        assertEquals(112.5, record.caloriesAchievement!!, 0.001)
        assertEquals(120.0, record.durationAchievement!!, 0.001)
    }

    @Test
    fun `ExerciseRecordDetailResponse - data为null`() {
        val json = """
        {
            "code": 404,
            "message": "运动记录不存在，record_id: 999",
            "data": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordDetailResponse::class.java)
        assertEquals(404, response.code)
        assertNull(response.data)
    }

    @Test
    fun `ExerciseRecordDetailResponse - 权限校验失败`() {
        val json = """
        {
            "code": 403,
            "message": "无权查看此运动记录，只能查看自己的记录",
            "data": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordDetailResponse::class.java)
        assertEquals(403, response.code)
        assertEquals("无权查看此运动记录，只能查看自己的记录", response.message)
        assertNull(response.data)
    }

    // ==================== ExerciseRecordResponseData 字段完整性测试 ====================

    @Test
    fun `ExerciseRecordResponseData - 所有字段完整构造`() {
        val record = createSampleRecord()
        assertEquals(1, record.id)
        assertEquals(1, record.userId)
        assertEquals(10, record.planId)
        assertEquals("running", record.exerciseType)
        assertEquals(280.0, record.actualCalories, 0.001)
        assertEquals(35, record.actualDuration)
        assertEquals(4500.0, record.distance!!, 0.001)
        assertEquals("2026-02-07", record.exerciseDate)
        assertEquals("2026-02-07T18:00:00", record.startedAt)
        assertEquals("2026-02-07T18:35:00", record.endedAt)
        assertEquals("沿河跑步", record.notes)
        assertEquals("2026-02-07T18:40:00", record.createdAt)
        assertEquals(93.3, record.caloriesAchievement!!, 0.001)
        assertEquals(116.7, record.durationAchievement!!, 0.001)
    }

    @Test
    fun `ExerciseRecordResponseData - 最小字段构造`() {
        val record = ExerciseRecordResponseData(
            id = 1,
            userId = 1,
            exerciseType = "walking",
            actualCalories = 100.0,
            actualDuration = 30,
            exerciseDate = "2026-02-07",
            createdAt = "2026-02-07T10:00:00"
        )
        assertEquals(1, record.id)
        assertNull(record.planId)
        assertNull(record.distance)
        assertNull(record.routeData)
        assertNull(record.plannedCalories)
        assertNull(record.plannedDuration)
        assertNull(record.startedAt)
        assertNull(record.endedAt)
        assertNull(record.notes)
        assertNull(record.caloriesAchievement)
        assertNull(record.durationAchievement)
    }

    @Test
    fun `ExerciseRecordResponseData - 达成率超过100%`() {
        val record = ExerciseRecordResponseData(
            id = 1,
            userId = 1,
            exerciseType = "running",
            actualCalories = 350.0,
            actualDuration = 45,
            exerciseDate = "2026-02-07",
            createdAt = "2026-02-07T10:00:00",
            plannedCalories = 300.0,
            plannedDuration = 30,
            caloriesAchievement = 116.7,
            durationAchievement = 150.0
        )
        assertTrue(record.caloriesAchievement!! > 100.0)
        assertTrue(record.durationAchievement!! > 100.0)
    }

    @Test
    fun `ExerciseRecordResponseData - 零消耗和零时长`() {
        val record = ExerciseRecordResponseData(
            id = 1,
            userId = 1,
            exerciseType = "walking",
            actualCalories = 0.0,
            actualDuration = 0,
            exerciseDate = "2026-02-07",
            createdAt = "2026-02-07T10:00:00"
        )
        assertEquals(0.0, record.actualCalories, 0.001)
        assertEquals(0, record.actualDuration)
    }

    @Test
    fun `ExerciseRecordResponseData - 距离为零`() {
        val record = ExerciseRecordResponseData(
            id = 1,
            userId = 1,
            exerciseType = "gym",
            actualCalories = 200.0,
            actualDuration = 40,
            distance = 0.0,
            exerciseDate = "2026-02-07",
            createdAt = "2026-02-07T10:00:00"
        )
        assertEquals(0.0, record.distance!!, 0.001)
    }

    @Test
    fun `ExerciseRecordResponseData - 极大热量值`() {
        val record = ExerciseRecordResponseData(
            id = 1,
            userId = 1,
            exerciseType = "cycling",
            actualCalories = 9999.9,
            actualDuration = 480,
            distance = 100000.0,
            exerciseDate = "2026-02-07",
            createdAt = "2026-02-07T10:00:00"
        )
        assertEquals(9999.9, record.actualCalories, 0.001)
        assertEquals(480, record.actualDuration)
        assertEquals(100000.0, record.distance!!, 0.001)
    }

    // ==================== ExerciseHistoryState 状态机测试 ====================

    @Test
    fun `ExerciseHistoryState - Idle状态`() {
        val state: ExerciseHistoryState = ExerciseHistoryState.Idle
        assertTrue(state is ExerciseHistoryState.Idle)
        assertFalse(state is ExerciseHistoryState.Loading)
        assertFalse(state is ExerciseHistoryState.Success)
        assertFalse(state is ExerciseHistoryState.Error)
    }

    @Test
    fun `ExerciseHistoryState - Loading状态`() {
        val state: ExerciseHistoryState = ExerciseHistoryState.Loading
        assertTrue(state is ExerciseHistoryState.Loading)
        assertFalse(state is ExerciseHistoryState.Idle)
    }

    @Test
    fun `ExerciseHistoryState - Success状态携带数据`() {
        val records = listOf(createSampleRecord(), createSampleRecord(id = 2))
        val state: ExerciseHistoryState = ExerciseHistoryState.Success(records, total = 10)
        assertTrue(state is ExerciseHistoryState.Success)
        val successState = state as ExerciseHistoryState.Success
        assertEquals(2, successState.records.size)
        assertEquals(10, successState.total)
    }

    @Test
    fun `ExerciseHistoryState - Success状态空列表`() {
        val state: ExerciseHistoryState = ExerciseHistoryState.Success(emptyList(), total = 0)
        val successState = state as ExerciseHistoryState.Success
        assertTrue(successState.records.isEmpty())
        assertEquals(0, successState.total)
    }

    @Test
    fun `ExerciseHistoryState - Error状态携带消息`() {
        val state: ExerciseHistoryState = ExerciseHistoryState.Error("网络请求失败")
        assertTrue(state is ExerciseHistoryState.Error)
        assertEquals("网络请求失败", (state as ExerciseHistoryState.Error).message)
    }

    @Test
    fun `ExerciseHistoryState - Error状态空消息`() {
        val state: ExerciseHistoryState = ExerciseHistoryState.Error("")
        assertEquals("", (state as ExerciseHistoryState.Error).message)
    }

    // ==================== ExerciseDetailState 状态机测试 ====================

    @Test
    fun `ExerciseDetailState - Idle状态`() {
        val state: ExerciseDetailState = ExerciseDetailState.Idle
        assertTrue(state is ExerciseDetailState.Idle)
    }

    @Test
    fun `ExerciseDetailState - Loading状态`() {
        val state: ExerciseDetailState = ExerciseDetailState.Loading
        assertTrue(state is ExerciseDetailState.Loading)
    }

    @Test
    fun `ExerciseDetailState - Success状态携带记录数据`() {
        val record = createSampleRecord()
        val state: ExerciseDetailState = ExerciseDetailState.Success(record)
        assertTrue(state is ExerciseDetailState.Success)
        val successState = state as ExerciseDetailState.Success
        assertEquals(1, successState.record.id)
        assertEquals("running", successState.record.exerciseType)
        assertEquals(280.0, successState.record.actualCalories, 0.001)
    }

    @Test
    fun `ExerciseDetailState - Error状态`() {
        val state: ExerciseDetailState = ExerciseDetailState.Error("运动记录不存在")
        assertTrue(state is ExerciseDetailState.Error)
        assertEquals("运动记录不存在", (state as ExerciseDetailState.Error).message)
    }

    // ==================== ExerciseTypeUtils 工具类测试 ====================

    @Test
    fun `ExerciseTypeUtils - getTypeLabel所有已知类型`() {
        assertEquals("散步", ExerciseTypeUtils.getTypeLabel("walking"))
        assertEquals("跑步", ExerciseTypeUtils.getTypeLabel("running"))
        assertEquals("骑行", ExerciseTypeUtils.getTypeLabel("cycling"))
        assertEquals("慢跑", ExerciseTypeUtils.getTypeLabel("jogging"))
        assertEquals("徒步", ExerciseTypeUtils.getTypeLabel("hiking"))
        assertEquals("游泳", ExerciseTypeUtils.getTypeLabel("swimming"))
        assertEquals("健身房", ExerciseTypeUtils.getTypeLabel("gym"))
        assertEquals("室内运动", ExerciseTypeUtils.getTypeLabel("indoor"))
        assertEquals("户外运动", ExerciseTypeUtils.getTypeLabel("outdoor"))
    }

    @Test
    fun `ExerciseTypeUtils - getTypeLabel未知类型返回原值`() {
        assertEquals("custom_sport", ExerciseTypeUtils.getTypeLabel("custom_sport"))
        assertEquals("", ExerciseTypeUtils.getTypeLabel(""))
        assertEquals("yoga", ExerciseTypeUtils.getTypeLabel("yoga"))
    }

    @Test
    fun `ExerciseTypeUtils - getTypeEmoji所有已知类型`() {
        assertEquals("🚶", ExerciseTypeUtils.getTypeEmoji("walking"))
        assertEquals("🏃", ExerciseTypeUtils.getTypeEmoji("running"))
        assertEquals("🚴", ExerciseTypeUtils.getTypeEmoji("cycling"))
        assertEquals("🏃", ExerciseTypeUtils.getTypeEmoji("jogging"))
        assertEquals("🥾", ExerciseTypeUtils.getTypeEmoji("hiking"))
        assertEquals("🏊", ExerciseTypeUtils.getTypeEmoji("swimming"))
        assertEquals("🏋️", ExerciseTypeUtils.getTypeEmoji("gym"))
        assertEquals("🏠", ExerciseTypeUtils.getTypeEmoji("indoor"))
        assertEquals("🌳", ExerciseTypeUtils.getTypeEmoji("outdoor"))
    }

    @Test
    fun `ExerciseTypeUtils - getTypeEmoji未知类型返回默认`() {
        assertEquals("🏅", ExerciseTypeUtils.getTypeEmoji("unknown"))
        assertEquals("🏅", ExerciseTypeUtils.getTypeEmoji(""))
        assertEquals("🏅", ExerciseTypeUtils.getTypeEmoji("yoga"))
    }

    // ==================== ExerciseTrackingUtils 格式化工具测试 ====================

    @Test
    fun `ExerciseTrackingUtils - formatDistance 小于1km`() {
        assertEquals("0 m", ExerciseTrackingUtils.formatDistance(0.0))
        assertEquals("500 m", ExerciseTrackingUtils.formatDistance(500.0))
        assertEquals("999 m", ExerciseTrackingUtils.formatDistance(999.0))
    }

    @Test
    fun `ExerciseTrackingUtils - formatDistance 大于等于1km`() {
        assertEquals("1.00 km", ExerciseTrackingUtils.formatDistance(1000.0))
        assertEquals("4.50 km", ExerciseTrackingUtils.formatDistance(4500.0))
        assertEquals("15.00 km", ExerciseTrackingUtils.formatDistance(15000.0))
        assertEquals("42.20 km", ExerciseTrackingUtils.formatDistance(42200.0))
    }

    @Test
    fun `ExerciseTrackingUtils - formatPace 正常配速`() {
        // 5分钟/公里
        assertEquals("5'00\"", ExerciseTrackingUtils.formatPace(5.0))
        // 6分30秒/公里
        assertEquals("6'30\"", ExerciseTrackingUtils.formatPace(6.5))
        // 4分15秒/公里
        assertEquals("4'15\"", ExerciseTrackingUtils.formatPace(4.25))
    }

    @Test
    fun `ExerciseTrackingUtils - formatPace 边界情况`() {
        assertEquals("--'--\"", ExerciseTrackingUtils.formatPace(0.0))
        assertEquals("--'--\"", ExerciseTrackingUtils.formatPace(-1.0))
        assertEquals("--'--\"", ExerciseTrackingUtils.formatPace(61.0))
    }

    @Test
    fun `ExerciseTrackingUtils - formatDuration 各种时长`() {
        // 0秒
        assertEquals("00:00", ExerciseTrackingUtils.formatDuration(0L))
        // 30秒
        assertEquals("00:30", ExerciseTrackingUtils.formatDuration(30000L))
        // 5分钟
        assertEquals("05:00", ExerciseTrackingUtils.formatDuration(300000L))
        // 35分10秒
        assertEquals("35:10", ExerciseTrackingUtils.formatDuration(2110000L))
        // 1小时5分30秒
        assertEquals("1:05:30", ExerciseTrackingUtils.formatDuration(3930000L))
    }

    @Test
    fun `ExerciseTrackingUtils - calculatePace 正常计算`() {
        // 4500米 35分钟 = 35/4.5 ≈ 7.78分钟/km
        val pace = ExerciseTrackingUtils.calculatePace(4500.0, 35 * 60000L)
        assertEquals(7.78, pace, 0.01)
    }

    @Test
    fun `ExerciseTrackingUtils - calculatePace 零距离返回0`() {
        assertEquals(0.0, ExerciseTrackingUtils.calculatePace(0.0, 60000L), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - calculatePace 零时间返回0`() {
        assertEquals(0.0, ExerciseTrackingUtils.calculatePace(1000.0, 0L), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - calculateSpeed 正常计算`() {
        // 4500米 35分钟 = 4.5/0.583 ≈ 7.71 km/h
        val speed = ExerciseTrackingUtils.calculateSpeed(4500.0, 35 * 60000L)
        assertEquals(7.71, speed, 0.1)
    }

    @Test
    fun `ExerciseTrackingUtils - calculateSpeed 零距离返回0`() {
        assertEquals(0.0, ExerciseTrackingUtils.calculateSpeed(0.0, 60000L), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - calculateSpeed 零时间返回0`() {
        assertEquals(0.0, ExerciseTrackingUtils.calculateSpeed(1000.0, 0L), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - estimateCalories 各运动类型`() {
        // 散步 30分钟 70kg: 3.5 * 70 * 0.5 = 122.5 kcal
        assertEquals(122.5, ExerciseTrackingUtils.estimateCalories("walking", 30.0), 0.1)
        // 跑步 30分钟 70kg: 8.0 * 70 * 0.5 = 280.0 kcal
        assertEquals(280.0, ExerciseTrackingUtils.estimateCalories("running", 30.0), 0.1)
        // 骑行 30分钟 70kg: 6.0 * 70 * 0.5 = 210.0 kcal
        assertEquals(210.0, ExerciseTrackingUtils.estimateCalories("cycling", 30.0), 0.1)
        // 徒步 30分钟 70kg: 5.5 * 70 * 0.5 = 192.5 kcal
        assertEquals(192.5, ExerciseTrackingUtils.estimateCalories("hiking", 30.0), 0.1)
        // 未知类型 30分钟 70kg: 4.0 * 70 * 0.5 = 140.0 kcal
        assertEquals(140.0, ExerciseTrackingUtils.estimateCalories("unknown", 30.0), 0.1)
    }

    @Test
    fun `ExerciseTrackingUtils - estimateCalories 零时长返回0`() {
        assertEquals(0.0, ExerciseTrackingUtils.estimateCalories("running", 0.0), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - calculateTotalDistance 空列表`() {
        assertEquals(0.0, ExerciseTrackingUtils.calculateTotalDistance(emptyList()), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - calculateTotalDistance 单点列表`() {
        val points = listOf(TrackPoint(39.99, 116.47))
        assertEquals(0.0, ExerciseTrackingUtils.calculateTotalDistance(points), 0.001)
    }

    @Test
    fun `ExerciseTrackingUtils - calculateTotalDistance 多点列表`() {
        val points = listOf(
            TrackPoint(39.990, 116.470),
            TrackPoint(39.991, 116.470),
            TrackPoint(39.992, 116.470)
        )
        val dist = ExerciseTrackingUtils.calculateTotalDistance(points)
        // 每段约111m，两段约222m
        assertTrue(dist > 200.0)
        assertTrue(dist < 250.0)
    }

    // ==================== JSON序列化/反序列化往返测试 ====================

    @Test
    fun `ExerciseRecordResponseData - JSON往返序列化`() {
        val original = createSampleRecord()
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ExerciseRecordResponseData::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.userId, deserialized.userId)
        assertEquals(original.planId, deserialized.planId)
        assertEquals(original.exerciseType, deserialized.exerciseType)
        assertEquals(original.actualCalories, deserialized.actualCalories, 0.001)
        assertEquals(original.actualDuration, deserialized.actualDuration)
        assertEquals(original.distance!!, deserialized.distance!!, 0.001)
        assertEquals(original.exerciseDate, deserialized.exerciseDate)
        assertEquals(original.startedAt, deserialized.startedAt)
        assertEquals(original.endedAt, deserialized.endedAt)
        assertEquals(original.notes, deserialized.notes)
        assertEquals(original.createdAt, deserialized.createdAt)
        assertEquals(original.caloriesAchievement!!, deserialized.caloriesAchievement!!, 0.001)
        assertEquals(original.durationAchievement!!, deserialized.durationAchievement!!, 0.001)
    }

    @Test
    fun `ExerciseRecordListResponse - JSON往返序列化`() {
        val original = ExerciseRecordListResponse(
            code = 200,
            message = "获取成功",
            data = listOf(createSampleRecord()),
            total = 1
        )
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ExerciseRecordListResponse::class.java)

        assertEquals(original.code, deserialized.code)
        assertEquals(original.message, deserialized.message)
        assertEquals(original.data.size, deserialized.data.size)
        assertEquals(original.total, deserialized.total)
    }

    @Test
    fun `ExerciseRecordDetailResponse - JSON往返序列化`() {
        val original = ExerciseRecordDetailResponse(
            code = 200,
            message = "获取成功",
            data = createSampleRecord()
        )
        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ExerciseRecordDetailResponse::class.java)

        assertEquals(original.code, deserialized.code)
        assertEquals(original.message, deserialized.message)
        assertNotNull(deserialized.data)
        assertEquals(original.data!!.id, deserialized.data!!.id)
    }

    // ==================== snake_case字段映射验证 ====================

    @Test
    fun `ExerciseRecordResponseData - snake_case字段名正确映射`() {
        val json = """
        {
            "id": 5,
            "user_id": 3,
            "plan_id": 7,
            "exercise_type": "hiking",
            "actual_calories": 350.0,
            "actual_duration": 90,
            "distance": 8000.0,
            "route_data": "{\"points\":[]}",
            "planned_calories": 400.0,
            "planned_duration": 120,
            "exercise_date": "2026-02-05",
            "started_at": "2026-02-05T09:00:00",
            "ended_at": "2026-02-05T10:30:00",
            "notes": "山间徒步",
            "created_at": "2026-02-05T11:00:00",
            "calories_achievement": 87.5,
            "duration_achievement": 75.0
        }
        """.trimIndent()

        val record = gson.fromJson(json, ExerciseRecordResponseData::class.java)
        assertEquals(5, record.id)
        assertEquals(3, record.userId)
        assertEquals(7, record.planId)
        assertEquals("hiking", record.exerciseType)
        assertEquals(350.0, record.actualCalories, 0.001)
        assertEquals(90, record.actualDuration)
        assertEquals(8000.0, record.distance!!, 0.001)
        assertNotNull(record.routeData)
        assertEquals(400.0, record.plannedCalories!!, 0.001)
        assertEquals(120, record.plannedDuration)
        assertEquals("2026-02-05", record.exerciseDate)
        assertEquals("2026-02-05T09:00:00", record.startedAt)
        assertEquals("2026-02-05T10:30:00", record.endedAt)
        assertEquals("山间徒步", record.notes)
        assertEquals("2026-02-05T11:00:00", record.createdAt)
        assertEquals(87.5, record.caloriesAchievement!!, 0.001)
        assertEquals(75.0, record.durationAchievement!!, 0.001)
    }

    // ==================== 多运动类型场景测试 ====================

    @Test
    fun `多运动类型列表 - 混合类型正确反序列化`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": [
                {"id": 1, "user_id": 1, "exercise_type": "walking", "actual_calories": 100.0, "actual_duration": 30, "exercise_date": "2026-02-07", "created_at": "2026-02-07T10:00:00"},
                {"id": 2, "user_id": 1, "exercise_type": "running", "actual_calories": 300.0, "actual_duration": 30, "exercise_date": "2026-02-07", "created_at": "2026-02-07T12:00:00"},
                {"id": 3, "user_id": 1, "exercise_type": "cycling", "actual_calories": 250.0, "actual_duration": 45, "exercise_date": "2026-02-06", "created_at": "2026-02-06T08:00:00"},
                {"id": 4, "user_id": 1, "exercise_type": "swimming", "actual_calories": 400.0, "actual_duration": 60, "exercise_date": "2026-02-06", "created_at": "2026-02-06T14:00:00"},
                {"id": 5, "user_id": 1, "exercise_type": "gym", "actual_calories": 200.0, "actual_duration": 50, "exercise_date": "2026-02-05", "created_at": "2026-02-05T18:00:00"}
            ],
            "total": 5
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertEquals(5, response.data.size)

        val types = response.data.map { it.exerciseType }
        assertTrue(types.contains("walking"))
        assertTrue(types.contains("running"))
        assertTrue(types.contains("cycling"))
        assertTrue(types.contains("swimming"))
        assertTrue(types.contains("gym"))

        // 按日期分组验证
        val grouped = response.data.groupBy { it.exerciseDate }
        assertEquals(3, grouped.size)
        assertEquals(2, grouped["2026-02-07"]?.size)
        assertEquals(2, grouped["2026-02-06"]?.size)
        assertEquals(1, grouped["2026-02-05"]?.size)
    }

    @Test
    fun `统计计算 - 总消耗热量和总时长`() {
        val records = listOf(
            createSampleRecord(id = 1, calories = 280.0, duration = 35),
            createSampleRecord(id = 2, calories = 120.0, duration = 45),
            createSampleRecord(id = 3, calories = 350.0, duration = 60)
        )
        val totalCalories = records.sumOf { it.actualCalories }
        val totalDuration = records.sumOf { it.actualDuration }

        assertEquals(750.0, totalCalories, 0.001)
        assertEquals(140, totalDuration)
    }

    @Test
    fun `统计计算 - 总距离`() {
        val records = listOf(
            createSampleRecord(id = 1, distance = 4500.0),
            createSampleRecord(id = 2, distance = 3200.0),
            createSampleRecord(id = 3, distance = null)
        )
        val totalDistance = records.sumOf { it.distance ?: 0.0 }
        assertEquals(7700.0, totalDistance, 0.001)
    }

    // ==================== 日期筛选场景测试 ====================

    @Test
    fun `日期筛选 - 按特定日期筛选后响应正确`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": [
                {"id": 1, "user_id": 1, "exercise_type": "running", "actual_calories": 280.0, "actual_duration": 35, "exercise_date": "2026-02-07", "created_at": "2026-02-07T18:40:00"}
            ],
            "total": 1
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertEquals(1, response.data.size)
        assertEquals("2026-02-07", response.data[0].exerciseDate)
    }

    @Test
    fun `运动类型筛选 - 按类型筛选后仅返回该类型`() {
        val json = """
        {
            "code": 200,
            "message": "获取成功",
            "data": [
                {"id": 1, "user_id": 1, "exercise_type": "running", "actual_calories": 280.0, "actual_duration": 35, "exercise_date": "2026-02-07", "created_at": "2026-02-07T18:40:00"},
                {"id": 3, "user_id": 1, "exercise_type": "running", "actual_calories": 310.0, "actual_duration": 40, "exercise_date": "2026-02-06", "created_at": "2026-02-06T19:00:00"}
            ],
            "total": 2
        }
        """.trimIndent()

        val response = gson.fromJson(json, ExerciseRecordListResponse::class.java)
        assertTrue(response.data.all { it.exerciseType == "running" })
    }

    // ==================== CreateExerciseRecordRequest 模型验证 ====================

    @Test
    fun `CreateExerciseRecordRequest - 完整构造`() {
        val request = CreateExerciseRecordRequest(
            userId = 1,
            planId = 10,
            exerciseType = "running",
            actualCalories = 280.0,
            actualDuration = 35,
            distance = 4500.0,
            exerciseDate = "2026-02-07",
            startedAt = "2026-02-07T18:00:00",
            endedAt = "2026-02-07T18:35:00",
            plannedCalories = 300.0,
            plannedDuration = 30,
            notes = "测试"
        )
        assertEquals(1, request.userId)
        assertEquals(10, request.planId)
        assertEquals("running", request.exerciseType)
        assertEquals(280.0, request.actualCalories, 0.001)
    }

    @Test
    fun `CreateExerciseRecordRequest - 最小必需字段`() {
        val request = CreateExerciseRecordRequest(
            userId = 1,
            actualCalories = 100.0,
            actualDuration = 30,
            exerciseDate = "2026-02-07"
        )
        assertEquals("walking", request.exerciseType) // 默认值
        assertNull(request.planId)
        assertNull(request.distance)
        assertNull(request.routeData)
        assertNull(request.plannedCalories)
        assertNull(request.plannedDuration)
        assertNull(request.startedAt)
        assertNull(request.endedAt)
        assertNull(request.notes)
    }

    @Test
    fun `CreateExerciseRecordRequest - JSON序列化snake_case`() {
        val request = CreateExerciseRecordRequest(
            userId = 1,
            actualCalories = 200.0,
            actualDuration = 40,
            exerciseDate = "2026-02-07"
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("user_id"))
        assertTrue(json.contains("actual_calories"))
        assertTrue(json.contains("actual_duration"))
        assertTrue(json.contains("exercise_date"))
    }

    // ==================== CreateExerciseRecordResponse 测试 ====================

    @Test
    fun `CreateExerciseRecordResponse - 成功响应`() {
        val json = """
        {
            "code": 200,
            "message": "运动记录添加成功",
            "data": {
                "id": 1,
                "user_id": 1,
                "exercise_type": "running",
                "actual_calories": 280.0,
                "actual_duration": 35,
                "exercise_date": "2026-02-07",
                "created_at": "2026-02-07T18:40:00"
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, CreateExerciseRecordResponse::class.java)
        assertEquals(200, response.code)
        assertEquals("运动记录添加成功", response.message)
        assertNotNull(response.data)
        assertEquals(1, response.data!!.id)
    }

    @Test
    fun `CreateExerciseRecordResponse - 失败响应`() {
        val json = """
        {
            "code": 400,
            "message": "不支持的运动类型",
            "data": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, CreateExerciseRecordResponse::class.java)
        assertEquals(400, response.code)
        assertNull(response.data)
    }

    // ==================== SaveExerciseState 测试 ====================

    @Test
    fun `SaveExerciseState - 各状态验证`() {
        val idle: SaveExerciseState = SaveExerciseState.Idle
        assertTrue(idle is SaveExerciseState.Idle)

        val saving: SaveExerciseState = SaveExerciseState.Saving
        assertTrue(saving is SaveExerciseState.Saving)

        val success: SaveExerciseState = SaveExerciseState.Success(42)
        assertTrue(success is SaveExerciseState.Success)
        assertEquals(42, (success as SaveExerciseState.Success).recordId)

        val error: SaveExerciseState = SaveExerciseState.Error("保存失败")
        assertTrue(error is SaveExerciseState.Error)
        assertEquals("保存失败", (error as SaveExerciseState.Error).message)
    }

    // ==================== 辅助方法 ====================

    private fun createSampleRecord(
        id: Int = 1,
        calories: Double = 280.0,
        duration: Int = 35,
        distance: Double? = 4500.0
    ): ExerciseRecordResponseData {
        return ExerciseRecordResponseData(
            id = id,
            userId = 1,
            planId = 10,
            exerciseType = "running",
            actualCalories = calories,
            actualDuration = duration,
            distance = distance,
            routeData = null,
            plannedCalories = 300.0,
            plannedDuration = 30,
            exerciseDate = "2026-02-07",
            startedAt = "2026-02-07T18:00:00",
            endedAt = "2026-02-07T18:35:00",
            notes = "沿河跑步",
            createdAt = "2026-02-07T18:40:00",
            caloriesAchievement = 93.3,
            durationAchievement = 116.7
        )
    }
}
