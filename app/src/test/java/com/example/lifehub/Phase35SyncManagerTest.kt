package com.example.lifehub

import com.example.lifehub.data.DietRecord
import com.example.lifehub.data.local.entity.DietRecordEntity
import com.example.lifehub.data.local.entity.ExerciseRecordEntity
import com.example.lifehub.data.sync.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 35: 端云数据同步 - 单元测试
 * 测试同步状态模型、冲突解决器、同步结果统计等核心逻辑
 * 不依赖Android框架，纯业务逻辑测试
 */
class Phase35SyncManagerTest {

    // ==================== SyncResult 测试 ====================

    @Test
    fun `SyncResult totalSynced counts all synced items`() {
        val result = SyncResult(
            uploadedDietRecords = 3,
            uploadedExerciseRecords = 2,
            downloadedDietRecords = 5,
            downloadedExerciseRecords = 1,
            downloadedTripPlans = 4,
            userPreferencesSynced = true
        )
        // 3 + 2 + 5 + 1 + 4 + 1(prefs) = 16
        assertEquals(16, result.totalSynced)
    }

    @Test
    fun `SyncResult totalSynced without prefs sync`() {
        val result = SyncResult(
            uploadedDietRecords = 1,
            uploadedExerciseRecords = 0,
            downloadedDietRecords = 2,
            userPreferencesSynced = false
        )
        assertEquals(3, result.totalSynced)
    }

    @Test
    fun `SyncResult hasErrors returns true when errors exist`() {
        val result = SyncResult(errors = listOf("网络超时"))
        assertTrue(result.hasErrors)
        assertFalse(result.isFullSuccess)
    }

    @Test
    fun `SyncResult hasErrors returns false when no errors`() {
        val result = SyncResult(uploadedDietRecords = 1)
        assertFalse(result.hasErrors)
        assertTrue(result.isFullSuccess)
    }

    @Test
    fun `SyncResult default values are all zero`() {
        val result = SyncResult()
        assertEquals(0, result.uploadedDietRecords)
        assertEquals(0, result.uploadedExerciseRecords)
        assertEquals(0, result.downloadedDietRecords)
        assertEquals(0, result.downloadedExerciseRecords)
        assertEquals(0, result.downloadedTripPlans)
        assertFalse(result.userPreferencesSynced)
        assertTrue(result.errors.isEmpty())
        assertEquals(0, result.totalSynced)
        assertTrue(result.isFullSuccess)
    }

    @Test
    fun `SyncResult with multiple errors`() {
        val result = SyncResult(
            uploadedDietRecords = 1,
            errors = listOf("错误1", "错误2", "错误3")
        )
        assertTrue(result.hasErrors)
        assertEquals(3, result.errors.size)
        assertFalse(result.isFullSuccess)
        assertEquals(1, result.totalSynced)
    }

    @Test
    fun `SyncResult copy preserves immutability`() {
        val original = SyncResult(uploadedDietRecords = 5)
        val modified = original.copy(downloadedDietRecords = 3)
        assertEquals(5, original.uploadedDietRecords)
        assertEquals(0, original.downloadedDietRecords)
        assertEquals(5, modified.uploadedDietRecords)
        assertEquals(3, modified.downloadedDietRecords)
    }

    // ==================== SyncStatus 测试 ====================

    @Test
    fun `SyncStatus Idle is singleton`() {
        val s1 = SyncStatus.Idle
        val s2 = SyncStatus.Idle
        assertSame(s1, s2)
    }

    @Test
    fun `SyncStatus Syncing is singleton`() {
        val s1 = SyncStatus.Syncing
        val s2 = SyncStatus.Syncing
        assertSame(s1, s2)
    }

    @Test
    fun `SyncStatus Success stores count and timestamp`() {
        val now = System.currentTimeMillis()
        val status = SyncStatus.Success(syncedCount = 10, timestamp = now)
        assertEquals(10, status.syncedCount)
        assertEquals(now, status.timestamp)
    }

    @Test
    fun `SyncStatus Error stores message and timestamp`() {
        val now = System.currentTimeMillis()
        val status = SyncStatus.Error(message = "网络超时", timestamp = now)
        assertEquals("网络超时", status.message)
        assertEquals(now, status.timestamp)
    }

