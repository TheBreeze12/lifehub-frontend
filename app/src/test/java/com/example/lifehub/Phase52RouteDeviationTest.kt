package com.example.lifehub

import com.example.lifehub.data.TrackPoint
import com.example.lifehub.utils.RouteDeviationDetector
import com.example.lifehub.utils.RouteDeviationDetector.DeviationResult
import com.example.lifehub.utils.RouteDeviationDetector.DeviationTracker
import com.example.lifehub.utils.RouteDeviationDetector.LatLng
import com.example.lifehub.utils.RouteDeviationState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Phase 52: 路线偏离检测与偏航纠偏 - 全面单元测试
 *
 * 测试范围：
 * 1. Haversine距离计算正确性
 * 2. 点到线段距离计算（投影在线段内/外）
 * 3. 路线最近点查找
 * 4. 偏离检测（单次检测）
 * 5. DeviationTracker状态追踪（连续偏离计数）
 * 6. 边界条件（空路线、单点路线、极端坐标）
 * 7. GPS抖动过滤（连续计数机制）
 * 8. 重置功能
 * 9. 格式化输出
 * 10. RouteDeviationState状态模型
 * 11. 坐标转换辅助函数
 * 12. 实际场景模拟
 */
class Phase52RouteDeviationTest {

    // ==================== 1. Haversine距离计算 ====================

    @Test
    fun `haversine - same point returns zero distance`() {
        val p = LatLng(39.9042, 116.4074)
        val dist = RouteDeviationDetector.haversineDistance(p, p)
        assertEquals(0.0, dist, 0.01)
    }

    @Test
    fun `haversine - known distance Beijing to Tianjin`() {
        // 北京天安门 -> 天津站 约120km
        val beijing = LatLng(39.9042, 116.4074)
        val tianjin = LatLng(39.1422, 117.1767)
        val dist = RouteDeviationDetector.haversineDistance(beijing, tianjin)
        // 实际约105km，允许合理误差
        assertTrue("距离应在90-130km之间，实际: ${dist / 1000}km", dist in 90000.0..130000.0)
    }

    @Test
    fun `haversine - short distance about 100m`() {
        // 约100米的两点
        val p1 = LatLng(39.9042, 116.4074)
        val p2 = LatLng(39.9051, 116.4074) // 纬度差约0.0009度 ≈ 100m
        val dist = RouteDeviationDetector.haversineDistance(p1, p2)
        assertTrue("100m左右距离，实际: ${dist}m", dist in 80.0..120.0)
    }

    @Test
    fun `haversine - equator points`() {
        val p1 = LatLng(0.0, 0.0)
        val p2 = LatLng(0.0, 1.0) // 赤道上1度经度 ≈ 111km
        val dist = RouteDeviationDetector.haversineDistance(p1, p2)
        assertTrue("赤道1度约111km，实际: ${dist / 1000}km", dist in 110000.0..112000.0)
    }

    @Test
    fun `haversine - very close points under 1m`() {
        val p1 = LatLng(39.9042, 116.4074)
        val p2 = LatLng(39.90421, 116.40741)
        val dist = RouteDeviationDetector.haversineDistance(p1, p2)
        assertTrue("极近距离应 < 5m，实际: ${dist}m", dist < 5.0)
    }

    @Test
    fun `haversine - antipodal points`() {
        // 对跖点：地球两端
        val p1 = LatLng(0.0, 0.0)
        val p2 = LatLng(0.0, 180.0)
        val dist = RouteDeviationDetector.haversineDistance(p1, p2)
        // 半个地球周长 ≈ 20015km
        assertTrue("对跖点约20015km，实际: ${dist / 1000}km", dist in 19000000.0..21000000.0)
    }

    @Test
    fun `haversine - negative coordinates`() {
        val p1 = LatLng(-33.8688, 151.2093) // 悉尼
        val p2 = LatLng(-37.8136, 144.9631) // 墨尔本
        val dist = RouteDeviationDetector.haversineDistance(p1, p2)
        // 悉尼到墨尔本约714km
        assertTrue("约714km，实际: ${dist / 1000}km", dist in 600000.0..800000.0)
    }

