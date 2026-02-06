package com.example.lifehub

import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.ExerciseRecordResponseData
import com.example.lifehub.data.TripItem
import com.example.lifehub.data.TripPlan
import com.example.lifehub.data.TripSummary
import com.example.lifehub.data.UserPreferencesData
import com.example.lifehub.data.local.Converters
import com.example.lifehub.data.local.EntityMapper
import com.example.lifehub.data.local.dao.DietRecordDao
import com.example.lifehub.data.local.dao.ExerciseRecordDao
import com.example.lifehub.data.local.dao.TripPlanDao
import com.example.lifehub.data.local.dao.UserDao
import com.example.lifehub.data.local.entity.DietRecordEntity
import com.example.lifehub.data.local.entity.ExerciseRecordEntity
import com.example.lifehub.data.local.entity.TripPlanEntity
import com.example.lifehub.data.local.entity.UserEntity
import com.example.lifehub.data.repository.DietRepository
import com.example.lifehub.data.repository.ExerciseRepository
import com.example.lifehub.data.repository.TripRepository
import com.example.lifehub.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 34: Room本地存储 综合测试
 * 测试范围：TypeConverters、Entity映射、EntityMapper、Repository逻辑
 * 使用Fake DAO实现来测试Repository层，无需Android环境
 */
class Phase34RoomStorageTest {

    // ==================== 1. TypeConverters 测试 ====================

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun `test fromStringList with normal list`() {
        val list = listOf("花生", "鸡蛋", "牛奶")
        val json = converters.fromStringList(list)
        assertNotNull(json)
        assertTrue(json!!.contains("花生"))
        assertTrue(json.contains("鸡蛋"))
        assertTrue(json.contains("牛奶"))
    }

    @Test
    fun `test fromStringList with empty list`() {
        val json = converters.fromStringList(emptyList())
        assertNotNull(json)
        assertEquals("[]", json)
    }

    @Test
    fun `test fromStringList with null`() {
        val json = converters.fromStringList(null)
        assertNull(json)
    }

    @Test
    fun `test toStringList with valid JSON`() {
        val json = """["花生","鸡蛋","牛奶"]"""
        val list = converters.toStringList(json)
        assertNotNull(list)
        assertEquals(3, list!!.size)
        assertEquals("花生", list[0])
        assertEquals("鸡蛋", list[1])
        assertEquals("牛奶", list[2])
    }

    @Test
    fun `test toStringList with empty array JSON`() {
        val json = "[]"
        val list = converters.toStringList(json)
        assertNotNull(list)
        assertTrue(list!!.isEmpty())
    }

    @Test
    fun `test toStringList with null`() {
        val list = converters.toStringList(null)
        assertNull(list)
    }

