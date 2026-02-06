package com.example.lifehub

import com.example.lifehub.data.ExerciseTrackingData
import com.example.lifehub.data.ExerciseTrackingState
import com.example.lifehub.data.ExerciseTrackingUtils
import com.example.lifehub.data.TrackPoint
import org.junit.Assert.*
import org.junit.Test

/**
 * Phase 27: 运动轨迹记录功能单元测试
 *
 * 测试范围：
 * 1. 数据模型正确性（TrackPoint, ExerciseTrackingData）
 * 2. 距离计算（Haversine公式）
 * 3. 配速计算
 * 4. 速度计算
 * 5. 热量估算
 * 6. 格式化工具（时长、配速、距离）
 * 7. 状态机管理
 * 8. 边界条件
 */
class Phase27ExerciseTrackingTest {

    // ==================== 数据模型测试 ====================

    @Test
    fun `test TrackPoint creation with defaults`() {
        val point = TrackPoint(latitude = 39.9042, longitude = 116.4074)
        assertEquals(39.9042, point.latitude, 0.0001)
        assertEquals(116.4074, point.longitude, 0.0001)
        assertEquals(0.0, point.altitude, 0.01)
        assertEquals(0f, point.speed, 0.01f)
        assertTrue(point.timestamp > 0)
    }

    @Test
    fun `test TrackPoint creation with all fields`() {
        val ts = 1700000000000L
        val point = TrackPoint(
            latitude = 31.2304,
            longitude = 121.4737,
            altitude = 15.5,
            speed = 2.5f,
            timestamp = ts
        )
        assertEquals(31.2304, point.latitude, 0.0001)
        assertEquals(121.4737, point.longitude, 0.0001)
        assertEquals(15.5, point.altitude, 0.01)
        assertEquals(2.5f, point.speed, 0.01f)
        assertEquals(ts, point.timestamp)
    }

    @Test
    fun `test ExerciseTrackingData default values`() {
        val data = ExerciseTrackingData()
        assertTrue(data.trackPoints.isEmpty())
        assertEquals(0.0, data.totalDistance, 0.01)
        assertEquals(0L, data.elapsedTime)
        assertEquals(0.0, data.currentPace, 0.01)
        assertEquals(0.0, data.averagePace, 0.01)
        assertEquals(0.0, data.currentSpeed, 0.01)
        assertEquals(0.0, data.caloriesBurned, 0.01)
        assertNull(data.planId)
        assertEquals("walking", data.exerciseType)
    }

    @Test
    fun `test ExerciseTrackingData with values`() {
        val points = listOf(
            TrackPoint(39.9042, 116.4074),
            TrackPoint(39.9052, 116.4084)
        )
        val data = ExerciseTrackingData(
            trackPoints = points,
            totalDistance = 150.0,
            elapsedTime = 120000L,
            currentPace = 5.5,
            averagePace = 5.8,
            currentSpeed = 10.9,
            caloriesBurned = 45.0,
            planId = 42,
            exerciseType = "running"
        )
        assertEquals(2, data.trackPoints.size)
        assertEquals(150.0, data.totalDistance, 0.01)
        assertEquals(120000L, data.elapsedTime)
        assertEquals(5.5, data.currentPace, 0.01)
        assertEquals(42, data.planId)
        assertEquals("running", data.exerciseType)
    }

    // ==================== 状态机测试 ====================

    @Test
    fun `test ExerciseTrackingState Idle`() {
        val state = ExerciseTrackingState.Idle
        assertTrue(state is ExerciseTrackingState.Idle)
    }

    @Test
    fun `test ExerciseTrackingState Tracking`() {
        val state = ExerciseTrackingState.Tracking
        assertTrue(state is ExerciseTrackingState.Tracking)
    }

    @Test
    fun `test ExerciseTrackingState Paused`() {
        val state = ExerciseTrackingState.Paused
        assertTrue(state is ExerciseTrackingState.Paused)
    }