    // ==================== 2. 点到线段距离计算 ====================

    @Test
    fun `pointToSegment - point projects onto segment middle`() {
        // 线段水平，点在正上方
        val segStart = LatLng(39.9000, 116.4000)
        val segEnd = LatLng(39.9000, 116.4020)
        val point = LatLng(39.9005, 116.4010) // 线段中点正上方约55m

        val (dist, closest) = RouteDeviationDetector.pointToSegmentDistance(point, segStart, segEnd)

        assertTrue("投影到中间时距离应合理，实际: ${dist}m", dist in 30.0..80.0)
        // 最近点的经度应接近目标点经度（因为投影到中间）
        assertEquals(116.4010, closest.longitude, 0.001)
    }

    @Test
    fun `pointToSegment - point projects before segment start`() {
        val segStart = LatLng(39.9000, 116.4010)
        val segEnd = LatLng(39.9000, 116.4020)
        val point = LatLng(39.9000, 116.4000) // 在线段起点之前

        val (dist, closest) = RouteDeviationDetector.pointToSegmentDistance(point, segStart, segEnd)

        // 最近点应该是线段起点
        assertEquals(segStart.latitude, closest.latitude, 0.0001)
        assertEquals(segStart.longitude, closest.longitude, 0.0001)
    }

    @Test
    fun `pointToSegment - point projects after segment end`() {
        val segStart = LatLng(39.9000, 116.4000)
        val segEnd = LatLng(39.9000, 116.4010)
        val point = LatLng(39.9000, 116.4020) // 在线段终点之后

        val (dist, closest) = RouteDeviationDetector.pointToSegmentDistance(point, segStart, segEnd)

        // 最近点应该是线段终点
        assertEquals(segEnd.latitude, closest.latitude, 0.0001)
        assertEquals(segEnd.longitude, closest.longitude, 0.0001)
    }

    @Test
    fun `pointToSegment - point on segment returns near zero`() {
        val segStart = LatLng(39.9000, 116.4000)
        val segEnd = LatLng(39.9000, 116.4020)
        val point = LatLng(39.9000, 116.4010) // 恰好在线段上

        val (dist, _) = RouteDeviationDetector.pointToSegmentDistance(point, segStart, segEnd)

        assertTrue("点在线段上距离应接近0，实际: ${dist}m", dist < 5.0)
    }

    @Test
    fun `pointToSegment - degenerate segment (single point)`() {
        val segStart = LatLng(39.9000, 116.4000)
        val segEnd = LatLng(39.9000, 116.4000) // 退化为点
        val point = LatLng(39.9005, 116.4000)

        val (dist, closest) = RouteDeviationDetector.pointToSegmentDistance(point, segStart, segEnd)

        // 应返回到该点的距离
        assertTrue("退化线段距离应合理，实际: ${dist}m", dist > 0)
        assertEquals(segStart.latitude, closest.latitude, 0.0001)
    }

    @Test
    fun `pointToSegment - diagonal segment`() {
        // 对角线线段
        val segStart = LatLng(39.9000, 116.4000)
        val segEnd = LatLng(39.9010, 116.4010)
        val point = LatLng(39.9010, 116.4000) // 在对角线旁边

        val (dist, _) = RouteDeviationDetector.pointToSegmentDistance(point, segStart, segEnd)
        assertTrue("对角线段偏离距离应 > 0，实际: ${dist}m", dist > 0)
    }

    // ==================== 3. 路线最近点查找 ====================

    @Test
    fun `findNearest - empty route returns MAX_VALUE`() {
        val point = LatLng(39.9042, 116.4074)
        val (dist, _, index) = RouteDeviationDetector.findNearestPointOnRoute(point, emptyList())
        assertEquals(Double.MAX_VALUE, dist, 0.0)
        assertEquals(-1, index)
    }