    @Test
    fun `test fromStringList and toStringList roundtrip`() {
        val original = listOf("海鲜", "花生", "大豆", "小麦")
        val json = converters.fromStringList(original)
        val restored = converters.toStringList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `test converter with single element list`() {
        val list = listOf("牛奶")
        val json = converters.fromStringList(list)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun `test converter with special characters`() {
        val list = listOf("鱼类(含贝壳)", "树坚果/杏仁", "小麦&麸质")
        val json = converters.fromStringList(list)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun `test converter with unicode characters`() {
        val list = listOf("🥜花生", "🥛牛奶", "🍳鸡蛋")
        val json = converters.fromStringList(list)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    // ==================== 2. Entity 创建与字段测试 ====================

    @Test
    fun `test UserEntity creation with all fields`() {
        val entity = UserEntity(
            id = 1,
            nickname = "测试用户",
            healthGoal = "reduce_fat",
            allergens = listOf("花生", "牛奶"),
            travelPreference = "walking",
            dailyBudget = 500,
            weight = 70.5,
            height = 175.0,
            age = 25,
            gender = "male",
            lastSyncedAt = 1000L
        )
        assertEquals(1, entity.id)
        assertEquals("测试用户", entity.nickname)
        assertEquals("reduce_fat", entity.healthGoal)
        assertEquals(listOf("花生", "牛奶"), entity.allergens)
        assertEquals("walking", entity.travelPreference)
        assertEquals(500, entity.dailyBudget)
        assertEquals(70.5, entity.weight!!, 0.01)
        assertEquals(175.0, entity.height!!, 0.01)
        assertEquals(25, entity.age)
        assertEquals("male", entity.gender)
        assertEquals(1000L, entity.lastSyncedAt)
    }

    @Test
    fun `test UserEntity creation with defaults`() {
        val entity = UserEntity(id = 1)
        assertNull(entity.nickname)
        assertNull(entity.healthGoal)
        assertNull(entity.allergens)
        assertNull(entity.travelPreference)
        assertNull(entity.dailyBudget)
        assertNull(entity.weight)
        assertNull(entity.height)
        assertNull(entity.age)
        assertNull(entity.gender)
        assertTrue(entity.lastSyncedAt > 0)
    }

    @Test
    fun `test DietRecordEntity creation`() {
        val entity = DietRecordEntity(
            localId = 0,
            serverId = 42,
            userId = 1,
            foodName = "番茄炒蛋",
            calories = 150.0,
            protein = 10.5,
            fat = 8.2,
            carbs = 6.3,
            mealType = "lunch",
            recordDate = "2026-02-06",
            createdAt = "2026-02-06T12:00:00",
            isSynced = true
        )
        assertEquals(0L, entity.localId)
        assertEquals(42, entity.serverId)
        assertEquals(1, entity.userId)
        assertEquals("番茄炒蛋", entity.foodName)
        assertEquals(150.0, entity.calories, 0.01)
        assertEquals(10.5, entity.protein, 0.01)
        assertEquals(8.2, entity.fat, 0.01)
        assertEquals(6.3, entity.carbs, 0.01)
        assertEquals("lunch", entity.mealType)
        assertEquals("2026-02-06", entity.recordDate)
        assertTrue(entity.isSynced)
    }

    @Test
    fun `test DietRecordEntity defaults`() {
        val entity = DietRecordEntity(
            userId = 1,
            foodName = "白粥",
            calories = 50.0,
            mealType = "breakfast",
            recordDate = "2026-02-06",
            createdAt = "2026-02-06T08:00:00"
        )
        assertEquals(0L, entity.localId)
        assertNull(entity.serverId)
        assertEquals(0.0, entity.protein, 0.01)
        assertEquals(0.0, entity.fat, 0.01)
        assertEquals(0.0, entity.carbs, 0.01)
        assertFalse(entity.isSynced)
    }

    @Test
    fun `test ExerciseRecordEntity creation`() {
        val entity = ExerciseRecordEntity(
            localId = 0,
            serverId = 10,
            userId = 1,
            planId = 5,
            exerciseType = "running",
            actualCalories = 300.0,
            actualDuration = 30,
            distance = 5000.0,
            exerciseDate = "2026-02-06",
            startedAt = "2026-02-06T19:00:00",
            endedAt = "2026-02-06T19:30:00",
            notes = "公园跑步",
            createdAt = "2026-02-06T19:30:00",
            isSynced = true
        )
        assertEquals(10, entity.serverId)
        assertEquals(1, entity.userId)
        assertEquals(5, entity.planId)
        assertEquals("running", entity.exerciseType)
        assertEquals(300.0, entity.actualCalories, 0.01)
        assertEquals(30, entity.actualDuration)
        assertEquals(5000.0, entity.distance!!, 0.01)
        assertEquals("公园跑步", entity.notes)
        assertTrue(entity.isSynced)
    }

    @Test
    fun `test ExerciseRecordEntity defaults`() {
        val entity = ExerciseRecordEntity(
            userId = 1,
            actualCalories = 100.0,
            actualDuration = 15,
            exerciseDate = "2026-02-06",
            createdAt = "2026-02-06T20:00:00"
        )
        assertEquals(0L, entity.localId)
        assertNull(entity.serverId)
        assertNull(entity.planId)
        assertEquals("walking", entity.exerciseType)
        assertNull(entity.distance)
        assertNull(entity.startedAt)
        assertNull(entity.endedAt)
        assertNull(entity.notes)
        assertFalse(entity.isSynced)
    }

    @Test
    fun `test TripPlanEntity creation`() {
        val entity = TripPlanEntity(
            tripId = 1,
            userId = 1,
            title = "餐后散步",
            destination = "附近公园",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            itemsJson = """[{"dayIndex":1,"placeName":"公园","placeType":"walking","duration":30}]""",
            lastSyncedAt = 1000L
        )
        assertEquals(1, entity.tripId)
        assertEquals(1, entity.userId)
        assertEquals("餐后散步", entity.title)
        assertEquals("附近公园", entity.destination)
        assertTrue(entity.itemsJson.contains("公园"))
    }

    @Test
    fun `test TripPlanEntity defaults`() {
        val entity = TripPlanEntity(
            tripId = 1,
            userId = 1,
            title = "测试",
            startDate = "2026-02-06",
            endDate = "2026-02-06"
        )
        assertNull(entity.destination)
        assertEquals("[]", entity.itemsJson)
        assertTrue(entity.lastSyncedAt > 0)
    }

    // ==================== 3. EntityMapper 测试 ====================

    @Test
    fun `test UserPreferencesData to UserEntity mapping`() {
        val data = UserPreferencesData(
            userId = 1,
            nickname = "测试用户",
            healthGoal = "reduce_fat",
            allergens = listOf("花生"),
            travelPreference = "walking",
            dailyBudget = 500,
            weight = 70.5,
            height = 175.0,
            age = 25,
            gender = "male"
        )
        val entity = EntityMapper.toUserEntity(data)
        assertEquals(data.userId, entity.id)
        assertEquals(data.nickname, entity.nickname)
        assertEquals(data.healthGoal, entity.healthGoal)
        assertEquals(data.allergens, entity.allergens)
        assertEquals(data.weight, entity.weight)
        assertEquals(data.height, entity.height)
        assertEquals(data.age, entity.age)
        assertEquals(data.gender, entity.gender)
    }

    @Test
    fun `test UserEntity to UserPreferencesData mapping`() {
        val entity = UserEntity(
            id = 1,
            nickname = "测试",
            healthGoal = "gain_muscle",
            allergens = listOf("牛奶", "鸡蛋"),
            travelPreference = "self_driving",
            dailyBudget = 300,
            weight = 80.0,
            height = 180.0,
            age = 30,
            gender = "male"
        )
        val data = EntityMapper.toUserPreferencesData(entity)
        assertEquals(entity.id, data.userId)
        assertEquals(entity.nickname, data.nickname)
        assertEquals(entity.healthGoal, data.healthGoal)
        assertEquals(entity.allergens, data.allergens)
        assertEquals(entity.weight, data.weight)
    }

    @Test
    fun `test User mapping roundtrip`() {
        val original = UserPreferencesData(
            userId = 42,
            nickname = "健康达人",
            healthGoal = "balanced",
            allergens = listOf("海鲜", "花生"),
            travelPreference = "public_transport",
            dailyBudget = 200,
            weight = 65.0,
            height = 168.0,
            age = 28,
            gender = "female"
        )
        val entity = EntityMapper.toUserEntity(original)
        val restored = EntityMapper.toUserPreferencesData(entity)
        assertEquals(original.userId, restored.userId)
        assertEquals(original.nickname, restored.nickname)
        assertEquals(original.healthGoal, restored.healthGoal)
        assertEquals(original.allergens, restored.allergens)
        assertEquals(original.travelPreference, restored.travelPreference)
        assertEquals(original.dailyBudget, restored.dailyBudget)
        assertEquals(original.weight, restored.weight)
        assertEquals(original.height, restored.height)
        assertEquals(original.age, restored.age)
        assertEquals(original.gender, restored.gender)
    }

    @Test
    fun `test DietRecord to DietRecordEntity mapping`() {
        val record = DietRecord(
            id = 1,
            userId = 1,
            foodName = "宫保鸡丁",
            calories = 320.0,
            protein = 28.0,
            fat = 18.0,
            carbs = 15.0,
            mealType = "lunch",
            recordDate = "2026-02-06",
            createdAt = "2026-02-06T12:00:00"
        )
        val entity = EntityMapper.toDietRecordEntity(record)
        assertEquals(record.id, entity.serverId)
        assertEquals(record.userId, entity.userId)
        assertEquals(record.foodName, entity.foodName)
        assertEquals(record.calories, entity.calories, 0.01)
        assertEquals(record.protein, entity.protein, 0.01)
        assertTrue(entity.isSynced)
    }

    @Test
    fun `test DietRecordEntity to DietRecord mapping`() {
        val entity = DietRecordEntity(
            localId = 5,
            serverId = 10,
            userId = 1,
            foodName = "番茄炒蛋",
            calories = 150.0,
            protein = 10.5,
            fat = 8.2,
            carbs = 6.3,
            mealType = "dinner",
            recordDate = "2026-02-06",
            createdAt = "2026-02-06T18:00:00",
            isSynced = true
        )
        val record = EntityMapper.toDietRecord(entity)
        assertEquals(10, record.id) // 使用serverId
        assertEquals(entity.userId, record.userId)
        assertEquals(entity.foodName, record.foodName)
        assertEquals(entity.calories, record.calories, 0.01)
    }

    @Test
    fun `test DietRecordEntity to DietRecord mapping uses localId when serverId is null`() {
        val entity = DietRecordEntity(
            localId = 99,
            serverId = null,
            userId = 1,
            foodName = "白粥",
            calories = 50.0,
            mealType = "breakfast",
            recordDate = "2026-02-06",
            createdAt = "2026-02-06T08:00:00",
            isSynced = false
        )
        val record = EntityMapper.toDietRecord(entity)
        assertEquals(99, record.id) // serverId为null时使用localId
    }

    @Test
    fun `test DietRecord batch mapping`() {
        val records = listOf(
            DietRecord(1, 1, "菜A", 100.0, 5.0, 3.0, 10.0, "breakfast", "2026-02-06", "2026-02-06T08:00:00"),
            DietRecord(2, 1, "菜B", 200.0, 10.0, 8.0, 20.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"),
            DietRecord(3, 1, "菜C", 300.0, 15.0, 12.0, 30.0, "dinner", "2026-02-06", "2026-02-06T18:00:00")
        )
        val entities = EntityMapper.toDietRecordEntities(records)
        assertEquals(3, entities.size)
        assertEquals("菜A", entities[0].foodName)
        assertEquals("菜B", entities[1].foodName)
        assertEquals("菜C", entities[2].foodName)

        val restored = EntityMapper.toDietRecords(entities)
        assertEquals(3, restored.size)
        assertEquals(records[0].foodName, restored[0].foodName)
        assertEquals(records[1].calories, restored[1].calories, 0.01)
    }

    @Test
    fun `test ExerciseRecordResponseData to ExerciseRecordEntity mapping`() {
        val data = ExerciseRecordResponseData(
            id = 5,
            userId = 1,
            planId = 3,
            exerciseType = "running",
            actualCalories = 280.0,
            actualDuration = 35,
            distance = 4500.0,
            exerciseDate = "2026-02-06",
            startedAt = "2026-02-06T19:00:00",
            endedAt = "2026-02-06T19:35:00",
            notes = "公园跑步",
            createdAt = "2026-02-06T19:35:00"
        )
        val entity = EntityMapper.toExerciseRecordEntity(data)
        assertEquals(data.id, entity.serverId)
        assertEquals(data.userId, entity.userId)
        assertEquals(data.planId, entity.planId)
        assertEquals(data.exerciseType, entity.exerciseType)
        assertEquals(data.actualCalories, entity.actualCalories, 0.01)
        assertEquals(data.actualDuration, entity.actualDuration)
        assertEquals(data.distance, entity.distance)
        assertTrue(entity.isSynced)
    }

    @Test
    fun `test ExerciseRecordEntity to ExerciseRecordResponseData mapping`() {
        val entity = ExerciseRecordEntity(
            localId = 1,
            serverId = 10,
            userId = 1,
            planId = 5,
            exerciseType = "walking",
            actualCalories = 150.0,
            actualDuration = 30,
            distance = 3000.0,
            exerciseDate = "2026-02-06",
            createdAt = "2026-02-06T20:00:00",
            isSynced = true
        )
        val data = EntityMapper.toExerciseRecordResponseData(entity)
        assertEquals(10, data.id)
        assertEquals(entity.userId, data.userId)
        assertEquals(entity.exerciseType, data.exerciseType)
    }

    @Test
    fun `test TripPlan to TripPlanEntity mapping`() {
        val plan = TripPlan(
            tripId = 1,
            title = "餐后散步计划",
            destination = "附近公园",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            items = listOf(
                TripItem(1, "19:00", "公园入口", "walking", 15, 75.0, "热身散步"),
                TripItem(1, "19:15", "湖边小道", "running", 20, 200.0, "慢跑")
            )
        )
        val entity = EntityMapper.toTripPlanEntity(plan, userId = 1)
        assertEquals(plan.tripId, entity.tripId)
        assertEquals(1, entity.userId)
        assertEquals(plan.title, entity.title)
        assertEquals(plan.destination, entity.destination)
        assertTrue(entity.itemsJson.contains("公园入口"))
        assertTrue(entity.itemsJson.contains("湖边小道"))
    }

    @Test
    fun `test TripPlanEntity to TripPlan mapping`() {
        val itemsJson = """[{"dayIndex":1,"startTime":"19:00","placeName":"公园","placeType":"walking","duration":30,"cost":150.0,"notes":"散步"}]"""
        val entity = TripPlanEntity(
            tripId = 1,
            userId = 1,
            title = "散步计划",
            destination = "公园",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            itemsJson = itemsJson
        )
        val plan = EntityMapper.toTripPlan(entity)
        assertEquals(entity.tripId, plan.tripId)
        assertEquals(entity.title, plan.title)
        assertEquals(entity.destination, plan.destination)
        assertEquals(1, plan.items.size)
        assertEquals("公园", plan.items[0].placeName)
        assertEquals(30, plan.items[0].duration)
    }

    @Test
    fun `test TripPlanEntity to TripPlan mapping with empty items`() {
        val entity = TripPlanEntity(
            tripId = 1,
            userId = 1,
            title = "空计划",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            itemsJson = "[]"
        )
        val plan = EntityMapper.toTripPlan(entity)
        assertTrue(plan.items.isEmpty())
    }

    @Test
    fun `test TripPlanEntity to TripPlan mapping with invalid JSON`() {
        val entity = TripPlanEntity(
            tripId = 1,
            userId = 1,
            title = "损坏数据",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            itemsJson = "invalid json"
        )
        val plan = EntityMapper.toTripPlan(entity)
        // 无效JSON应该返回空列表，不崩溃
        assertTrue(plan.items.isEmpty())
    }

    @Test
    fun `test TripPlanEntity to TripSummary mapping`() {
        val itemsJson = """[{"dayIndex":1,"placeName":"A"},{"dayIndex":1,"placeName":"B"},{"dayIndex":1,"placeName":"C"}]"""
        val entity = TripPlanEntity(
            tripId = 1,
            userId = 1,
            title = "三站计划",
            destination = "公园",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            itemsJson = itemsJson
        )
        val summary = EntityMapper.toTripSummary(entity)
        assertEquals(entity.tripId, summary.tripId)
        assertEquals(entity.title, summary.title)
        assertEquals(3, summary.itemCount)
    }

    @Test
    fun `test TripPlan roundtrip mapping`() {
        val original = TripPlan(
            tripId = 99,
            title = "综合运动",
            destination = "体育场",
            startDate = "2026-02-06",
            endDate = "2026-02-06",
            items = listOf(
                TripItem(1, "08:00", "热身区", "walking", 10, 50.0, "热身"),
                TripItem(1, "08:10", "跑道", "running", 20, 200.0, "跑步"),
                TripItem(1, "08:30", "拉伸区", "walking", 10, 30.0, "拉伸放松")
            )
        )
        val entity = EntityMapper.toTripPlanEntity(original, userId = 1)
        val restored = EntityMapper.toTripPlan(entity)
        assertEquals(original.tripId, restored.tripId)
        assertEquals(original.title, restored.title)
        assertEquals(original.destination, restored.destination)
        assertEquals(original.items.size, restored.items.size)
        assertEquals(original.items[0].placeName, restored.items[0].placeName)
        assertEquals(original.items[1].placeName, restored.items[1].placeName)
        assertEquals(original.items[2].placeName, restored.items[2].placeName)
    }

    // ==================== 4. Fake DAO 实现（用于Repository测试） ====================

    /** Fake UserDao - 内存实现 */
    class FakeUserDao : UserDao {
        private val users = mutableMapOf<Int, UserEntity>()
        private val flows = mutableMapOf<Int, MutableStateFlow<UserEntity?>>()

        override suspend fun insertOrUpdate(user: UserEntity) {
            users[user.id] = user
            flows[user.id]?.value = user
        }

        override suspend fun getUserById(userId: Int): UserEntity? = users[userId]

        override fun observeUser(userId: Int): Flow<UserEntity?> {
            return flows.getOrPut(userId) { MutableStateFlow(users[userId]) }
        }

        override suspend fun deleteUser(userId: Int) {
            users.remove(userId)
            flows[userId]?.value = null
        }

        override suspend fun deleteAll() {
            users.clear()
            flows.values.forEach { it.value = null }
        }

        override suspend fun getUserCount(): Int = users.size
    }

    /** Fake DietRecordDao - 内存实现 */
    class FakeDietRecordDao : DietRecordDao {
        private val records = mutableListOf<DietRecordEntity>()
        private var nextId = 1L
        private val flowState = MutableStateFlow<List<DietRecordEntity>>(emptyList())

        private fun emitUpdate() {
            flowState.value = records.toList()
        }

        override suspend fun insert(record: DietRecordEntity): Long {
            val newRecord = if (record.localId == 0L) {
                record.copy(localId = nextId++)
            } else {
                records.removeAll { it.localId == record.localId }
                record
            }
            records.add(newRecord)
            emitUpdate()
            return newRecord.localId
        }

        override suspend fun insertAll(recordsList: List<DietRecordEntity>) {
            recordsList.forEach { insert(it) }
        }

        override suspend fun update(record: DietRecordEntity) {
            val index = records.indexOfFirst { it.localId == record.localId }
            if (index >= 0) {
                records[index] = record
                emitUpdate()
            }
        }

        override suspend fun getRecordsByUserId(userId: Int): List<DietRecordEntity> {
            return records.filter { it.userId == userId }
                .sortedWith(compareByDescending<DietRecordEntity> { it.recordDate }
                    .thenByDescending { it.createdAt })
        }

        override fun observeRecordsByUserId(userId: Int): Flow<List<DietRecordEntity>> {
            return flowState.map { all -> all.filter { it.userId == userId } }
        }

        override suspend fun getRecordsByDate(userId: Int, date: String): List<DietRecordEntity> {
            return records.filter { it.userId == userId && it.recordDate == date }
        }

        override fun observeRecordsByDate(userId: Int, date: String): Flow<List<DietRecordEntity>> {
            return flowState.map { all -> all.filter { it.userId == userId && it.recordDate == date } }
        }

        override suspend fun getRecordByLocalId(localId: Long): DietRecordEntity? {
            return records.find { it.localId == localId }
        }

        override suspend fun getRecordByServerId(serverId: Int): DietRecordEntity? {
            return records.find { it.serverId == serverId }
        }

        override suspend fun deleteByLocalId(localId: Long) {
            records.removeAll { it.localId == localId }
            emitUpdate()
        }

        override suspend fun deleteByServerId(serverId: Int) {
            records.removeAll { it.serverId == serverId }
            emitUpdate()
        }

        override suspend fun getUnsyncedRecords(userId: Int): List<DietRecordEntity> {
            return records.filter { it.userId == userId && !it.isSynced }
        }

        override suspend fun deleteAllByUserId(userId: Int) {
            records.removeAll { it.userId == userId }
            emitUpdate()
        }

        override suspend fun getRecordCount(userId: Int): Int {
            return records.count { it.userId == userId }
        }

        override suspend fun getTotalCaloriesByDate(userId: Int, date: String): Double? {
            val total = records.filter { it.userId == userId && it.recordDate == date }
                .sumOf { it.calories }
            return if (total == 0.0 && records.none { it.userId == userId && it.recordDate == date }) null else total
        }
    }

    /** Fake ExerciseRecordDao - 内存实现 */
    class FakeExerciseRecordDao : ExerciseRecordDao {
        private val records = mutableListOf<ExerciseRecordEntity>()
        private var nextId = 1L
        private val flowState = MutableStateFlow<List<ExerciseRecordEntity>>(emptyList())

        private fun emitUpdate() { flowState.value = records.toList() }

        override suspend fun insert(record: ExerciseRecordEntity): Long {
            val newRecord = if (record.localId == 0L) record.copy(localId = nextId++) else record
            records.removeAll { it.localId == newRecord.localId }
            records.add(newRecord)
            emitUpdate()
            return newRecord.localId
        }

        override suspend fun insertAll(recordsList: List<ExerciseRecordEntity>) {
            recordsList.forEach { insert(it) }
        }

        override suspend fun update(record: ExerciseRecordEntity) {
            val index = records.indexOfFirst { it.localId == record.localId }
            if (index >= 0) { records[index] = record; emitUpdate() }
        }

        override suspend fun getRecordsByUserId(userId: Int): List<ExerciseRecordEntity> {
            return records.filter { it.userId == userId }
        }

        override fun observeRecordsByUserId(userId: Int): Flow<List<ExerciseRecordEntity>> {
            return flowState.map { all -> all.filter { it.userId == userId } }
        }

        override suspend fun getRecordsByDate(userId: Int, date: String): List<ExerciseRecordEntity> {
            return records.filter { it.userId == userId && it.exerciseDate == date }
        }

        override suspend fun getRecordByLocalId(localId: Long): ExerciseRecordEntity? {
            return records.find { it.localId == localId }
        }

        override suspend fun getRecordByServerId(serverId: Int): ExerciseRecordEntity? {
            return records.find { it.serverId == serverId }
        }

        override suspend fun deleteByLocalId(localId: Long) {
            records.removeAll { it.localId == localId }; emitUpdate()
        }

        override suspend fun deleteByServerId(serverId: Int) {
            records.removeAll { it.serverId == serverId }; emitUpdate()
        }

        override suspend fun getUnsyncedRecords(userId: Int): List<ExerciseRecordEntity> {
            return records.filter { it.userId == userId && !it.isSynced }
        }

        override suspend fun deleteAllByUserId(userId: Int) {
            records.removeAll { it.userId == userId }; emitUpdate()
        }

        override suspend fun getRecordCount(userId: Int): Int {
            return records.count { it.userId == userId }
        }

        override suspend fun getTotalCaloriesByDate(userId: Int, date: String): Double? {
            val matching = records.filter { it.userId == userId && it.exerciseDate == date }
            return if (matching.isEmpty()) null else matching.sumOf { it.actualCalories }
        }
    }

    /** Fake TripPlanDao - 内存实现 */
    class FakeTripPlanDao : TripPlanDao {
        private val plans = mutableMapOf<Int, TripPlanEntity>()
        private val flowState = MutableStateFlow<List<TripPlanEntity>>(emptyList())

        private fun emitUpdate() { flowState.value = plans.values.toList() }

        override suspend fun insertOrUpdate(plan: TripPlanEntity) {
            plans[plan.tripId] = plan; emitUpdate()
        }

        override suspend fun insertAll(plansList: List<TripPlanEntity>) {
            plansList.forEach { plans[it.tripId] = it }; emitUpdate()
        }

        override suspend fun update(plan: TripPlanEntity) {
            if (plans.containsKey(plan.tripId)) { plans[plan.tripId] = plan; emitUpdate() }
        }

        override suspend fun getPlansByUserId(userId: Int): List<TripPlanEntity> {
            return plans.values.filter { it.userId == userId }.sortedByDescending { it.startDate }
        }

        override fun observePlansByUserId(userId: Int): Flow<List<TripPlanEntity>> {
            return flowState.map { all -> all.filter { it.userId == userId } }
        }

        override suspend fun getPlanById(tripId: Int): TripPlanEntity? = plans[tripId]

        override fun observePlanById(tripId: Int): Flow<TripPlanEntity?> {
            return flowState.map { all -> all.find { it.tripId == tripId } }
        }

        override suspend fun getRecentPlans(userId: Int, limit: Int): List<TripPlanEntity> {
            return plans.values.filter { it.userId == userId }
                .sortedByDescending { it.startDate }.take(limit)
        }

        override suspend fun deleteById(tripId: Int) {
            plans.remove(tripId); emitUpdate()
        }

        override suspend fun deleteAllByUserId(userId: Int) {
            plans.entries.removeAll { it.value.userId == userId }; emitUpdate()
        }

        override suspend fun getPlanCount(userId: Int): Int {
            return plans.values.count { it.userId == userId }
        }
    }

    // ==================== 5. UserRepository 测试 ====================

    @Test
    fun `test UserRepository save and get preferences`() = runTest {
        val dao = FakeUserDao()
        val repo = UserRepository(dao)

        val data = UserPreferencesData(
            userId = 1, nickname = "测试", healthGoal = "reduce_fat",
            allergens = listOf("花生"), travelPreference = "walking",
            dailyBudget = 500, weight = 70.0, height = 175.0, age = 25, gender = "male"
        )
        repo.saveUserPreferences(data)

        val result = repo.getUserPreferences(1)
        assertNotNull(result)
        assertEquals("测试", result!!.nickname)
        assertEquals("reduce_fat", result.healthGoal)
        assertEquals(listOf("花生"), result.allergens)
        assertEquals(70.0, result.weight!!, 0.01)
    }

    @Test
    fun `test UserRepository get nonexistent user returns null`() = runTest {
        val dao = FakeUserDao()
        val repo = UserRepository(dao)
        assertNull(repo.getUserPreferences(999))
    }

    @Test
    fun `test UserRepository update preferences`() = runTest {
        val dao = FakeUserDao()
        val repo = UserRepository(dao)

        // 初始保存
        repo.saveUserPreferences(
            UserPreferencesData(1, "用户A", "reduce_fat", listOf("花生"), null, null)
        )
        // 更新
        repo.saveUserPreferences(
            UserPreferencesData(1, "用户A", "gain_muscle", listOf("牛奶", "鸡蛋"), "walking", 300)
        )

        val result = repo.getUserPreferences(1)
        assertEquals("gain_muscle", result!!.healthGoal)
        assertEquals(listOf("牛奶", "鸡蛋"), result.allergens)
        assertEquals(1, repo.getUserCount())
    }

    @Test
    fun `test UserRepository delete user`() = runTest {
        val dao = FakeUserDao()
        val repo = UserRepository(dao)

        repo.saveUserPreferences(UserPreferencesData(1, "用户A", null, null, null, null))
        repo.saveUserPreferences(UserPreferencesData(2, "用户B", null, null, null, null))
        assertEquals(2, repo.getUserCount())

        repo.deleteUser(1)
        assertEquals(1, repo.getUserCount())
        assertNull(repo.getUserPreferences(1))
        assertNotNull(repo.getUserPreferences(2))
    }

    @Test
    fun `test UserRepository deleteAll`() = runTest {
        val dao = FakeUserDao()
        val repo = UserRepository(dao)

        repo.saveUserPreferences(UserPreferencesData(1, "A", null, null, null, null))
        repo.saveUserPreferences(UserPreferencesData(2, "B", null, null, null, null))
        repo.deleteAll()
        assertEquals(0, repo.getUserCount())
    }

    @Test
    fun `test UserRepository observe preferences via Flow`() = runTest {
        val dao = FakeUserDao()
        val repo = UserRepository(dao)

        repo.saveUserPreferences(UserPreferencesData(1, "测试", "reduce_fat", null, null, null))
        val observed = repo.observeUserPreferences(1).first()
        assertNotNull(observed)
        assertEquals("测试", observed!!.nickname)
    }

    // ==================== 6. DietRepository 测试 ====================

    @Test
    fun `test DietRepository save from server and get`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        val record = DietRecord(1, 1, "番茄炒蛋", 150.0, 10.5, 8.2, 6.3, "lunch", "2026-02-06", "2026-02-06T12:00:00")
        repo.saveFromServer(record)

        val results = repo.getRecordsByUserId(1)
        assertEquals(1, results.size)
        assertEquals("番茄炒蛋", results[0].foodName)
        assertEquals(150.0, results[0].calories, 0.01)
    }

    @Test
    fun `test DietRepository add local record`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        val localId = repo.addLocalRecord(
            userId = 1, foodName = "白粥", calories = 50.0,
            protein = 1.0, fat = 0.2, carbs = 10.0,
            mealType = "breakfast", recordDate = "2026-02-06",
            createdAt = "2026-02-06T08:00:00"
        )
        assertTrue(localId > 0)

        val unsyncedRecords = repo.getUnsyncedRecords(1)
        assertEquals(1, unsyncedRecords.size)
        assertEquals("白粥", unsyncedRecords[0].foodName)
        assertFalse(unsyncedRecords[0].isSynced)
    }

    @Test
    fun `test DietRepository batch save from server`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        val records = listOf(
            DietRecord(1, 1, "菜A", 100.0, 5.0, 3.0, 10.0, "breakfast", "2026-02-06", "2026-02-06T08:00:00"),
            DietRecord(2, 1, "菜B", 200.0, 10.0, 8.0, 20.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"),
            DietRecord(3, 1, "菜C", 300.0, 15.0, 12.0, 30.0, "dinner", "2026-02-06", "2026-02-06T18:00:00")
        )
        repo.saveAllFromServer(records)

        assertEquals(3, repo.getRecordCount(1))
        val results = repo.getRecordsByUserId(1)
        assertEquals(3, results.size)
    }

    @Test
    fun `test DietRepository get records by date`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        repo.saveAllFromServer(listOf(
            DietRecord(1, 1, "菜A", 100.0, 5.0, 3.0, 10.0, "breakfast", "2026-02-05", "2026-02-05T08:00:00"),
            DietRecord(2, 1, "菜B", 200.0, 10.0, 8.0, 20.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"),
            DietRecord(3, 1, "菜C", 300.0, 15.0, 12.0, 30.0, "dinner", "2026-02-06", "2026-02-06T18:00:00")
        ))

        val feb6 = repo.getRecordsByDate(1, "2026-02-06")
        assertEquals(2, feb6.size)

        val feb5 = repo.getRecordsByDate(1, "2026-02-05")
        assertEquals(1, feb5.size)
    }

    @Test
    fun `test DietRepository delete by localId`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        val localId = repo.addLocalRecord(
            userId = 1, foodName = "测试", calories = 100.0,
            mealType = "snack", recordDate = "2026-02-06",
            createdAt = "2026-02-06T15:00:00"
        )
        assertEquals(1, repo.getRecordCount(1))
        repo.deleteByLocalId(localId)
        assertEquals(0, repo.getRecordCount(1))
    }

    @Test
    fun `test DietRepository mark as synced`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        val localId = repo.addLocalRecord(
            userId = 1, foodName = "离线记录", calories = 100.0,
            mealType = "snack", recordDate = "2026-02-06",
            createdAt = "2026-02-06T15:00:00"
        )
        assertEquals(1, repo.getUnsyncedRecords(1).size)

        repo.markAsSynced(localId, serverId = 42)
        assertEquals(0, repo.getUnsyncedRecords(1).size)
    }

    @Test
    fun `test DietRepository total calories by date`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        repo.saveAllFromServer(listOf(
            DietRecord(1, 1, "早餐", 200.0, 0.0, 0.0, 0.0, "breakfast", "2026-02-06", "2026-02-06T08:00:00"),
            DietRecord(2, 1, "午餐", 500.0, 0.0, 0.0, 0.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"),
            DietRecord(3, 1, "晚餐", 400.0, 0.0, 0.0, 0.0, "dinner", "2026-02-06", "2026-02-06T18:00:00")
        ))

        val total = repo.getTotalCaloriesByDate(1, "2026-02-06")
        assertEquals(1100.0, total, 0.01)
    }

    @Test
    fun `test DietRepository isolation between users`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        repo.saveFromServer(DietRecord(1, 1, "用户1的菜", 100.0, 0.0, 0.0, 0.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"))
        repo.saveFromServer(DietRecord(2, 2, "用户2的菜", 200.0, 0.0, 0.0, 0.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"))

        assertEquals(1, repo.getRecordCount(1))
        assertEquals(1, repo.getRecordCount(2))

        repo.deleteAllByUserId(1)
        assertEquals(0, repo.getRecordCount(1))
        assertEquals(1, repo.getRecordCount(2))
    }

    @Test
    fun `test DietRepository observe records flow`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        repo.saveFromServer(DietRecord(1, 1, "菜A", 100.0, 0.0, 0.0, 0.0, "lunch", "2026-02-06", "2026-02-06T12:00:00"))
        val records = repo.observeRecordsByUserId(1).first()
        assertEquals(1, records.size)
    }

    // ==================== 7. ExerciseRepository 测试 ====================

    @Test
    fun `test ExerciseRepository save and get`() = runTest {
        val dao = FakeExerciseRecordDao()
        val repo = ExerciseRepository(dao)

        val data = ExerciseRecordResponseData(
            id = 1, userId = 1, planId = 5, exerciseType = "running",
            actualCalories = 280.0, actualDuration = 35, distance = 4500.0,
            exerciseDate = "2026-02-06", createdAt = "2026-02-06T19:35:00"
        )
        repo.saveFromServer(data)

        val results = repo.getRecordsByUserId(1)
        assertEquals(1, results.size)
        assertEquals("running", results[0].exerciseType)
        assertEquals(280.0, results[0].actualCalories, 0.01)
    }

    @Test
    fun `test ExerciseRepository add local record`() = runTest {
        val dao = FakeExerciseRecordDao()
        val repo = ExerciseRepository(dao)

        val localId = repo.addLocalRecord(
            userId = 1, exerciseType = "walking",
            actualCalories = 150.0, actualDuration = 30,
            distance = 3000.0, exerciseDate = "2026-02-06",
            createdAt = "2026-02-06T20:00:00"
        )
        assertTrue(localId > 0)
        assertEquals(1, repo.getUnsyncedRecords(1).size)
    }

    @Test
    fun `test ExerciseRepository mark as synced`() = runTest {
        val dao = FakeExerciseRecordDao()
        val repo = ExerciseRepository(dao)

        val localId = repo.addLocalRecord(
            userId = 1, actualCalories = 100.0, actualDuration = 15,
            exerciseDate = "2026-02-06", createdAt = "2026-02-06T20:00:00"
        )
        assertEquals(1, repo.getUnsyncedRecords(1).size)
        repo.markAsSynced(localId, serverId = 10)
        assertEquals(0, repo.getUnsyncedRecords(1).size)
    }

    @Test
    fun `test ExerciseRepository total calories by date`() = runTest {
        val dao = FakeExerciseRecordDao()
        val repo = ExerciseRepository(dao)

        repo.addLocalRecord(userId = 1, actualCalories = 200.0, actualDuration = 20,
            exerciseDate = "2026-02-06", createdAt = "2026-02-06T19:00:00")
        repo.addLocalRecord(userId = 1, actualCalories = 150.0, actualDuration = 30,
            exerciseDate = "2026-02-06", createdAt = "2026-02-06T20:00:00")

        val total = repo.getTotalCaloriesByDate(1, "2026-02-06")
        assertEquals(350.0, total, 0.01)
    }

    @Test
    fun `test ExerciseRepository delete`() = runTest {
        val dao = FakeExerciseRecordDao()
        val repo = ExerciseRepository(dao)

        val localId = repo.addLocalRecord(
            userId = 1, actualCalories = 100.0, actualDuration = 15,
            exerciseDate = "2026-02-06", createdAt = "2026-02-06T20:00:00"
        )
        assertEquals(1, repo.getRecordCount(1))
        repo.deleteByLocalId(localId)
        assertEquals(0, repo.getRecordCount(1))
    }

    // ==================== 8. TripRepository 测试 ====================

    @Test
    fun `test TripRepository save and get plan`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        val plan = TripPlan(
            tripId = 1, title = "散步计划", destination = "公园",
            startDate = "2026-02-06", endDate = "2026-02-06",
            items = listOf(TripItem(1, "19:00", "公园", "walking", 30, 150.0, "散步"))
        )
        repo.savePlan(plan, userId = 1)

        val result = repo.getPlanById(1)
        assertNotNull(result)
        assertEquals("散步计划", result!!.title)
        assertEquals(1, result.items.size)
        assertEquals("公园", result.items[0].placeName)
    }

    @Test
    fun `test TripRepository get nonexistent plan returns null`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)
        assertNull(repo.getPlanById(999))
    }