    @Test
    fun `test ExerciseTrackingState Completed`() {
        val points = listOf(TrackPoint(39.9042, 116.4074))
        val state = ExerciseTrackingState.Completed(
            totalDistance = 5000.0,
            totalDuration = 1800000L,
            averagePace = 6.0,
            trackPoints = points
        )
        assertTrue(state is ExerciseTrackingState.Completed)
        assertEquals(5000.0, state.totalDistance, 0.01)
        assertEquals(1800000L, state.totalDuration)
        assertEquals(6.0, state.averagePace, 0.01)
        assertEquals(1, state.trackPoints.size)
    }

    @Test
    fun `test state transitions Idle to Tracking`() {
        var state: ExerciseTrackingState = ExerciseTrackingState.Idle
        // 点击开始
        state = ExerciseTrackingState.Tracking
        assertTrue(state is ExerciseTrackingState.Tracking)
    }

    @Test
    fun `test state transitions Tracking to Paused`() {
        var state: ExerciseTrackingState = ExerciseTrackingState.Tracking
        state = ExerciseTrackingState.Paused
        assertTrue(state is ExerciseTrackingState.Paused)
    }

    @Test
    fun `test state transitions Paused to Tracking (resume)`() {
        var state: ExerciseTrackingState = ExerciseTrackingState.Paused
        state = ExerciseTrackingState.Tracking
        assertTrue(state is ExerciseTrackingState.Tracking)
    }

    @Test
    fun `test state transitions Tracking to Completed`() {
        var state: ExerciseTrackingState = ExerciseTrackingState.Tracking
        state = ExerciseTrackingState.Completed(
            totalDistance = 3000.0,
            totalDuration = 1200000L,
            averagePace = 6.67,
            trackPoints = emptyList()
        )
        assertTrue(state is ExerciseTrackingState.Completed)
    }

    // ==================== 距离计算测试 ====================

