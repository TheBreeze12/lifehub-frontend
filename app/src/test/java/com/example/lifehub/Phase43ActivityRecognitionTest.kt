package com.example.lifehub

import com.example.lifehub.data.ActivityRecognitionResult
import com.example.lifehub.data.ActivityRecognitionState
import com.example.lifehub.data.ActivityRecognitionUtils
import com.example.lifehub.data.DetectedActivityType
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 43: Activity Recognition 单元测试
 *
 * 测试范围:
 * 1. DetectedActivityType 枚举正确性
 * 2. ActivityRecognitionResult 数据模型
 * 3. ActivityRecognitionState 状态模型
 * 4. GMS活动类型映射 (mapFromGmsActivityType)
 * 5. 运动类型映射 (mapToExerciseType)
 * 6. 置信度阈值判断 (isHighConfidence / isAcceptableConfidence)
 * 7. 有效运动判断 (isActiveExercise)
 * 8. 最可能活动选择 (getMostProbableActivity)
 * 9. 可靠活动过滤 (filterReliableActivities)
 * 10. 自动追踪判断 (shouldAutoStartTracking)
 * 11. METs估算与热量计算
 * 12. 活动变化检测 (hasActivityChanged)
 * 13. 图标映射 (getActivityIcon)
 * 14. 边界情况与异常输入
 */
class Phase43ActivityRecognitionTest {

    // ==================== 1. DetectedActivityType 枚举测试 ====================

    @Test
    fun `test DetectedActivityType - all types exist`() {
        val allTypes = DetectedActivityType.values()
        assertEquals(8, allTypes.size)
        assertTrue(allTypes.contains(DetectedActivityType.STILL))
        assertTrue(allTypes.contains(DetectedActivityType.WALKING))
        assertTrue(allTypes.contains(DetectedActivityType.RUNNING))
        assertTrue(allTypes.contains(DetectedActivityType.CYCLING))
        assertTrue(allTypes.contains(DetectedActivityType.IN_VEHICLE))
        assertTrue(allTypes.contains(DetectedActivityType.ON_FOOT))
        assertTrue(allTypes.contains(DetectedActivityType.TILTING))
        assertTrue(allTypes.contains(DetectedActivityType.UNKNOWN))
    }

    @Test
    fun `test DetectedActivityType - Chinese labels`() {
        assertEquals("静止", DetectedActivityType.STILL.label)
        assertEquals("步行", DetectedActivityType.WALKING.label)
        assertEquals("跑步", DetectedActivityType.RUNNING.label)
        assertEquals("骑行", DetectedActivityType.CYCLING.label)
        assertEquals("乘车", DetectedActivityType.IN_VEHICLE.label)
        assertEquals("步行中", DetectedActivityType.ON_FOOT.label)
        assertEquals("倾斜", DetectedActivityType.TILTING.label)
        assertEquals("未知", DetectedActivityType.UNKNOWN.label)
    }

    @Test
    fun `test DetectedActivityType - English labels`() {
        assertEquals("Still", DetectedActivityType.STILL.labelEn)
        assertEquals("Walking", DetectedActivityType.WALKING.labelEn)
        assertEquals("Running", DetectedActivityType.RUNNING.labelEn)
        assertEquals("Cycling", DetectedActivityType.CYCLING.labelEn)
        assertEquals("In Vehicle", DetectedActivityType.IN_VEHICLE.labelEn)
        assertEquals("On Foot", DetectedActivityType.ON_FOOT.labelEn)
        assertEquals("Tilting", DetectedActivityType.TILTING.labelEn)
        assertEquals("Unknown", DetectedActivityType.UNKNOWN.labelEn)
    }