    @Test
    fun `SyncStatus types are distinguishable via when expression`() {
        val statuses: List<SyncStatus> = listOf(
            SyncStatus.Idle,
            SyncStatus.Syncing,
            SyncStatus.Success(5),
            SyncStatus.Error("fail")
        )
        val labels = statuses.map { status ->
            when (status) {
                is SyncStatus.Idle -> "idle"
                is SyncStatus.Syncing -> "syncing"
                is SyncStatus.Success -> "success:${status.syncedCount}"
                is SyncStatus.Error -> "error:${status.message}"
            }
        }
        assertEquals(listOf("idle", "syncing", "success:5", "error:fail"), labels)
    }

    // ==================== SyncConfig 测试 ====================

    @Test
    fun `SyncConfig default values`() {
        val config = SyncConfig()
        assertEquals(15 * 60 * 1000L, config.syncIntervalMs)
        assertEquals(30 * 1000L, config.retryDelayMs)
        assertEquals(3, config.maxRetryCount)
        assertTrue(config.enableAutoSync)
    }

    @Test
    fun `SyncConfig custom values`() {
        val config = SyncConfig(
            syncIntervalMs = 5 * 60 * 1000L,
            retryDelayMs = 10 * 1000L,
            maxRetryCount = 5,
            enableAutoSync = false
        )
        assertEquals(5 * 60 * 1000L, config.syncIntervalMs)
        assertEquals(10 * 1000L, config.retryDelayMs)
        assertEquals(5, config.maxRetryCount)
        assertFalse(config.enableAutoSync)
    }

    // ==================== NetworkState 测试 ====================

    @Test
    fun `NetworkState enum values`() {
        val states = NetworkState.values()
        assertEquals(3, states.size)
        assertTrue(states.contains(NetworkState.AVAILABLE))
        assertTrue(states.contains(NetworkState.UNAVAILABLE))
        assertTrue(states.contains(NetworkState.UNKNOWN))
    }

    @Test
    fun `NetworkState valueOf works`() {
        assertEquals(NetworkState.AVAILABLE, NetworkState.valueOf("AVAILABLE"))
        assertEquals(NetworkState.UNAVAILABLE, NetworkState.valueOf("UNAVAILABLE"))
        assertEquals(NetworkState.UNKNOWN, NetworkState.valueOf("UNKNOWN"))
    }

    // ==================== ConflictResolver 测试 ====================

    private fun createLocalDietEntity(
        localId: Long = 1,
        serverId: Int? = 100,
        userId: Int = 1,
        foodName: String = "番茄炒蛋",
        calories: Double = 150.0,
        protein: Double = 10.0,
        fat: Double = 8.0,
        carbs: Double = 6.0,
        mealType: String = "lunch",
        recordDate: String = "2026-02-06",
        createdAt: String = "2026-02-06T12:00:00",
        isSynced: Boolean = true
    ) = DietRecordEntity(
        localId = localId,
        serverId = serverId,
        userId = userId,
        foodName = foodName,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        mealType = mealType,
        recordDate = recordDate,
        createdAt = createdAt,
        isSynced = isSynced
    )

    private fun createServerDietRecord(
        id: Int = 100,
        userId: Int = 1,
        foodName: String = "番茄炒蛋（更新）",
        calories: Double = 160.0,
        protein: Double = 11.0,
        fat: Double = 9.0,
        carbs: Double = 7.0,
        mealType: String = "lunch",
        recordDate: String = "2026-02-06",
        createdAt: String = "2026-02-06T12:00:00"
    ) = DietRecord(
        id = id,
        userId = userId,
        foodName = foodName,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        mealType = mealType,
        recordDate = recordDate,
        createdAt = createdAt
    )

    @Test
    fun `ConflictResolver resolveDietRecord server wins for synced record`() {
        val local = createLocalDietEntity(isSynced = true, serverId = 100)
        val server = createServerDietRecord(id = 100, foodName = "番茄蛋汤", calories = 120.0)

        val resolved = ConflictResolver.resolveDietRecord(local, server)

        assertEquals("番茄蛋汤", resolved.foodName)
        assertEquals(120.0, resolved.calories, 0.01)
        assertEquals(100, resolved.serverId)
        assertTrue(resolved.isSynced)
    }