    @Test
    fun `test TripRepository batch save plans`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        val plans = listOf(
            TripPlan(1, "计划A", "公园A", "2026-02-04", "2026-02-04"),
            TripPlan(2, "计划B", "公园B", "2026-02-05", "2026-02-05"),
            TripPlan(3, "计划C", "公园C", "2026-02-06", "2026-02-06")
        )
        repo.saveAllPlans(plans, userId = 1)

        assertEquals(3, repo.getPlanCount(1))
    }

    @Test
    fun `test TripRepository get plan summaries`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        val items = listOf(TripItem(1, "19:00", "A", "walking", 15, 75.0, null))
        repo.savePlan(TripPlan(1, "计划1", "公园", "2026-02-06", "2026-02-06", items), 1)
        repo.savePlan(TripPlan(2, "计划2", "广场", "2026-02-05", "2026-02-05"), 1)

        val summaries = repo.getPlanSummaries(1)
        assertEquals(2, summaries.size)
        // 按日期倒序
        assertEquals("计划1", summaries[0].title)
        assertEquals(1, summaries[0].itemCount)
        assertEquals("计划2", summaries[1].title)
        assertEquals(0, summaries[1].itemCount)
    }

    @Test
    fun `test TripRepository get recent plans`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        for (i in 1..10) {
            repo.savePlan(
                TripPlan(i, "计划$i", null, "2026-02-%02d".format(i), "2026-02-%02d".format(i)),
                userId = 1
            )
        }

        val recent = repo.getRecentPlans(1, limit = 3)
        assertEquals(3, recent.size)
        // 最新的排在前面
        assertEquals("计划10", recent[0].title)
    }

    @Test
    fun `test TripRepository delete plan`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        repo.savePlan(TripPlan(1, "计划1", null, "2026-02-06", "2026-02-06"), 1)
        repo.savePlan(TripPlan(2, "计划2", null, "2026-02-06", "2026-02-06"), 1)
        assertEquals(2, repo.getPlanCount(1))

        repo.deletePlan(1)
        assertEquals(1, repo.getPlanCount(1))
        assertNull(repo.getPlanById(1))
        assertNotNull(repo.getPlanById(2))
    }

    @Test
    fun `test TripRepository isolation between users`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        repo.savePlan(TripPlan(1, "用户1计划", null, "2026-02-06", "2026-02-06"), 1)
        repo.savePlan(TripPlan(2, "用户2计划", null, "2026-02-06", "2026-02-06"), 2)

        assertEquals(1, repo.getPlanCount(1))
        assertEquals(1, repo.getPlanCount(2))

        repo.deleteAllByUserId(1)
        assertEquals(0, repo.getPlanCount(1))
        assertEquals(1, repo.getPlanCount(2))
    }

    @Test
    fun `test TripRepository observe plan by id`() = runTest {
        val dao = FakeTripPlanDao()
        val repo = TripRepository(dao)

        repo.savePlan(
            TripPlan(1, "观察测试", "公园", "2026-02-06", "2026-02-06"),
            userId = 1
        )
        val observed = repo.observePlanById(1).first()
        assertNotNull(observed)
        assertEquals("观察测试", observed!!.title)
    }

    // ==================== 9. 边界条件与异常场景测试 ====================

    @Test
    fun `test empty database returns empty results`() = runTest {
        val dietDao = FakeDietRecordDao()
        val dietRepo = DietRepository(dietDao)
        assertTrue(dietRepo.getRecordsByUserId(1).isEmpty())
        assertEquals(0, dietRepo.getRecordCount(1))
        assertEquals(0.0, dietRepo.getTotalCaloriesByDate(1, "2026-02-06"), 0.01)

        val exerciseDao = FakeExerciseRecordDao()
        val exerciseRepo = ExerciseRepository(exerciseDao)
        assertTrue(exerciseRepo.getRecordsByUserId(1).isEmpty())
        assertEquals(0, exerciseRepo.getRecordCount(1))

        val tripDao = FakeTripPlanDao()
        val tripRepo = TripRepository(tripDao)
        assertTrue(tripRepo.getPlanSummaries(1).isEmpty())
        assertEquals(0, tripRepo.getPlanCount(1))
    }

    @Test
    fun `test DietRecord with zero calories`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        repo.addLocalRecord(
            userId = 1, foodName = "水", calories = 0.0,
            mealType = "snack", recordDate = "2026-02-06",
            createdAt = "2026-02-06T10:00:00"
        )
        val records = repo.getRecordsByUserId(1)
        assertEquals(1, records.size)
        assertEquals(0.0, records[0].calories, 0.01)
    }

    @Test
    fun `test DietRecord with large calorie value`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        repo.addLocalRecord(
            userId = 1, foodName = "火锅大餐", calories = 9999.99,
            mealType = "dinner", recordDate = "2026-02-06",
            createdAt = "2026-02-06T18:00:00"
        )
        val total = repo.getTotalCaloriesByDate(1, "2026-02-06")
        assertEquals(9999.99, total, 0.01)
    }

    @Test
    fun `test multiple unsynced records tracking`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        // 添加3条离线记录
        repeat(3) { i ->
            repo.addLocalRecord(
                userId = 1, foodName = "离线菜品$i", calories = 100.0 * (i + 1),
                mealType = "lunch", recordDate = "2026-02-06",
                createdAt = "2026-02-06T12:0${i}:00"
            )
        }
        // 添加1条已同步记录
        repo.saveFromServer(DietRecord(99, 1, "已同步菜品", 500.0, 0.0, 0.0, 0.0, "dinner", "2026-02-06", "2026-02-06T18:00:00"))

        val unsynced = repo.getUnsyncedRecords(1)
        assertEquals(3, unsynced.size)
        assertEquals(4, repo.getRecordCount(1))
    }

    @Test
    fun `test mark nonexistent record as synced is safe`() = runTest {
        val dao = FakeDietRecordDao()
        val repo = DietRepository(dao)

        // 标记不存在的localId，应该不报错
        repo.markAsSynced(9999L, serverId = 1)
        assertEquals(0, repo.getRecordCount(1))
    }

    @Test
    fun `test entity copy for update pattern`() {
        val entity = DietRecordEntity(
            localId = 1, serverId = null, userId = 1,
            foodName = "原始菜名", calories = 100.0,
            mealType = "lunch", recordDate = "2026-02-06",
            createdAt = "2026-02-06T12:00:00", isSynced = false
        )
        val updated = entity.copy(serverId = 42, isSynced = true)
        assertEquals(1L, updated.localId)
        assertEquals(42, updated.serverId)
        assertTrue(updated.isSynced)
        assertEquals("原始菜名", updated.foodName)
    }

    @Test
    fun `test UserEntity with null allergens`() {
        val entity = UserEntity(id = 1, allergens = null)
        assertNull(entity.allergens)

        val data = EntityMapper.toUserPreferencesData(entity)
        assertNull(data.allergens)
    }

    @Test
    fun `test UserEntity with empty allergens list`() {
        val entity = UserEntity(id = 1, allergens = emptyList())
        assertTrue(entity.allergens!!.isEmpty())

        val data = EntityMapper.toUserPreferencesData(entity)
        assertNotNull(data.allergens)
        assertTrue(data.allergens!!.isEmpty())
    }
}