    @Test
    fun `test DetectedActivityType - valueOf works`() {
        assertEquals(DetectedActivityType.WALKING, DetectedActivityType.valueOf("WALKING"))
        assertEquals(DetectedActivityType.RUNNING, DetectedActivityType.valueOf("RUNNING"))
        assertEquals(DetectedActivityType.CYCLING, DetectedActivityType.valueOf("CYCLING"))
        assertEquals(DetectedActivityType.STILL, DetectedActivityType.valueOf("STILL"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test DetectedActivityType - invalid valueOf throws`() {
        DetectedActivityType.valueOf("SWIMMING")
    }

    // ==================== 2. ActivityRecognitionResult 数据模型测试 ====================

    @Test
    fun `test ActivityRecognitionResult - walking with high confidence`() {
        val result = ActivityRecognitionResult(
            activityType = DetectedActivityType.WALKING,
            confidence = 90,
            timestamp = 1000L
        )
        assertEquals(DetectedActivityType.WALKING, result.activityType)
        assertEquals(90, result.confidence)
        assertEquals(1000L, result.timestamp)
    }

    @Test
    fun `test ActivityRecognitionResult - running with medium confidence`() {
        val result = ActivityRecognitionResult(
            activityType = DetectedActivityType.RUNNING,
            confidence = 60
        )
        assertEquals(DetectedActivityType.RUNNING, result.activityType)
        assertEquals(60, result.confidence)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `test ActivityRecognitionResult - still with zero confidence`() {
        val result = ActivityRecognitionResult(
            activityType = DetectedActivityType.STILL,
            confidence = 0,
            timestamp = 500L
        )
        assertEquals(DetectedActivityType.STILL, result.activityType)
        assertEquals(0, result.confidence)
    }

    @Test
    fun `test ActivityRecognitionResult - max confidence 100`() {
        val result = ActivityRecognitionResult(
            activityType = DetectedActivityType.CYCLING,
            confidence = 100
        )
        assertEquals(100, result.confidence)
    }

    @Test
    fun `test ActivityRecognitionResult - default timestamp is set`() {
        val before = System.currentTimeMillis()
        val result = ActivityRecognitionResult(
            activityType = DetectedActivityType.WALKING,
            confidence = 80
        )
        val after = System.currentTimeMillis()
        assertTrue(result.timestamp in before..after)
    }

    @Test
    fun `test ActivityRecognitionResult - copy works`() {
        val original = ActivityRecognitionResult(
            activityType = DetectedActivityType.WALKING,
            confidence = 80,
            timestamp = 1000L
        )
        val modified = original.copy(confidence = 95)
        assertEquals(DetectedActivityType.WALKING, modified.activityType)
        assertEquals(95, modified.confidence)
        assertEquals(1000L, modified.timestamp)
    }

    @Test
    fun `test ActivityRecognitionResult - equality`() {
        val r1 = ActivityRecognitionResult(DetectedActivityType.RUNNING, 85, 1000L)
        val r2 = ActivityRecognitionResult(DetectedActivityType.RUNNING, 85, 1000L)
        assertEquals(r1, r2)
    }

    @Test
    fun `test ActivityRecognitionResult - inequality different type`() {
        val r1 = ActivityRecognitionResult(DetectedActivityType.RUNNING, 85, 1000L)
        val r2 = ActivityRecognitionResult(DetectedActivityType.WALKING, 85, 1000L)
        assertNotEquals(r1, r2)
    }

    @Test
    fun `test ActivityRecognitionResult - inequality different confidence`() {
        val r1 = ActivityRecognitionResult(DetectedActivityType.RUNNING, 85, 1000L)
        val r2 = ActivityRecognitionResult(DetectedActivityType.RUNNING, 50, 1000L)
        assertNotEquals(r1, r2)
    }

    // ==================== 3. ActivityRecognitionState 状态模型测试 ====================

    @Test
    fun `test ActivityRecognitionState - Idle`() {
        val state: ActivityRecognitionState = ActivityRecognitionState.Idle
        assertTrue(state is ActivityRecognitionState.Idle)
        assertFalse(state is ActivityRecognitionState.Monitoring)
        assertFalse(state is ActivityRecognitionState.Error)
    }

    @Test
    fun `test ActivityRecognitionState - Monitoring`() {
        val state: ActivityRecognitionState = ActivityRecognitionState.Monitoring
        assertTrue(state is ActivityRecognitionState.Monitoring)
        assertFalse(state is ActivityRecognitionState.Idle)
    }

    @Test
    fun `test ActivityRecognitionState - Error with message`() {
        val state: ActivityRecognitionState = ActivityRecognitionState.Error("权限被拒绝")
        assertTrue(state is ActivityRecognitionState.Error)
        assertEquals("权限被拒绝", (state as ActivityRecognitionState.Error).message)
    }

    @Test
    fun `test ActivityRecognitionState - Error with empty message`() {
        val state = ActivityRecognitionState.Error("")
        assertEquals("", state.message)
    }

    // ==================== 4. GMS活动类型映射测试 ====================

    @Test
    fun `test mapFromGmsActivityType - IN_VEHICLE (0)`() {
        assertEquals(DetectedActivityType.IN_VEHICLE, ActivityRecognitionUtils.mapFromGmsActivityType(0))
    }

    @Test
    fun `test mapFromGmsActivityType - ON_BICYCLE (1)`() {
        assertEquals(DetectedActivityType.CYCLING, ActivityRecognitionUtils.mapFromGmsActivityType(1))
    }

    @Test
    fun `test mapFromGmsActivityType - ON_FOOT (2)`() {
        assertEquals(DetectedActivityType.ON_FOOT, ActivityRecognitionUtils.mapFromGmsActivityType(2))
    }

    @Test
    fun `test mapFromGmsActivityType - STILL (3)`() {
        assertEquals(DetectedActivityType.STILL, ActivityRecognitionUtils.mapFromGmsActivityType(3))
    }

    @Test
    fun `test mapFromGmsActivityType - UNKNOWN (4)`() {
        assertEquals(DetectedActivityType.UNKNOWN, ActivityRecognitionUtils.mapFromGmsActivityType(4))
    }

    @Test
    fun `test mapFromGmsActivityType - TILTING (5)`() {
        assertEquals(DetectedActivityType.TILTING, ActivityRecognitionUtils.mapFromGmsActivityType(5))
    }

    @Test
    fun `test mapFromGmsActivityType - WALKING (7)`() {
        assertEquals(DetectedActivityType.WALKING, ActivityRecognitionUtils.mapFromGmsActivityType(7))
    }

    @Test
    fun `test mapFromGmsActivityType - RUNNING (8)`() {
        assertEquals(DetectedActivityType.RUNNING, ActivityRecognitionUtils.mapFromGmsActivityType(8))
    }

    @Test
    fun `test mapFromGmsActivityType - unmapped value 6 returns UNKNOWN`() {
        assertEquals(DetectedActivityType.UNKNOWN, ActivityRecognitionUtils.mapFromGmsActivityType(6))
    }

    @Test
    fun `test mapFromGmsActivityType - negative value returns UNKNOWN`() {
        assertEquals(DetectedActivityType.UNKNOWN, ActivityRecognitionUtils.mapFromGmsActivityType(-1))
    }

    @Test
    fun `test mapFromGmsActivityType - large value returns UNKNOWN`() {
        assertEquals(DetectedActivityType.UNKNOWN, ActivityRecognitionUtils.mapFromGmsActivityType(999))
    }

    @Test
    fun `test mapFromGmsActivityType - all known GMS types covered`() {
        // GMS type 4 IS DetectedActivity.UNKNOWN, so it correctly maps to UNKNOWN
        val knownNonUnknownTypes = listOf(0, 1, 2, 3, 5, 7, 8)
        knownNonUnknownTypes.forEach { type ->
            val result = ActivityRecognitionUtils.mapFromGmsActivityType(type)
            assertNotEquals(
                "GMS type $type should not map to UNKNOWN",
                DetectedActivityType.UNKNOWN,
                result
            )
        }
        // GMS type 4 (DetectedActivity.UNKNOWN) should map to our UNKNOWN
        assertEquals(
            DetectedActivityType.UNKNOWN,
            ActivityRecognitionUtils.mapFromGmsActivityType(4)
        )
    }

    // ==================== 5. 运动类型映射测试 ====================

    @Test
    fun `test mapToExerciseType - WALKING maps to walking`() {
        assertEquals("walking", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.WALKING))
    }

    @Test
    fun `test mapToExerciseType - RUNNING maps to running`() {
        assertEquals("running", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.RUNNING))
    }

    @Test
    fun `test mapToExerciseType - CYCLING maps to cycling`() {
        assertEquals("cycling", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.CYCLING))
    }

    @Test
    fun `test mapToExerciseType - ON_FOOT maps to walking`() {
        assertEquals("walking", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.ON_FOOT))
    }

    @Test
    fun `test mapToExerciseType - STILL maps to still`() {
        assertEquals("still", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.STILL))
    }

    @Test
    fun `test mapToExerciseType - IN_VEHICLE maps to in_vehicle`() {
        assertEquals("in_vehicle", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.IN_VEHICLE))
    }

    @Test
    fun `test mapToExerciseType - TILTING maps to unknown`() {
        assertEquals("unknown", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.TILTING))
    }

    @Test
    fun `test mapToExerciseType - UNKNOWN maps to unknown`() {
        assertEquals("unknown", ActivityRecognitionUtils.mapToExerciseType(DetectedActivityType.UNKNOWN))
    }

    @Test
    fun `test mapToExerciseType - all types have mapping`() {
        DetectedActivityType.values().forEach { type ->
            val exerciseType = ActivityRecognitionUtils.mapToExerciseType(type)
            assertTrue(
                "Type ${type.name} should have non-empty mapping",
                exerciseType.isNotEmpty()
            )
        }
    }

    // ==================== 6. 置信度阈值判断测试 ====================

    @Test
    fun `test isHighConfidence - exactly at threshold`() {
        assertTrue(ActivityRecognitionUtils.isHighConfidence(75))
    }

    @Test
    fun `test isHighConfidence - above threshold`() {
        assertTrue(ActivityRecognitionUtils.isHighConfidence(76))
        assertTrue(ActivityRecognitionUtils.isHighConfidence(100))
    }

    @Test
    fun `test isHighConfidence - below threshold`() {
        assertFalse(ActivityRecognitionUtils.isHighConfidence(74))
        assertFalse(ActivityRecognitionUtils.isHighConfidence(50))
        assertFalse(ActivityRecognitionUtils.isHighConfidence(0))
    }

    @Test
    fun `test isAcceptableConfidence - exactly at threshold`() {
        assertTrue(ActivityRecognitionUtils.isAcceptableConfidence(50))
    }

    @Test
    fun `test isAcceptableConfidence - above threshold`() {
        assertTrue(ActivityRecognitionUtils.isAcceptableConfidence(51))
        assertTrue(ActivityRecognitionUtils.isAcceptableConfidence(100))
    }

    @Test
    fun `test isAcceptableConfidence - below threshold`() {
        assertFalse(ActivityRecognitionUtils.isAcceptableConfidence(49))
        assertFalse(ActivityRecognitionUtils.isAcceptableConfidence(0))
    }

    @Test
    fun `test confidence thresholds - constants are correct`() {
        assertEquals(50, ActivityRecognitionUtils.MIN_CONFIDENCE_THRESHOLD)
        assertEquals(75, ActivityRecognitionUtils.HIGH_CONFIDENCE_THRESHOLD)
        assertTrue(
            ActivityRecognitionUtils.MIN_CONFIDENCE_THRESHOLD <
                ActivityRecognitionUtils.HIGH_CONFIDENCE_THRESHOLD
        )
    }

    @Test
    fun `test isHighConfidence - negative value`() {
        assertFalse(ActivityRecognitionUtils.isHighConfidence(-1))
    }

    @Test
    fun `test isAcceptableConfidence - negative value`() {
        assertFalse(ActivityRecognitionUtils.isAcceptableConfidence(-1))
    }

    // ==================== 7. 有效运动判断测试 ====================

    @Test
    fun `test isActiveExercise - WALKING is active`() {
        assertTrue(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.WALKING))
    }

    @Test
    fun `test isActiveExercise - RUNNING is active`() {
        assertTrue(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.RUNNING))
    }

    @Test
    fun `test isActiveExercise - CYCLING is active`() {
        assertTrue(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.CYCLING))
    }

    @Test
    fun `test isActiveExercise - ON_FOOT is active`() {
        assertTrue(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.ON_FOOT))
    }

    @Test
    fun `test isActiveExercise - STILL is not active`() {
        assertFalse(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.STILL))
    }

    @Test
    fun `test isActiveExercise - IN_VEHICLE is not active`() {
        assertFalse(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.IN_VEHICLE))
    }

    @Test
    fun `test isActiveExercise - TILTING is not active`() {
        assertFalse(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.TILTING))
    }

    @Test
    fun `test isActiveExercise - UNKNOWN is not active`() {
        assertFalse(ActivityRecognitionUtils.isActiveExercise(DetectedActivityType.UNKNOWN))
    }

    @Test
    fun `test isActiveExercise - exactly 4 active types`() {
        val activeCount = DetectedActivityType.values().count {
            ActivityRecognitionUtils.isActiveExercise(it)
        }
        assertEquals(4, activeCount)
    }

    // ==================== 8. 最可能活动选择测试 ====================

    @Test
    fun `test getMostProbableActivity - single result`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 80)
        )
        val most = ActivityRecognitionUtils.getMostProbableActivity(results)
        assertNotNull(most)
        assertEquals(DetectedActivityType.WALKING, most!!.activityType)
        assertEquals(80, most.confidence)
    }

    @Test
    fun `test getMostProbableActivity - multiple results picks highest`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.STILL, 30),
            ActivityRecognitionResult(DetectedActivityType.WALKING, 60),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 10)
        )
        val most = ActivityRecognitionUtils.getMostProbableActivity(results)
        assertNotNull(most)
        assertEquals(DetectedActivityType.WALKING, most!!.activityType)
        assertEquals(60, most.confidence)
    }

    @Test
    fun `test getMostProbableActivity - empty list returns null`() {
        val most = ActivityRecognitionUtils.getMostProbableActivity(emptyList())
        assertNull(most)
    }

    @Test
    fun `test getMostProbableActivity - all same confidence picks first max`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 50),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 50),
            ActivityRecognitionResult(DetectedActivityType.CYCLING, 50)
        )
        val most = ActivityRecognitionUtils.getMostProbableActivity(results)
        assertNotNull(most)
        assertEquals(50, most!!.confidence)
    }

    @Test
    fun `test getMostProbableActivity - all zero confidence`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 0),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 0)
        )
        val most = ActivityRecognitionUtils.getMostProbableActivity(results)
        assertNotNull(most)
        assertEquals(0, most!!.confidence)
    }

    @Test
    fun `test getMostProbableActivity - high confidence non-exercise vs low confidence exercise`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.STILL, 95),
            ActivityRecognitionResult(DetectedActivityType.WALKING, 5)
        )
        val most = ActivityRecognitionUtils.getMostProbableActivity(results)
        assertNotNull(most)
        assertEquals(DetectedActivityType.STILL, most!!.activityType)
        assertEquals(95, most.confidence)
    }

    // ==================== 9. 可靠活动过滤测试 ====================

    @Test
    fun `test filterReliableActivities - filters below threshold`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 80),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 30),
            ActivityRecognitionResult(DetectedActivityType.CYCLING, 60)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results)
        assertEquals(2, reliable.size)
        assertTrue(reliable.all { it.confidence >= 50 })
    }

    @Test
    fun `test filterReliableActivities - all above threshold`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 80),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 90)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results)
        assertEquals(2, reliable.size)
    }

    @Test
    fun `test filterReliableActivities - all below threshold`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 10),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 20)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results)
        assertTrue(reliable.isEmpty())
    }

    @Test
    fun `test filterReliableActivities - empty list`() {
        val reliable = ActivityRecognitionUtils.filterReliableActivities(emptyList())
        assertTrue(reliable.isEmpty())
    }

    @Test
    fun `test filterReliableActivities - custom threshold`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 80),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 70),
            ActivityRecognitionResult(DetectedActivityType.CYCLING, 60)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results, minConfidence = 75)
        assertEquals(1, reliable.size)
        assertEquals(DetectedActivityType.WALKING, reliable[0].activityType)
    }

    @Test
    fun `test filterReliableActivities - exactly at threshold is included`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 50)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results, minConfidence = 50)
        assertEquals(1, reliable.size)
    }

    @Test
    fun `test filterReliableActivities - threshold 0 includes all`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 0),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 10)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results, minConfidence = 0)
        assertEquals(2, reliable.size)
    }

    @Test
    fun `test filterReliableActivities - threshold 100 only perfect`() {
        val results = listOf(
            ActivityRecognitionResult(DetectedActivityType.WALKING, 99),
            ActivityRecognitionResult(DetectedActivityType.RUNNING, 100)
        )
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results, minConfidence = 100)
        assertEquals(1, reliable.size)
        assertEquals(DetectedActivityType.RUNNING, reliable[0].activityType)
    }

    // ==================== 10. 自动追踪判断测试 ====================

    @Test
    fun `test shouldAutoStartTracking - walking high confidence`() {
        val result = ActivityRecognitionResult(DetectedActivityType.WALKING, 80)
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - running high confidence`() {
        val result = ActivityRecognitionResult(DetectedActivityType.RUNNING, 90)
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - cycling at threshold`() {
        val result = ActivityRecognitionResult(DetectedActivityType.CYCLING, 50)
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - on_foot high confidence`() {
        val result = ActivityRecognitionResult(DetectedActivityType.ON_FOOT, 75)
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - walking low confidence should not`() {
        val result = ActivityRecognitionResult(DetectedActivityType.WALKING, 30)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - still high confidence should not`() {
        val result = ActivityRecognitionResult(DetectedActivityType.STILL, 95)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - in_vehicle high confidence should not`() {
        val result = ActivityRecognitionResult(DetectedActivityType.IN_VEHICLE, 90)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - unknown high confidence should not`() {
        val result = ActivityRecognitionResult(DetectedActivityType.UNKNOWN, 80)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - tilting should not`() {
        val result = ActivityRecognitionResult(DetectedActivityType.TILTING, 100)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - walking zero confidence should not`() {
        val result = ActivityRecognitionResult(DetectedActivityType.WALKING, 0)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - walking at exactly min threshold`() {
        val result = ActivityRecognitionResult(
            DetectedActivityType.WALKING,
            ActivityRecognitionUtils.MIN_CONFIDENCE_THRESHOLD
        )
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    @Test
    fun `test shouldAutoStartTracking - walking just below min threshold`() {
        val result = ActivityRecognitionResult(
            DetectedActivityType.WALKING,
            ActivityRecognitionUtils.MIN_CONFIDENCE_THRESHOLD - 1
        )
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(result))
    }

    // ==================== 11. METs估算与热量计算测试 ====================

    @Test
    fun `test getEstimatedMets - STILL is 1_0`() {
        assertEquals(1.0, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.STILL), 0.01)
    }

    @Test
    fun `test getEstimatedMets - WALKING is 3_5`() {
        assertEquals(3.5, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.WALKING), 0.01)
    }

    @Test
    fun `test getEstimatedMets - ON_FOOT is 3_5`() {
        assertEquals(3.5, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.ON_FOOT), 0.01)
    }

    @Test
    fun `test getEstimatedMets - RUNNING is 8_0`() {
        assertEquals(8.0, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.RUNNING), 0.01)
    }

    @Test
    fun `test getEstimatedMets - CYCLING is 6_0`() {
        assertEquals(6.0, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.CYCLING), 0.01)
    }

    @Test
    fun `test getEstimatedMets - IN_VEHICLE is 1_0`() {
        assertEquals(1.0, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.IN_VEHICLE), 0.01)
    }

    @Test
    fun `test getEstimatedMets - UNKNOWN is 1_5`() {
        assertEquals(1.5, ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.UNKNOWN), 0.01)
    }

    @Test
    fun `test getEstimatedMets - all types have positive value`() {
        DetectedActivityType.values().forEach { type ->
            assertTrue(
                "METs for ${type.name} should be positive",
                ActivityRecognitionUtils.getEstimatedMets(type) > 0
            )
        }
    }

    @Test
    fun `test getEstimatedMets - WALKING and ON_FOOT are same`() {
        assertEquals(
            ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.WALKING),
            ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.ON_FOOT),
            0.01
        )
    }

    @Test
    fun `test getEstimatedMets - RUNNING greater than WALKING`() {
        assertTrue(
            ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.RUNNING) >
                ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.WALKING)
        )
    }

    @Test
    fun `test getEstimatedMets - CYCLING between WALKING and RUNNING`() {
        val cycling = ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.CYCLING)
        val walking = ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.WALKING)
        val running = ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.RUNNING)
        assertTrue(cycling > walking)
        assertTrue(cycling < running)
    }

    @Test
    fun `test estimateCaloriesFromActivity - walking 30 min 70 kg`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.WALKING, 30.0, 70.0
        )
        // METs 3.5 * 70 kg * 0.5 h = 122.5 kcal
        assertEquals(122.5, calories, 0.1)
    }

    @Test
    fun `test estimateCaloriesFromActivity - running 30 min 70 kg`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, 30.0, 70.0
        )
        // METs 8.0 * 70 kg * 0.5 h = 280.0 kcal
        assertEquals(280.0, calories, 0.1)
    }

    @Test
    fun `test estimateCaloriesFromActivity - cycling 60 min 80 kg`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.CYCLING, 60.0, 80.0
        )
        // METs 6.0 * 80 kg * 1.0 h = 480.0 kcal
        assertEquals(480.0, calories, 0.1)
    }

    @Test
    fun `test estimateCaloriesFromActivity - still 30 min`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.STILL, 30.0, 70.0
        )
        // METs 1.0 * 70 kg * 0.5 h = 35.0 kcal
        assertEquals(35.0, calories, 0.1)
    }

    @Test
    fun `test estimateCaloriesFromActivity - zero duration returns 0`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, 0.0, 70.0
        )
        assertEquals(0.0, calories, 0.01)
    }

    @Test
    fun `test estimateCaloriesFromActivity - negative duration returns 0`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, -10.0, 70.0
        )
        assertEquals(0.0, calories, 0.01)
    }

    @Test
    fun `test estimateCaloriesFromActivity - zero weight returns 0`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, 30.0, 0.0
        )
        assertEquals(0.0, calories, 0.01)
    }

    @Test
    fun `test estimateCaloriesFromActivity - negative weight returns 0`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, 30.0, -70.0
        )
        assertEquals(0.0, calories, 0.01)
    }

    @Test
    fun `test estimateCaloriesFromActivity - default weight is 70 kg`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.WALKING, 60.0
        )
        // METs 3.5 * 70 * 1.0 h = 245.0 kcal
        assertEquals(245.0, calories, 0.1)
    }

    @Test
    fun `test estimateCaloriesFromActivity - very short duration`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, 1.0, 70.0
        )
        // METs 8.0 * 70 * (1/60) h ≈ 9.33 kcal
        assertEquals(9.33, calories, 0.1)
    }

    @Test
    fun `test estimateCaloriesFromActivity - very long duration`() {
        val calories = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.WALKING, 480.0, 70.0
        )
        // METs 3.5 * 70 * 8.0 h = 1960.0 kcal
        assertEquals(1960.0, calories, 0.1)
    }

    // ==================== 12. 活动变化检测测试 ====================

    @Test
    fun `test hasActivityChanged - null previous always changed`() {
        val current = ActivityRecognitionResult(DetectedActivityType.WALKING, 80)
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(null, current))
    }

    @Test
    fun `test hasActivityChanged - same type not changed`() {
        val previous = ActivityRecognitionResult(DetectedActivityType.WALKING, 80, 1000L)
        val current = ActivityRecognitionResult(DetectedActivityType.WALKING, 90, 2000L)
        assertFalse(ActivityRecognitionUtils.hasActivityChanged(previous, current))
    }

    @Test
    fun `test hasActivityChanged - different type is changed`() {
        val previous = ActivityRecognitionResult(DetectedActivityType.WALKING, 80)
        val current = ActivityRecognitionResult(DetectedActivityType.RUNNING, 80)
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(previous, current))
    }

    @Test
    fun `test hasActivityChanged - STILL to WALKING`() {
        val previous = ActivityRecognitionResult(DetectedActivityType.STILL, 90)
        val current = ActivityRecognitionResult(DetectedActivityType.WALKING, 70)
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(previous, current))
    }

    @Test
    fun `test hasActivityChanged - WALKING to STILL`() {
        val previous = ActivityRecognitionResult(DetectedActivityType.WALKING, 80)
        val current = ActivityRecognitionResult(DetectedActivityType.STILL, 90)
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(previous, current))
    }

    @Test
    fun `test hasActivityChanged - WALKING to RUNNING transition`() {
        val previous = ActivityRecognitionResult(DetectedActivityType.WALKING, 80)
        val current = ActivityRecognitionResult(DetectedActivityType.RUNNING, 75)
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(previous, current))
    }

    @Test
    fun `test hasActivityChanged - confidence change only not a change`() {
        val previous = ActivityRecognitionResult(DetectedActivityType.RUNNING, 60, 1000L)
        val current = ActivityRecognitionResult(DetectedActivityType.RUNNING, 95, 2000L)
        assertFalse(ActivityRecognitionUtils.hasActivityChanged(previous, current))
    }

    // ==================== 13. 图标映射测试 ====================

    @Test
    fun `test getActivityIcon - all types have icon`() {
        DetectedActivityType.values().forEach { type ->
            val icon = ActivityRecognitionUtils.getActivityIcon(type)
            assertTrue(
                "Icon for ${type.name} should not be empty",
                icon.isNotEmpty()
            )
        }
    }

    @Test
    fun `test getActivityIcon - WALKING uses directions_walk`() {
        assertEquals("directions_walk", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.WALKING))
    }

    @Test
    fun `test getActivityIcon - ON_FOOT uses directions_walk`() {
        assertEquals("directions_walk", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.ON_FOOT))
    }

    @Test
    fun `test getActivityIcon - RUNNING uses directions_run`() {
        assertEquals("directions_run", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.RUNNING))
    }

    @Test
    fun `test getActivityIcon - CYCLING uses directions_bike`() {
        assertEquals("directions_bike", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.CYCLING))
    }

    @Test
    fun `test getActivityIcon - IN_VEHICLE uses directions_car`() {
        assertEquals("directions_car", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.IN_VEHICLE))
    }

    @Test
    fun `test getActivityIcon - STILL uses accessibility_new`() {
        assertEquals("accessibility_new", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.STILL))
    }

    @Test
    fun `test getActivityIcon - UNKNOWN uses help_outline`() {
        assertEquals("help_outline", ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.UNKNOWN))
    }

    @Test
    fun `test getActivityIcon - WALKING and ON_FOOT have same icon`() {
        assertEquals(
            ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.WALKING),
            ActivityRecognitionUtils.getActivityIcon(DetectedActivityType.ON_FOOT)
        )
    }

    // ==================== 14. 边界情况与综合测试 ====================

    @Test
    fun `test comprehensive - typical walking detection flow`() {
        // 模拟典型步行检测流程
        val gmsResults = listOf(
            Pair(3, 20),  // STILL confidence 20
            Pair(7, 60),  // WALKING confidence 60
            Pair(2, 15),  // ON_FOOT confidence 15
            Pair(8, 5)    // RUNNING confidence 5
        )

        val mapped = gmsResults.map { (type, conf) ->
            ActivityRecognitionResult(
                activityType = ActivityRecognitionUtils.mapFromGmsActivityType(type),
                confidence = conf
            )
        }

        // 最可能的活动
        val mostProbable = ActivityRecognitionUtils.getMostProbableActivity(mapped)
        assertNotNull(mostProbable)
        assertEquals(DetectedActivityType.WALKING, mostProbable!!.activityType)
        assertEquals(60, mostProbable.confidence)

        // 可靠活动
        val reliable = ActivityRecognitionUtils.filterReliableActivities(mapped)
        assertEquals(1, reliable.size)
        assertEquals(DetectedActivityType.WALKING, reliable[0].activityType)

        // 是否自动开始追踪
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(mostProbable))

        // 映射为运动类型
        assertEquals("walking", ActivityRecognitionUtils.mapToExerciseType(mostProbable.activityType))
    }

    @Test
    fun `test comprehensive - typical running detection flow`() {
        val gmsResults = listOf(
            Pair(8, 75),  // RUNNING confidence 75
            Pair(7, 15),  // WALKING confidence 15
            Pair(3, 10)   // STILL confidence 10
        )

        val mapped = gmsResults.map { (type, conf) ->
            ActivityRecognitionResult(
                activityType = ActivityRecognitionUtils.mapFromGmsActivityType(type),
                confidence = conf
            )
        }

        val mostProbable = ActivityRecognitionUtils.getMostProbableActivity(mapped)
        assertNotNull(mostProbable)
        assertEquals(DetectedActivityType.RUNNING, mostProbable!!.activityType)
        assertTrue(ActivityRecognitionUtils.isHighConfidence(mostProbable.confidence))
        assertTrue(ActivityRecognitionUtils.shouldAutoStartTracking(mostProbable))
        assertEquals("running", ActivityRecognitionUtils.mapToExerciseType(mostProbable.activityType))
    }

    @Test
    fun `test comprehensive - stationary user should not trigger tracking`() {
        val gmsResults = listOf(
            Pair(3, 90),  // STILL confidence 90
            Pair(5, 5),   // TILTING confidence 5
            Pair(4, 5)    // UNKNOWN confidence 5
        )

        val mapped = gmsResults.map { (type, conf) ->
            ActivityRecognitionResult(
                activityType = ActivityRecognitionUtils.mapFromGmsActivityType(type),
                confidence = conf
            )
        }

        val mostProbable = ActivityRecognitionUtils.getMostProbableActivity(mapped)
        assertNotNull(mostProbable)
        assertEquals(DetectedActivityType.STILL, mostProbable!!.activityType)
        assertFalse(ActivityRecognitionUtils.shouldAutoStartTracking(mostProbable))
        assertFalse(ActivityRecognitionUtils.isActiveExercise(mostProbable.activityType))
    }

    @Test
    fun `test comprehensive - activity transition detection`() {
        // 模拟从静止到步行的状态转换
        val result1 = ActivityRecognitionResult(DetectedActivityType.STILL, 90, 1000L)
        val result2 = ActivityRecognitionResult(DetectedActivityType.WALKING, 70, 5000L)
        val result3 = ActivityRecognitionResult(DetectedActivityType.WALKING, 85, 8000L)
        val result4 = ActivityRecognitionResult(DetectedActivityType.RUNNING, 60, 12000L)

        // 第一次检测：从null到STILL
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(null, result1))
        // 第二次：从STILL到WALKING
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(result1, result2))
        // 第三次：WALKING不变
        assertFalse(ActivityRecognitionUtils.hasActivityChanged(result2, result3))
        // 第四次：WALKING到RUNNING
        assertTrue(ActivityRecognitionUtils.hasActivityChanged(result3, result4))
    }

    @Test
    fun `test comprehensive - calorie comparison across exercise types`() {
        val durationMinutes = 30.0
        val weightKg = 70.0

        val walkingCal = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.WALKING, durationMinutes, weightKg
        )
        val cyclingCal = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.CYCLING, durationMinutes, weightKg
        )
        val runningCal = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.RUNNING, durationMinutes, weightKg
        )
        val stillCal = ActivityRecognitionUtils.estimateCaloriesFromActivity(
            DetectedActivityType.STILL, durationMinutes, weightKg
        )

        // 热量消耗排序：跑步 > 骑行 > 步行 > 静止
        assertTrue(runningCal > cyclingCal)
        assertTrue(cyclingCal > walkingCal)
        assertTrue(walkingCal > stillCal)
        assertTrue(stillCal > 0)
    }

    @Test
    fun `test comprehensive - large history filtering`() {
        // 模拟100个检测结果，随机置信度
        val results = (0 until 100).map { i ->
            ActivityRecognitionResult(
                activityType = if (i % 3 == 0) DetectedActivityType.WALKING
                               else if (i % 3 == 1) DetectedActivityType.STILL
                               else DetectedActivityType.RUNNING,
                confidence = (i * 7) % 101, // 分散的置信度值
                timestamp = 1000L + i * 3000L
            )
        }

        // 过滤出可靠结果
        val reliable = ActivityRecognitionUtils.filterReliableActivities(results)
        assertTrue(reliable.all { it.confidence >= 50 })

        // 获取最可能的活动
        val mostProbable = ActivityRecognitionUtils.getMostProbableActivity(results)
        assertNotNull(mostProbable)
        assertTrue(mostProbable!!.confidence >= 0)
    }

    @Test
    fun `test comprehensive - GMS roundtrip mapping consistency`() {
        // 确保所有已知GMS类型映射后可以得到有效的exerciseType
        val gmsTypes = listOf(0, 1, 2, 3, 4, 5, 7, 8)
        gmsTypes.forEach { gmsType ->
            val activityType = ActivityRecognitionUtils.mapFromGmsActivityType(gmsType)
            val exerciseType = ActivityRecognitionUtils.mapToExerciseType(activityType)
            assertTrue(
                "GMS type $gmsType -> ${activityType.name} -> $exerciseType should be non-empty",
                exerciseType.isNotEmpty()
            )
        }
    }

    @Test
    fun `test comprehensive - METs consistency with ExerciseTrackingUtils`() {
        // 验证Activity Recognition的METs值与ExerciseTrackingUtils一致
        val walkingMets = ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.WALKING)
        val runningMets = ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.RUNNING)
        val cyclingMets = ActivityRecognitionUtils.getEstimatedMets(DetectedActivityType.CYCLING)

        // 与 ExerciseTrackingUtils.estimateCalories 中的METs值对齐
        assertEquals(3.5, walkingMets, 0.01)
        assertEquals(8.0, runningMets, 0.01)
        assertEquals(6.0, cyclingMets, 0.01)
    }
}