    @Test
    fun `findNearest - single point route`() {
        val point = LatLng(39.9042, 116.4074)
        val route = listOf(LatLng(39.9042, 116.4074))
        val (dist, nearest, index) = RouteDeviationDetector.findNearestPointOnRoute(point, route)
        assertTrue("同一点距离应接近0，实际: ${dist}m", dist < 1.0)
        assertEquals(0, index)
    }

    @Test
    fun `findNearest - point on first segment`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000),
            LatLng(39.9020, 116.4000)
        )
        val point = LatLng(39.9005, 116.4001) // 接近第一段

        val (_, _, index) = RouteDeviationDetector.findNearestPointOnRoute(point, route)
        assertEquals("应找到第一段", 0, index)
    }

    @Test
    fun `findNearest - point on last segment`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000),
            LatLng(39.9020, 116.4000)
        )
        val point = LatLng(39.9015, 116.4001) // 接近第二段

        val (_, _, index) = RouteDeviationDetector.findNearestPointOnRoute(point, route)
        assertEquals("应找到第二段", 1, index)
    }

    @Test
    fun `findNearest - multi-segment route finds global minimum`() {
        // L形路线
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000),
            LatLng(39.9010, 116.4010)
        )
        val point = LatLng(39.9010, 116.4005) // 接近L形拐角后的第二段

        val (dist, _, index) = RouteDeviationDetector.findNearestPointOnRoute(point, route)
        assertEquals("应找到第二段（拐角后）", 1, index)
        assertTrue("距离应较小，实际: ${dist}m", dist < 10.0)
    }

    // ==================== 4. 偏离检测（单次） ====================

    @Test
    fun `checkDeviation - on route returns not deviated`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000),
            LatLng(39.9020, 116.4000)
        )
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4000, route
        )

        assertFalse("在路线上不应偏离", result.isDeviated)
        assertTrue("偏离距离应很小，实际: ${result.deviationDistance}m", result.deviationDistance < 10.0)
    }

    @Test
    fun `checkDeviation - 30m off route not deviated with default threshold`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000)
        )
        // 向东偏移约30m（0.0003经度 ≈ 30m）
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4003, route
        )

        assertFalse("30m偏离不应触发默认50m阈值", result.isDeviated)
    }

    @Test
    fun `checkDeviation - 80m off route deviated with default threshold`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000)
        )
        // 向东偏移约80m（0.001经度 ≈ 86m at lat 39.9）
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4010, route
        )

        assertTrue("80m偏离应触发默认50m阈值", result.isDeviated)
        assertTrue("偏离距离应 > 50m，实际: ${result.deviationDistance}m", result.deviationDistance > 50.0)
    }

    @Test
    fun `checkDeviation - custom threshold 100m`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000)
        )
        // 向东偏移约80m
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4010, route, thresholdMeters = 100.0
        )

        assertFalse("80m偏离不应触发100m阈值", result.isDeviated)
    }

    @Test
    fun `checkDeviation - insufficient route points`() {
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4010, listOf(LatLng(39.9000, 116.4000))
        )
        assertFalse("单点路线不应判定偏离", result.isDeviated)
        assertEquals(0.0, result.deviationDistance, 0.01)
    }

    @Test
    fun `checkDeviation - empty route`() {
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4010, emptyList()
        )
        assertFalse("空路线不应判定偏离", result.isDeviated)
    }

    @Test
    fun `checkDeviation - returns nearest segment index`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000),
            LatLng(39.9020, 116.4010)
        )
        val result = RouteDeviationDetector.checkDeviation(
            39.9015, 116.4008, route
        )
        assertTrue("线段索引应有效", result.nearestSegmentIndex >= 0)
    }

    @Test
    fun `checkDeviation - returns nearest point on route`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4000)
        )
        val result = RouteDeviationDetector.checkDeviation(
            39.9005, 116.4010, route
        )
        assertNotNull("应返回最近点", result.nearestPointOnRoute)
        // 最近点的纬度应接近目标点纬度（因为投影到水平线段上）
        assertEquals(39.9005, result.nearestPointOnRoute!!.latitude, 0.001)
    }

    // ==================== 5. DeviationTracker状态追踪 ====================

    private lateinit var tracker: DeviationTracker
    private val testRoute = listOf(
        LatLng(39.9000, 116.4000),
        LatLng(39.9010, 116.4000),
        LatLng(39.9020, 116.4000)
    )

    @Before
    fun setupTracker() {
        tracker = DeviationTracker(
            thresholdMeters = 50.0,
            consecutiveThreshold = 3
        )
    }

    @Test
    fun `tracker - initial state`() {
        assertEquals(0, tracker.consecutiveCount)
        assertEquals(0.0, tracker.lastDeviationDistance, 0.01)
        assertFalse(tracker.isDeviated)
    }

    @Test
    fun `tracker - on route does not increment count`() {
        val location = TrackPoint(39.9005, 116.4000)
        val result = tracker.update(location, testRoute)

        assertFalse(result.isDeviated)
        assertEquals(0, tracker.consecutiveCount)
    }

    @Test
    fun `tracker - single deviation does not trigger alarm`() {
        // 偏离80m但只有1次
        val location = TrackPoint(39.9005, 116.4010)
        val result = tracker.update(location, testRoute)

        assertFalse("单次偏离不应触发", result.isDeviated)
        assertEquals(1, tracker.consecutiveCount)
    }

    @Test
    fun `tracker - two consecutive deviations not enough`() {
        val deviated = TrackPoint(39.9005, 116.4010) // 偏离约86m

        tracker.update(deviated, testRoute)
        val result = tracker.update(deviated, testRoute)

        assertFalse("2次连续偏离不应触发(阈值3)", result.isDeviated)
        assertEquals(2, tracker.consecutiveCount)
    }

    @Test
    fun `tracker - three consecutive deviations triggers alarm`() {
        val deviated = TrackPoint(39.9005, 116.4010)

        tracker.update(deviated, testRoute)
        tracker.update(deviated, testRoute)
        val result = tracker.update(deviated, testRoute)

        assertTrue("3次连续偏离应触发", result.isDeviated)
        assertEquals(3, tracker.consecutiveCount)
        assertTrue(tracker.isDeviated)
    }

    @Test
    fun `tracker - deviation resets when back on route`() {
        val deviated = TrackPoint(39.9005, 116.4010)
        val onRoute = TrackPoint(39.9005, 116.4000)

        tracker.update(deviated, testRoute)
        tracker.update(deviated, testRoute)
        // 回到路线上
        val result = tracker.update(onRoute, testRoute)

        assertFalse(result.isDeviated)
        assertEquals(0, tracker.consecutiveCount)
    }

    @Test
    fun `tracker - intermittent deviation does not accumulate`() {
        val deviated = TrackPoint(39.9005, 116.4010)
        val onRoute = TrackPoint(39.9005, 116.4000)

        // 偏离 -> 回来 -> 偏离 -> 回来
        tracker.update(deviated, testRoute)
        assertEquals(1, tracker.consecutiveCount)
        tracker.update(onRoute, testRoute)
        assertEquals(0, tracker.consecutiveCount)
        tracker.update(deviated, testRoute)
        assertEquals(1, tracker.consecutiveCount)
        tracker.update(onRoute, testRoute)
        assertEquals(0, tracker.consecutiveCount)
    }

    @Test
    fun `tracker - reset clears all state`() {
        val deviated = TrackPoint(39.9005, 116.4010)

        tracker.update(deviated, testRoute)
        tracker.update(deviated, testRoute)
        tracker.update(deviated, testRoute)
        assertTrue(tracker.isDeviated)

        tracker.reset()

        assertEquals(0, tracker.consecutiveCount)
        assertEquals(0.0, tracker.lastDeviationDistance, 0.01)
        assertFalse(tracker.isDeviated)
    }

    @Test
    fun `tracker - after reset deviation count restarts`() {
        val deviated = TrackPoint(39.9005, 116.4010)

        tracker.update(deviated, testRoute)
        tracker.update(deviated, testRoute)
        tracker.reset()
        tracker.update(deviated, testRoute)

        assertEquals(1, tracker.consecutiveCount)
        assertFalse(tracker.isDeviated)
    }

    @Test
    fun `tracker - custom threshold and consecutive count`() {
        val customTracker = DeviationTracker(
            thresholdMeters = 20.0,
            consecutiveThreshold = 2
        )

        // 偏移约30m (0.0003度经度 at lat39.9 ≈ 26m)
        val slightlyOff = TrackPoint(39.9005, 116.4003)

        customTracker.update(slightlyOff, testRoute)
        val result = customTracker.update(slightlyOff, testRoute)

        assertTrue("20m阈值+2次连续应触发", result.isDeviated)
    }

    @Test
    fun `tracker - insufficient route points returns safe`() {
        val location = TrackPoint(39.9005, 116.4010)
        val shortRoute = listOf(LatLng(39.9000, 116.4000))
        val result = tracker.update(location, shortRoute)

        assertFalse(result.isDeviated)
        assertEquals(0, result.consecutiveDeviationCount)
    }

    @Test
    fun `tracker - updates lastDeviationDistance`() {
        val location = TrackPoint(39.9005, 116.4010)
        tracker.update(location, testRoute)
        assertTrue("lastDeviationDistance应更新", tracker.lastDeviationDistance > 0)
    }

    @Test
    fun `tracker - continued deviation beyond threshold`() {
        val deviated = TrackPoint(39.9005, 116.4010)

        // 连续偏离5次
        repeat(5) { tracker.update(deviated, testRoute) }

        assertTrue(tracker.isDeviated)
        assertEquals(5, tracker.consecutiveCount)
    }

    // ==================== 6. 边界条件 ====================

    @Test
    fun `edge - zero latitude longitude`() {
        val route = listOf(LatLng(0.0, 0.0), LatLng(0.001, 0.0))
        val result = RouteDeviationDetector.checkDeviation(0.0005, 0.001, route)
        assertTrue("偏离距离应有正值", result.deviationDistance > 0)
    }

    @Test
    fun `edge - high latitude near pole`() {
        val route = listOf(LatLng(89.0, 0.0), LatLng(89.001, 0.0))
        val result = RouteDeviationDetector.checkDeviation(89.0005, 1.0, route)
        // 极地附近经度收敛，1度经度很短
        assertNotNull(result)
    }

    @Test
    fun `edge - negative coordinates (Southern hemisphere)`() {
        val route = listOf(LatLng(-33.86, 151.20), LatLng(-33.87, 151.21))
        val result = RouteDeviationDetector.checkDeviation(-33.865, 151.205, route)
        assertNotNull(result)
        assertFalse("路线上不应偏离", result.isDeviated)
    }

    @Test
    fun `edge - very long route many segments`() {
        // 100段路线
        val route = (0..100).map { i ->
            LatLng(39.9 + i * 0.0001, 116.4)
        }
        val result = RouteDeviationDetector.checkDeviation(39.905, 116.4, route)
        assertFalse("在路线上不应偏离", result.isDeviated)
    }

    @Test
    fun `edge - zigzag route`() {
        // Z字形路线
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9010, 116.4010),
            LatLng(39.9020, 116.4000),
            LatLng(39.9030, 116.4010)
        )
        // 在Z字中间位置
        val result = RouteDeviationDetector.checkDeviation(39.9015, 116.4005, route)
        assertNotNull(result)
    }

    // ==================== 7. GPS抖动模拟 ====================

    @Test
    fun `gps jitter - alternating on-off route resets count`() {
        val onRoute = TrackPoint(39.9005, 116.4000)
        val offRoute = TrackPoint(39.9005, 116.4010)

        // 模拟GPS抖动：偏离->回来->偏离->回来
        tracker.update(offRoute, testRoute)
        assertEquals(1, tracker.consecutiveCount)

        tracker.update(onRoute, testRoute)
        assertEquals(0, tracker.consecutiveCount)

        tracker.update(offRoute, testRoute)
        assertEquals(1, tracker.consecutiveCount)

        // 不应触发
        assertFalse(tracker.isDeviated)
    }

    @Test
    fun `gps jitter - small fluctuation around threshold`() {
        val tracker2 = DeviationTracker(thresholdMeters = 50.0, consecutiveThreshold = 3)
        // 稍微超过阈值
        val barelyOff = TrackPoint(39.9005, 116.40065) // ~56m off

        tracker2.update(barelyOff, testRoute)
        tracker2.update(barelyOff, testRoute)

        // 回到阈值内
        val barelyOn = TrackPoint(39.9005, 116.40045) // ~39m off
        tracker2.update(barelyOn, testRoute)

        assertFalse("阈值边缘抖动不应触发", tracker2.isDeviated)
        assertEquals(0, tracker2.consecutiveCount)
    }

    // ==================== 8. 格式化输出 ====================

    @Test
    fun `format - very small distance`() {
        val result = RouteDeviationDetector.formatDeviationDistance(0.5)
        assertEquals("在路线上", result)
    }

    @Test
    fun `format - meters`() {
        val result = RouteDeviationDetector.formatDeviationDistance(85.0)
        assertEquals("85米", result)
    }

    @Test
    fun `format - kilometers`() {
        val result = RouteDeviationDetector.formatDeviationDistance(1500.0)
        assertEquals("1.5公里", result)
    }

    @Test
    fun `format - exact 1km`() {
        val result = RouteDeviationDetector.formatDeviationDistance(1000.0)
        assertEquals("1.0公里", result)
    }

    @Test
    fun `format - large distance`() {
        val result = RouteDeviationDetector.formatDeviationDistance(5280.0)
        assertEquals("5.3公里", result)
    }

    @Test
    fun `format - just under 1m`() {
        val result = RouteDeviationDetector.formatDeviationDistance(0.99)
        assertEquals("在路线上", result)
    }

    @Test
    fun `format - exactly 1m`() {
        val result = RouteDeviationDetector.formatDeviationDistance(1.0)
        assertEquals("1米", result)
    }

    // ==================== 9. RouteDeviationState模型 ====================

    @Test
    fun `state - Normal state`() {
        val state: RouteDeviationState = RouteDeviationState.Normal
        assertTrue(state is RouteDeviationState.Normal)
    }

    @Test
    fun `state - Warning state holds data`() {
        val state = RouteDeviationState.Warning(
            deviationDistance = 60.0,
            consecutiveCount = 2
        )
        assertEquals(60.0, state.deviationDistance, 0.01)
        assertEquals(2, state.consecutiveCount)
    }

    @Test
    fun `state - Deviated state holds data`() {
        val nearestPoint = LatLng(39.9005, 116.4000)
        val state = RouteDeviationState.Deviated(
            deviationDistance = 80.0,
            nearestPointOnRoute = nearestPoint
        )
        assertEquals(80.0, state.deviationDistance, 0.01)
        assertNotNull(state.nearestPointOnRoute)
        assertEquals(39.9005, state.nearestPointOnRoute!!.latitude, 0.0001)
    }

    @Test
    fun `state - Deviated with null nearest point`() {
        val state = RouteDeviationState.Deviated(
            deviationDistance = 100.0,
            nearestPointOnRoute = null
        )
        assertNull(state.nearestPointOnRoute)
    }

    @Test
    fun `state - Replanning state`() {
        val state: RouteDeviationState = RouteDeviationState.Replanning
        assertTrue(state is RouteDeviationState.Replanning)
    }

    // ==================== 10. DeviationResult模型 ====================

    @Test
    fun `result - default values`() {
        val result = DeviationResult()
        assertFalse(result.isDeviated)
        assertEquals(0.0, result.deviationDistance, 0.01)
        assertNull(result.nearestPointOnRoute)
        assertEquals(0, result.consecutiveDeviationCount)
        assertEquals(-1, result.nearestSegmentIndex)
    }

    @Test
    fun `result - with all fields`() {
        val nearest = LatLng(39.9, 116.4)
        val result = DeviationResult(
            isDeviated = true,
            deviationDistance = 75.5,
            nearestPointOnRoute = nearest,
            consecutiveDeviationCount = 4,
            nearestSegmentIndex = 2
        )
        assertTrue(result.isDeviated)
        assertEquals(75.5, result.deviationDistance, 0.01)
        assertEquals(nearest, result.nearestPointOnRoute)
        assertEquals(4, result.consecutiveDeviationCount)
        assertEquals(2, result.nearestSegmentIndex)
    }

    // ==================== 11. 坐标转换辅助函数 ====================

    @Test
    fun `coordConvert - longitudeToMeters at equator`() {
        // 赤道上1度经度 ≈ 111km
        val meters = RouteDeviationDetector.longitudeToMeters(1.0, 0.0)
        assertTrue("赤道1度经度约111km，实际: ${meters / 1000}km", meters in 110000.0..112000.0)
    }

    @Test
    fun `coordConvert - longitudeToMeters at high latitude`() {
        // 高纬度，经度距离变短
        val metersEquator = RouteDeviationDetector.longitudeToMeters(1.0, 0.0)
        val metersHighLat = RouteDeviationDetector.longitudeToMeters(1.0, 60.0)
        assertTrue("高纬度经度距离应更短", metersHighLat < metersEquator)
        // cos(60°) = 0.5，所以应该约为赤道的一半
        assertEquals(metersEquator / 2.0, metersHighLat, metersEquator * 0.01)
    }

    @Test
    fun `coordConvert - latitudeToMeters`() {
        // 1度纬度 ≈ 111km
        val meters = RouteDeviationDetector.latitudeToMeters(1.0)
        assertTrue("1度纬度约111km，实际: ${meters / 1000}km", meters in 110000.0..112000.0)
    }

    @Test
    fun `coordConvert - roundtrip latitude conversion`() {
        val originalDeg = 0.001
        val meters = RouteDeviationDetector.latitudeToMeters(originalDeg)
        val backDeg = RouteDeviationDetector.metersToLatitude(meters)
        assertEquals(originalDeg, backDeg, 1e-8)
    }

    @Test
    fun `coordConvert - roundtrip longitude conversion`() {
        val originalDeg = 0.001
        val lat = 39.9
        val meters = RouteDeviationDetector.longitudeToMeters(originalDeg, lat)
        val backDeg = RouteDeviationDetector.metersToLongitude(meters, lat)
        assertEquals(originalDeg, backDeg, 1e-8)
    }

    // ==================== 12. 实际场景模拟 ====================

    @Test
    fun `scenario - runner following route closely`() {
        // 模拟跑步者沿路线跑，轻微偏差
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9005, 116.4000),
            LatLng(39.9010, 116.4000),
            LatLng(39.9015, 116.4000),
            LatLng(39.9020, 116.4000)
        )

        val tracker = DeviationTracker(thresholdMeters = 50.0, consecutiveThreshold = 3)

        // 每个位置轻微偏移1-2m
        val locations = listOf(
            TrackPoint(39.90002, 116.40001),
            TrackPoint(39.90052, 116.39999),
            TrackPoint(39.90098, 116.40002),
            TrackPoint(39.90153, 116.39998),
            TrackPoint(39.90198, 116.40001)
        )

        locations.forEach { loc ->
            val result = tracker.update(loc, route)
            assertFalse("沿路线跑不应偏离", result.isDeviated)
        }
    }

    @Test
    fun `scenario - runner gradually drifting away`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9020, 116.4000)
        )

        val tracker = DeviationTracker(thresholdMeters = 50.0, consecutiveThreshold = 3)

        // 逐渐向东偏移
        val locations = listOf(
            TrackPoint(39.9005, 116.4002), // ~17m off
            TrackPoint(39.9007, 116.4005), // ~43m off
            TrackPoint(39.9009, 116.4008), // ~69m off - 偏离!
            TrackPoint(39.9011, 116.4010), // ~86m off - 偏离!
            TrackPoint(39.9013, 116.4012)  // ~103m off - 偏离!
        )

        var deviatedAt = -1
        locations.forEachIndexed { i, loc ->
            val result = tracker.update(loc, route)
            if (result.isDeviated && deviatedAt == -1) {
                deviatedAt = i
            }
        }

        assertTrue("应在某个位置检测到偏航", tracker.isDeviated)
        assertTrue("偏航检测索引应 >= 2", deviatedAt >= 2)
    }

    @Test
    fun `scenario - runner turns back to route`() {
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9020, 116.4000)
        )

        val tracker = DeviationTracker(thresholdMeters = 50.0, consecutiveThreshold = 3)

        // 偏离路线
        val offRoute = TrackPoint(39.9010, 116.4010) // ~86m off
        repeat(3) { tracker.update(offRoute, route) }
        assertTrue("应偏航", tracker.isDeviated)

        // 重置后回到路线
        tracker.reset()
        val onRoute = TrackPoint(39.9010, 116.4000)
        tracker.update(onRoute, route)

        assertFalse("回到路线后不应偏航", tracker.isDeviated)
        assertEquals(0, tracker.consecutiveCount)
    }

    @Test
    fun `scenario - circular route`() {
        // 环形路线（简化的方形）
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9000, 116.4010),
            LatLng(39.9010, 116.4010),
            LatLng(39.9010, 116.4000),
            LatLng(39.9000, 116.4000)
        )

        // 在环形内部的点
        val inside = TrackPoint(39.9005, 116.4005)
        val result = RouteDeviationDetector.checkDeviation(
            inside.latitude, inside.longitude, route
        )
        // 内部点到边的距离应该是半边长的一半左右（约50-60m）
        assertNotNull(result)
    }

    @Test
    fun `scenario - parallel street deviation`() {
        // 跑在平行的另一条街上
        val route = listOf(
            LatLng(39.9000, 116.4000),
            LatLng(39.9050, 116.4000)
        )

        // 跑在约100m东边的平行路上
        val tracker = DeviationTracker(thresholdMeters = 50.0, consecutiveThreshold = 3)

        val parallelLocations = listOf(
            TrackPoint(39.9010, 116.4012),
            TrackPoint(39.9020, 116.4012),
            TrackPoint(39.9030, 116.4012)
        )

        parallelLocations.forEach { loc ->
            tracker.update(loc, route)
        }

        assertTrue("跑在平行路上应检测到偏航", tracker.isDeviated)
    }

    // ==================== 13. LatLng数据类测试 ====================

    @Test
    fun `latLng - equality`() {
        val a = LatLng(39.9, 116.4)
        val b = LatLng(39.9, 116.4)
        assertEquals(a, b)
    }

    @Test
    fun `latLng - inequality`() {
        val a = LatLng(39.9, 116.4)
        val b = LatLng(39.9, 116.5)
        assertNotEquals(a, b)
    }

    @Test
    fun `latLng - copy`() {
        val a = LatLng(39.9, 116.4)
        val b = a.copy(latitude = 40.0)
        assertEquals(40.0, b.latitude, 0.0001)
        assertEquals(116.4, b.longitude, 0.0001)
    }

    // ==================== 14. 常量值检查 ====================

    @Test
    fun `constants - default threshold is 50m`() {
        assertEquals(50.0, RouteDeviationDetector.DEFAULT_DEVIATION_THRESHOLD_METERS, 0.0)
    }

    @Test
    fun `constants - default consecutive count is 3`() {
        assertEquals(3, RouteDeviationDetector.DEFAULT_CONSECUTIVE_COUNT)
    }
}