    @Test
    fun `test calculateDistance same point returns zero`() {
        val distance = ExerciseTrackingUtils.calculateDistance(
            39.9042, 116.4074,
            39.9042, 116.4074
        )
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `test calculateDistance known distance Beijing to Tianjin approx 110km`() {
        // 北京(39.9042, 116.4074) 到 天津(39.0842, 117.2010) 约110-120公里
        val distance = ExerciseTrackingUtils.calculateDistance(
            39.9042, 116.4074,
            39.0842, 117.2010
        )
        assertTrue("Distance should be ~110-120km, got ${distance / 1000} km",
            distance > 100000 && distance < 130000)
    }

    @Test
    fun `test calculateDistance short walk about 100m`() {
        // 约100米的短距离
        val distance = ExerciseTrackingUtils.calculateDistance(
            39.9042, 116.4074,
            39.9051, 116.4074
        )
        assertTrue("Distance should be ~100m, got $distance m",
            distance > 80 && distance < 120)
    }

    @Test
    fun `test calculateDistance equator points`() {
        // 赤道上经度差1度约111km
        val distance = ExerciseTrackingUtils.calculateDistance(
            0.0, 0.0,
            0.0, 1.0
        )
        assertTrue("Distance should be ~111km, got ${distance / 1000} km",
            distance > 110000 && distance < 112000)
    }

    @Test
    fun `test calculateDistance negative coordinates`() {
        // 南半球坐标
        val distance = ExerciseTrackingUtils.calculateDistance(
            -33.8688, 151.2093,  // 悉尼
            -37.8136, 144.9631   // 墨尔本
        )
        assertTrue("Distance should be ~710-720km, got ${distance / 1000} km",
            distance > 700000 && distance < 730000)
    }

    @Test
    fun `test calculateTotalDistance empty list`() {
        val distance = ExerciseTrackingUtils.calculateTotalDistance(emptyList())
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `test calculateTotalDistance single point`() {
        val points = listOf(TrackPoint(39.9042, 116.4074))
        val distance = ExerciseTrackingUtils.calculateTotalDistance(points)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `test calculateTotalDistance multiple points`() {
        val points = listOf(
            TrackPoint(39.9042, 116.4074),
            TrackPoint(39.9052, 116.4074),  // 北移约100m
            TrackPoint(39.9052, 116.4084)   // 东移约80m
        )
        val distance = ExerciseTrackingUtils.calculateTotalDistance(points)
        assertTrue("Total distance should be > 150m, got $distance m", distance > 150)
        assertTrue("Total distance should be < 250m, got $distance m", distance < 250)
    }

    @Test
    fun `test calculateTotalDistance round trip returns double distance`() {
        val p1 = TrackPoint(39.9042, 116.4074)
        val p2 = TrackPoint(39.9052, 116.4074)
        val oneWay = ExerciseTrackingUtils.calculateDistance(
            p1.latitude, p1.longitude, p2.latitude, p2.longitude
        )
        val roundTrip = ExerciseTrackingUtils.calculateTotalDistance(listOf(p1, p2, p1))
        assertEquals(oneWay * 2, roundTrip, 1.0)
    }

    // ==================== 配速计算测试 ====================

    @Test
    fun `test calculatePace zero distance returns zero`() {
        val pace = ExerciseTrackingUtils.calculatePace(0.0, 60000)
        assertEquals(0.0, pace, 0.01)
    }

    @Test
    fun `test calculatePace zero duration returns zero`() {
        val pace = ExerciseTrackingUtils.calculatePace(1000.0, 0)
        assertEquals(0.0, pace, 0.01)
    }

    @Test
    fun `test calculatePace negative distance returns zero`() {
        val pace = ExerciseTrackingUtils.calculatePace(-100.0, 60000)
        assertEquals(0.0, pace, 0.01)
    }

    @Test
    fun `test calculatePace 1km in 5 minutes`() {
        // 1公里5分钟 -> 配速5分/公里
        val pace = ExerciseTrackingUtils.calculatePace(1000.0, 5 * 60 * 1000L)
        assertEquals(5.0, pace, 0.01)
    }

    @Test
    fun `test calculatePace 5km in 30 minutes`() {
        // 5公里30分钟 -> 配速6分/公里
        val pace = ExerciseTrackingUtils.calculatePace(5000.0, 30 * 60 * 1000L)
        assertEquals(6.0, pace, 0.01)
    }

    @Test
    fun `test calculatePace walking pace about 10 min per km`() {
        // 步行：1公里10分钟
        val pace = ExerciseTrackingUtils.calculatePace(1000.0, 10 * 60 * 1000L)
        assertEquals(10.0, pace, 0.01)
    }

    // ==================== 速度计算测试 ====================

    @Test
    fun `test calculateSpeed zero distance returns zero`() {
        val speed = ExerciseTrackingUtils.calculateSpeed(0.0, 60000)
        assertEquals(0.0, speed, 0.01)
    }

    @Test
    fun `test calculateSpeed zero duration returns zero`() {
        val speed = ExerciseTrackingUtils.calculateSpeed(1000.0, 0)
        assertEquals(0.0, speed, 0.01)
    }

    @Test
    fun `test calculateSpeed 1km in 1 hour equals 1 kmh`() {
        val speed = ExerciseTrackingUtils.calculateSpeed(1000.0, 3600000)
        assertEquals(1.0, speed, 0.01)
    }

    @Test
    fun `test calculateSpeed 10km in 1 hour equals 10 kmh`() {
        val speed = ExerciseTrackingUtils.calculateSpeed(10000.0, 3600000)
        assertEquals(10.0, speed, 0.01)
    }

    @Test
    fun `test calculateSpeed walking about 5 kmh`() {
        // 步行：5公里/小时 = 5000m/3600000ms
        val speed = ExerciseTrackingUtils.calculateSpeed(5000.0, 3600000)
        assertEquals(5.0, speed, 0.01)
    }

    // ==================== 热量估算测试 ====================

    @Test
    fun `test estimateCalories walking 30 min 70kg`() {
        // 散步METs=3.5, 70kg, 30分钟
        // 3.5 * 70 * 0.5 = 122.5 kcal
        val cal = ExerciseTrackingUtils.estimateCalories("walking", 30.0, 70.0)
        assertEquals(122.5, cal, 0.1)
    }

    @Test
    fun `test estimateCalories running 30 min 70kg`() {
        // 跑步METs=8.0, 70kg, 30分钟
        // 8.0 * 70 * 0.5 = 280 kcal
        val cal = ExerciseTrackingUtils.estimateCalories("running", 30.0, 70.0)
        assertEquals(280.0, cal, 0.1)
    }

    @Test
    fun `test estimateCalories cycling 60 min 80kg`() {
        // 骑行METs=6.0, 80kg, 60分钟
        // 6.0 * 80 * 1.0 = 480 kcal
        val cal = ExerciseTrackingUtils.estimateCalories("cycling", 60.0, 80.0)
        assertEquals(480.0, cal, 0.1)
    }

    @Test
    fun `test estimateCalories zero duration`() {
        val cal = ExerciseTrackingUtils.estimateCalories("walking", 0.0, 70.0)
        assertEquals(0.0, cal, 0.01)
    }

    @Test
    fun `test estimateCalories default weight`() {
        // 使用默认体重70kg
        val cal = ExerciseTrackingUtils.estimateCalories("walking", 30.0)
        assertEquals(122.5, cal, 0.1)
    }

    @Test
    fun `test estimateCalories unknown exercise type uses default METs`() {
        // 未知运动类型默认METs=4.0
        val cal = ExerciseTrackingUtils.estimateCalories("unknown_exercise", 60.0, 70.0)
        assertEquals(280.0, cal, 0.1) // 4.0 * 70 * 1.0
    }

    @Test
    fun `test estimateCalories hiking`() {
        // 徒步METs=5.5, 70kg, 60分钟
        // 5.5 * 70 * 1.0 = 385 kcal
        val cal = ExerciseTrackingUtils.estimateCalories("hiking", 60.0, 70.0)
        assertEquals(385.0, cal, 0.1)
    }

    // ==================== 格式化测试 ====================

    @Test
    fun `test formatDuration zero`() {
        assertEquals("00:00", ExerciseTrackingUtils.formatDuration(0))
    }

    @Test
    fun `test formatDuration 30 seconds`() {
        assertEquals("00:30", ExerciseTrackingUtils.formatDuration(30000))
    }

    @Test
    fun `test formatDuration 5 minutes`() {
        assertEquals("05:00", ExerciseTrackingUtils.formatDuration(5 * 60 * 1000L))
    }

    @Test
    fun `test formatDuration 1 hour 23 min 45 sec`() {
        val millis = (1 * 3600 + 23 * 60 + 45) * 1000L
        assertEquals("1:23:45", ExerciseTrackingUtils.formatDuration(millis))
    }

    @Test
    fun `test formatDuration 59 minutes 59 seconds no hours`() {
        val millis = (59 * 60 + 59) * 1000L
        assertEquals("59:59", ExerciseTrackingUtils.formatDuration(millis))
    }

    @Test
    fun `test formatDuration exactly 1 hour`() {
        assertEquals("1:00:00", ExerciseTrackingUtils.formatDuration(3600000))
    }

    @Test
    fun `test formatPace normal pace`() {
        assertEquals("5'30\"", ExerciseTrackingUtils.formatPace(5.5))
    }

    @Test
    fun `test formatPace zero returns placeholder`() {
        assertEquals("--'--\"", ExerciseTrackingUtils.formatPace(0.0))
    }

    @Test
    fun `test formatPace negative returns placeholder`() {
        assertEquals("--'--\"", ExerciseTrackingUtils.formatPace(-1.0))
    }

    @Test
    fun `test formatPace very slow returns placeholder`() {
        assertEquals("--'--\"", ExerciseTrackingUtils.formatPace(61.0))
    }

    @Test
    fun `test formatPace exact minute`() {
        assertEquals("6'00\"", ExerciseTrackingUtils.formatPace(6.0))
    }

    @Test
    fun `test formatDistance meters`() {
        assertEquals("500 m", ExerciseTrackingUtils.formatDistance(500.0))
    }

    @Test
    fun `test formatDistance kilometers`() {
        assertEquals("5.00 km", ExerciseTrackingUtils.formatDistance(5000.0))
    }

    @Test
    fun `test formatDistance zero`() {
        assertEquals("0 m", ExerciseTrackingUtils.formatDistance(0.0))
    }

    @Test
    fun `test formatDistance boundary 999m`() {
        assertEquals("999 m", ExerciseTrackingUtils.formatDistance(999.0))
    }

    @Test
    fun `test formatDistance boundary 1000m`() {
        assertEquals("1.00 km", ExerciseTrackingUtils.formatDistance(1000.0))
    }

    @Test
    fun `test formatDistance 10500m`() {
        assertEquals("10.50 km", ExerciseTrackingUtils.formatDistance(10500.0))
    }

    // ==================== 轨迹数据处理测试 ====================

    @Test
    fun `test track points ordering by timestamp`() {
        val points = listOf(
            TrackPoint(39.9042, 116.4074, timestamp = 1000L),
            TrackPoint(39.9052, 116.4074, timestamp = 2000L),
            TrackPoint(39.9062, 116.4074, timestamp = 3000L)
        )
        val sorted = points.sortedBy { it.timestamp }
        assertEquals(1000L, sorted[0].timestamp)
        assertEquals(2000L, sorted[1].timestamp)
        assertEquals(3000L, sorted[2].timestamp)
    }

    @Test
    fun `test filtering stationary points`() {
        // 过滤掉速度为0的静止点
        val points = listOf(
            TrackPoint(39.9042, 116.4074, speed = 0f),
            TrackPoint(39.9042, 116.4074, speed = 2.5f),
            TrackPoint(39.9042, 116.4074, speed = 0f),
            TrackPoint(39.9042, 116.4074, speed = 3.0f)
        )
        val moving = points.filter { it.speed > 0f }
        assertEquals(2, moving.size)
    }

    @Test
    fun `test track data copy with new distance`() {
        val data = ExerciseTrackingData(totalDistance = 100.0)
        val updated = data.copy(totalDistance = 200.0)
        assertEquals(200.0, updated.totalDistance, 0.01)
        assertEquals(100.0, data.totalDistance, 0.01) // 原数据不变
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `test very short distance between close points`() {
        // 两个非常接近的点（约1米）
        val distance = ExerciseTrackingUtils.calculateDistance(
            39.904200, 116.407400,
            39.904209, 116.407400
        )
        assertTrue("Very short distance: $distance m", distance > 0 && distance < 5)
    }

    @Test
    fun `test distance calculation symmetry`() {
        // 验证A->B距离等于B->A距离
        val distAB = ExerciseTrackingUtils.calculateDistance(
            39.9042, 116.4074,
            31.2304, 121.4737
        )
        val distBA = ExerciseTrackingUtils.calculateDistance(
            31.2304, 121.4737,
            39.9042, 116.4074
        )
        assertEquals(distAB, distBA, 0.01)
    }

    @Test
    fun `test large number of track points performance`() {
        // 模拟30分钟运动，每秒记录一个点（1800个点）
        val points = (0 until 1800).map { i ->
            TrackPoint(
                latitude = 39.9042 + i * 0.00001,
                longitude = 116.4074,
                timestamp = i * 1000L
            )
        }
        // 验证不会崩溃，且距离合理
        val distance = ExerciseTrackingUtils.calculateTotalDistance(points)
        assertTrue("Distance should be positive, got $distance", distance > 0)
    }

    @Test
    fun `test pace and speed are inverse related`() {
        // 配速和速度应该是反相关的
        val distance = 5000.0 // 5km
        val duration = 30 * 60 * 1000L // 30分钟
        val pace = ExerciseTrackingUtils.calculatePace(distance, duration)
        val speed = ExerciseTrackingUtils.calculateSpeed(distance, duration)
        // pace(min/km) * speed(km/h) ≈ 60
        val product = pace * speed
        assertEquals(60.0, product, 0.1)
    }

    @Test
    fun `test calories increase with duration`() {
        val cal30 = ExerciseTrackingUtils.estimateCalories("running", 30.0, 70.0)
        val cal60 = ExerciseTrackingUtils.estimateCalories("running", 60.0, 70.0)
        assertEquals(cal30 * 2, cal60, 0.01)
    }

    @Test
    fun `test calories increase with weight`() {
        val cal60 = ExerciseTrackingUtils.estimateCalories("running", 30.0, 60.0)
        val cal80 = ExerciseTrackingUtils.estimateCalories("running", 30.0, 80.0)
        assertTrue(cal80 > cal60)
    }
}