    @Test
    fun `ConflictResolver resolveDietRecord preserves unsynced local record`() {
        val local = createLocalDietEntity(isSynced = false, serverId = null, foodName = "离线新增菜品")
        val server = createServerDietRecord(id = 200, foodName = "服务端菜品")

        val resolved = ConflictResolver.resolveDietRecord(local, server)

        // 离线新增记录保留本地版本
        assertEquals("离线新增菜品", resolved.foodName)
        assertNull(resolved.serverId)
        assertFalse(resolved.isSynced)
    }

    @Test
    fun `ConflictResolver shouldUpdateLocalDietRecord detects field changes`() {
        val local = createLocalDietEntity(
            isSynced = true, serverId = 100,
            foodName = "番茄炒蛋", calories = 150.0
        )

        // 名称变化
        val serverNameChanged = createServerDietRecord(
            id = 100, foodName = "西红柿炒鸡蛋", calories = 150.0
        )
        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, serverNameChanged))

        // 热量变化
        val serverCalChanged = createServerDietRecord(
            id = 100, foodName = "番茄炒蛋", calories = 200.0
        )
        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, serverCalChanged))
    }

    @Test
    fun `ConflictResolver shouldUpdateLocalDietRecord no change returns false`() {
        val local = createLocalDietEntity(
            isSynced = true, serverId = 100,
            foodName = "番茄炒蛋", calories = 150.0, protein = 10.0,
            fat = 8.0, carbs = 6.0, mealType = "lunch", recordDate = "2026-02-06"
        )
        val server = createServerDietRecord(
            id = 100, foodName = "番茄炒蛋", calories = 150.0, protein = 10.0,
            fat = 8.0, carbs = 6.0, mealType = "lunch", recordDate = "2026-02-06"
        )
        assertFalse(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
    }

    @Test
    fun `ConflictResolver shouldUpdateLocalDietRecord skip unsynced local`() {
        val local = createLocalDietEntity(isSynced = false, serverId = null)
        val server = createServerDietRecord(id = 200, foodName = "完全不同")

        // 未同步的本地记录不应被覆盖
        assertFalse(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
    }

    @Test
    fun `ConflictResolver findMatchingLocalRecord finds by serverId`() {
        val locals = listOf(
            createLocalDietEntity(localId = 1, serverId = 100),
            createLocalDietEntity(localId = 2, serverId = 200),
            createLocalDietEntity(localId = 3, serverId = 300)
        )
        val server = createServerDietRecord(id = 200)

        val match = ConflictResolver.findMatchingLocalRecord(locals, server)

        assertNotNull(match)
        assertEquals(2L, match!!.localId)
        assertEquals(200, match.serverId)
    }

    @Test
    fun `ConflictResolver findMatchingLocalRecord returns null when not found`() {
        val locals = listOf(
            createLocalDietEntity(localId = 1, serverId = 100),
            createLocalDietEntity(localId = 2, serverId = 200)
        )
        val server = createServerDietRecord(id = 999)

        val match = ConflictResolver.findMatchingLocalRecord(locals, server)
        assertNull(match)
    }

    @Test
    fun `ConflictResolver findMatchingLocalRecord handles empty list`() {
        val server = createServerDietRecord(id = 100)
        val match = ConflictResolver.findMatchingLocalRecord(emptyList(), server)
        assertNull(match)
    }

    @Test
    fun `ConflictResolver findMatchingLocalExerciseRecord finds by serverId`() {
        val locals = listOf(
            ExerciseRecordEntity(
                localId = 1, serverId = 10, userId = 1,
                actualCalories = 100.0, actualDuration = 30,
                exerciseDate = "2026-02-06", createdAt = "2026-02-06T10:00:00"
            ),
            ExerciseRecordEntity(
                localId = 2, serverId = 20, userId = 1,
                actualCalories = 200.0, actualDuration = 45,
                exerciseDate = "2026-02-06", createdAt = "2026-02-06T11:00:00"
            )
        )

        val match = ConflictResolver.findMatchingLocalExerciseRecord(locals, 20)
        assertNotNull(match)
        assertEquals(2L, match!!.localId)
    }

    @Test
    fun `ConflictResolver findMatchingLocalExerciseRecord returns null when not found`() {
        val locals = listOf(
            ExerciseRecordEntity(
                localId = 1, serverId = 10, userId = 1,
                actualCalories = 100.0, actualDuration = 30,
                exerciseDate = "2026-02-06", createdAt = "2026-02-06T10:00:00"
            )
        )
        val match = ConflictResolver.findMatchingLocalExerciseRecord(locals, 999)
        assertNull(match)
    }

    // ==================== ConflictResolver 边缘情况测试 ====================

    @Test
    fun `ConflictResolver resolve handles protein change`() {
        val local = createLocalDietEntity(isSynced = true, serverId = 100, protein = 10.0)
        val server = createServerDietRecord(id = 100, protein = 15.0)

        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals(15.0, resolved.protein, 0.01)
    }

    @Test
    fun `ConflictResolver resolve handles fat change`() {
        val local = createLocalDietEntity(isSynced = true, serverId = 100, fat = 8.0)
        val server = createServerDietRecord(id = 100, fat = 12.0)

        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals(12.0, resolved.fat, 0.01)
    }

    @Test
    fun `ConflictResolver resolve handles carbs change`() {
        val local = createLocalDietEntity(isSynced = true, serverId = 100, carbs = 6.0)
        val server = createServerDietRecord(id = 100, carbs = 20.0)

        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals(20.0, resolved.carbs, 0.01)
    }

    @Test
    fun `ConflictResolver resolve handles mealType change`() {
        val local = createLocalDietEntity(isSynced = true, serverId = 100, mealType = "lunch")
        val server = createServerDietRecord(id = 100, mealType = "dinner")

        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals("dinner", resolved.mealType)
    }

    @Test
    fun `ConflictResolver resolve handles recordDate change`() {
        val local = createLocalDietEntity(isSynced = true, serverId = 100, recordDate = "2026-02-06")
        val server = createServerDietRecord(id = 100, recordDate = "2026-02-07")

        assertTrue(ConflictResolver.shouldUpdateLocalDietRecord(local, server))
        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals("2026-02-07", resolved.recordDate)
    }

    @Test
    fun `ConflictResolver resolve preserves localId`() {
        val local = createLocalDietEntity(localId = 42, isSynced = true, serverId = 100)
        val server = createServerDietRecord(id = 100)

        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals(42L, resolved.localId)
    }

    @Test
    fun `ConflictResolver resolve preserves userId`() {
        val local = createLocalDietEntity(userId = 5, isSynced = true, serverId = 100)
        val server = createServerDietRecord(id = 100, userId = 5)

        val resolved = ConflictResolver.resolveDietRecord(local, server)
        assertEquals(5, resolved.userId)
    }

    @Test
    fun `ConflictResolver multiple local records with mixed sync states`() {
        val locals = listOf(
            createLocalDietEntity(localId = 1, serverId = 100, isSynced = true),
            createLocalDietEntity(localId = 2, serverId = null, isSynced = false, foodName = "离线1"),
            createLocalDietEntity(localId = 3, serverId = 200, isSynced = true),
            createLocalDietEntity(localId = 4, serverId = null, isSynced = false, foodName = "离线2")
        )

        // 只有serverId匹配的才找得到
        val serverRecord100 = createServerDietRecord(id = 100)
        val match100 = ConflictResolver.findMatchingLocalRecord(locals, serverRecord100)
        assertNotNull(match100)
        assertEquals(1L, match100!!.localId)

        val serverRecord300 = createServerDietRecord(id = 300)
        val match300 = ConflictResolver.findMatchingLocalRecord(locals, serverRecord300)
        assertNull(match300)

        // 未同步记录不应被更新
        val unsyncedLocal = locals[1]
        assertFalse(ConflictResolver.shouldUpdateLocalDietRecord(unsyncedLocal, serverRecord100))
    }

    // ==================== DietRecordEntity 数据完整性测试 ====================

    @Test
    fun `DietRecordEntity default isSynced is false`() {
        val entity = DietRecordEntity(
            userId = 1, foodName = "测试", calories = 100.0,
            mealType = "lunch", recordDate = "2026-02-06", createdAt = "2026-02-06T12:00:00"
        )
        assertFalse(entity.isSynced)
        assertNull(entity.serverId)
        assertEquals(0L, entity.localId)
    }

    @Test
    fun `DietRecordEntity copy changes isSynced`() {
        val entity = DietRecordEntity(
            userId = 1, foodName = "测试", calories = 100.0,
            mealType = "lunch", recordDate = "2026-02-06", createdAt = "2026-02-06T12:00:00",
            isSynced = false
        )
        val synced = entity.copy(isSynced = true, serverId = 42)
        assertFalse(entity.isSynced)
        assertTrue(synced.isSynced)
        assertEquals(42, synced.serverId)
    }

    @Test
    fun `ExerciseRecordEntity default values`() {
        val entity = ExerciseRecordEntity(
            userId = 1, actualCalories = 200.0, actualDuration = 30,
            exerciseDate = "2026-02-06", createdAt = "2026-02-06T10:00:00"
        )
        assertFalse(entity.isSynced)
        assertNull(entity.serverId)
        assertNull(entity.planId)
        assertEquals("walking", entity.exerciseType)
        assertNull(entity.distance)
        assertNull(entity.startedAt)
        assertNull(entity.endedAt)
        assertNull(entity.notes)
    }

    // ==================== SyncResult 合并场景测试 ====================

    @Test
    fun `SyncResult merge upload and download results`() {
        val upload = SyncResult(
            uploadedDietRecords = 3,
            uploadedExerciseRecords = 1,
            errors = listOf("上传错误1")
        )
        val download = SyncResult(
            downloadedDietRecords = 5,
            downloadedTripPlans = 2,
            userPreferencesSynced = true,
            errors = listOf("下载错误1")
        )
        // 模拟SyncManager中的合并逻辑
        val merged = SyncResult(
            uploadedDietRecords = upload.uploadedDietRecords,
            uploadedExerciseRecords = upload.uploadedExerciseRecords,
            downloadedDietRecords = download.downloadedDietRecords,
            downloadedTripPlans = download.downloadedTripPlans,
            userPreferencesSynced = download.userPreferencesSynced,
            errors = upload.errors + download.errors
        )
        assertEquals(3, merged.uploadedDietRecords)
        assertEquals(1, merged.uploadedExerciseRecords)
        assertEquals(5, merged.downloadedDietRecords)
        assertEquals(2, merged.downloadedTripPlans)
        assertTrue(merged.userPreferencesSynced)
        assertEquals(2, merged.errors.size)
        assertTrue(merged.hasErrors)
        // 3+1+5+0+2+1 = 12
        assertEquals(12, merged.totalSynced)
    }

    @Test
    fun `SyncResult all zeros results in zero total`() {
        val result = SyncResult()
        assertEquals(0, result.totalSynced)
        assertTrue(result.isFullSuccess)
    }

    // ==================== 边缘情况：大量数据 ====================

    @Test
    fun `ConflictResolver handles large list efficiently`() {
        val locals = (1..1000).map { i ->
            createLocalDietEntity(localId = i.toLong(), serverId = i * 10, isSynced = true)
        }
        val server = createServerDietRecord(id = 5000)
        val match = ConflictResolver.findMatchingLocalRecord(locals, server)
        // serverId=5000 corresponds to localId=500
        assertNotNull(match)
        assertEquals(500L, match!!.localId)
    }

    @Test
    fun `ConflictResolver handles large list no match`() {
        val locals = (1..100).map { i ->
            createLocalDietEntity(localId = i.toLong(), serverId = i, isSynced = true)
        }
        val server = createServerDietRecord(id = 999)
        val match = ConflictResolver.findMatchingLocalRecord(locals, server)
        assertNull(match)
    }

    // ==================== SyncConfig 边缘测试 ====================

    @Test
    fun `SyncConfig with zero interval`() {
        val config = SyncConfig(syncIntervalMs = 0L)
        assertEquals(0L, config.syncIntervalMs)
    }

    @Test
    fun `SyncConfig with disabled auto sync`() {
        val config = SyncConfig(enableAutoSync = false)
        assertFalse(config.enableAutoSync)
    }

    @Test
    fun `SyncConfig with very large interval`() {
        val config = SyncConfig(syncIntervalMs = Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, config.syncIntervalMs)
    }
}
