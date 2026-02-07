package com.example.lifehub.utils

import com.example.lifehub.data.TrackPoint
import kotlin.math.*
import kotlin.math.roundToInt

/**
 * 路线偏离检测器 - Phase 52
 *
 * 功能：
 * - 检测用户当前位置是否偏离了规划路线
 * - 当偏离距离超过阈值（默认50m）时触发偏航告警
 * - 支持自定义偏离阈值
 * - 支持连续偏离计数（避免GPS抖动误报）
 *
 * 算法：
 * 计算当前位置到规划路线上最近线段的垂直距离（点到线段距离）。
 * 若距离 > 阈值且连续N次检测均偏离，则判定为偏航。
 */
object RouteDeviationDetector {

    /** 默认偏离阈值（米） */
    const val DEFAULT_DEVIATION_THRESHOLD_METERS = 50.0

    /** 连续偏离检测次数阈值，防止GPS抖动误报 */
    const val DEFAULT_CONSECUTIVE_COUNT = 3

    /** 地球半径（米），用于Haversine计算 */
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * 偏离检测结果
     *
     * @param isDeviated 是否偏离路线
     * @param deviationDistance 偏离距离（米），到最近路线段的距离
     * @param nearestPointOnRoute 路线上最近的点
     * @param consecutiveDeviationCount 连续偏离次数
     * @param nearestSegmentIndex 最近线段的索引
     */
    data class DeviationResult(
        val isDeviated: Boolean = false,
        val deviationDistance: Double = 0.0,
        val nearestPointOnRoute: LatLng? = null,
        val consecutiveDeviationCount: Int = 0,
        val nearestSegmentIndex: Int = -1
    )

    /**
     * 经纬度点（简化版，纯计算用，不依赖Android SDK）
     */
    data class LatLng(
        val latitude: Double,
        val longitude: Double
    )

    /**
     * 偏离检测状态追踪器
     *
     * 维护连续偏离计数，用于过滤GPS抖动导致的误报。
     * 仅当连续偏离次数 >= consecutiveThreshold 时才判定为真正偏航。
     */
    class DeviationTracker(
        val thresholdMeters: Double = DEFAULT_DEVIATION_THRESHOLD_METERS,
        val consecutiveThreshold: Int = DEFAULT_CONSECUTIVE_COUNT
    ) {
        private var _consecutiveCount: Int = 0
        val consecutiveCount: Int get() = _consecutiveCount

        private var _lastDeviationDistance: Double = 0.0
        val lastDeviationDistance: Double get() = _lastDeviationDistance

        private var _isDeviated: Boolean = false
        val isDeviated: Boolean get() = _isDeviated

        /**
         * 更新偏离状态
         * @param currentLocation 当前位置
         * @param routePoints 规划路线点列表
         * @return 偏离检测结果
         */
        fun update(
            currentLocation: TrackPoint,
            routePoints: List<LatLng>
        ): DeviationResult {
            if (routePoints.size < 2) {
                // 路线点不足，无法判断偏离
                return DeviationResult(
                    isDeviated = false,
                    deviationDistance = 0.0,
                    consecutiveDeviationCount = 0
                )
            }

            val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            val (distance, nearestPoint, segmentIndex) = findNearestPointOnRoute(
                currentLatLng, routePoints
            )

            _lastDeviationDistance = distance

            if (distance > thresholdMeters) {
                _consecutiveCount++
            } else {
                _consecutiveCount = 0
                _isDeviated = false
            }

            _isDeviated = _consecutiveCount >= consecutiveThreshold

            return DeviationResult(
                isDeviated = _isDeviated,
                deviationDistance = distance,
                nearestPointOnRoute = nearestPoint,
                consecutiveDeviationCount = _consecutiveCount,
                nearestSegmentIndex = segmentIndex
            )
        }

        /**
         * 重置偏离状态（用于重规划后）
         */
        fun reset() {
            _consecutiveCount = 0
            _lastDeviationDistance = 0.0
            _isDeviated = false
        }
    }

    /**
     * 在路线上找到离当前位置最近的点
     *
     * 遍历路线所有相邻线段，计算点到线段的最近距离，返回全局最小值。
     *
     * @param point 当前位置
     * @param routePoints 路线点列表
     * @return Triple(最近距离, 最近点, 线段索引)
     */
    fun findNearestPointOnRoute(
        point: LatLng,
        routePoints: List<LatLng>
    ): Triple<Double, LatLng, Int> {
        if (routePoints.isEmpty()) {
            return Triple(Double.MAX_VALUE, point, -1)
        }
        if (routePoints.size == 1) {
            val dist = haversineDistance(point, routePoints[0])
            return Triple(dist, routePoints[0], 0)
        }

        var minDistance = Double.MAX_VALUE
        var nearestPoint = routePoints[0]
        var nearestSegmentIndex = 0

        for (i in 0 until routePoints.size - 1) {
            val segStart = routePoints[i]
            val segEnd = routePoints[i + 1]
            val (dist, closest) = pointToSegmentDistance(point, segStart, segEnd)

            if (dist < minDistance) {
                minDistance = dist
                nearestPoint = closest
                nearestSegmentIndex = i
            }
        }

        return Triple(minDistance, nearestPoint, nearestSegmentIndex)
    }

    /**
     * 计算点到线段的最近距离和最近点
     *
     * 使用投影法：将点投影到线段所在直线上，
     * 若投影点在线段内则返回投影点距离，否则返回到端点的距离。
     *
     * 注意：为了精度，先将经纬度转换为平面坐标（局部切平面近似），
     * 在小范围内（<几公里）误差可忽略。
     *
     * @param point 目标点
     * @param segStart 线段起点
     * @param segEnd 线段终点
     * @return Pair(距离米, 最近点经纬度)
     */
    fun pointToSegmentDistance(
        point: LatLng,
        segStart: LatLng,
        segEnd: LatLng
    ): Pair<Double, LatLng> {
        // 将经纬度转换为以segStart为原点的局部平面坐标（米）
        val px = longitudeToMeters(point.longitude - segStart.longitude, segStart.latitude)
        val py = latitudeToMeters(point.latitude - segStart.latitude)

        val ax = 0.0
        val ay = 0.0
        val bx = longitudeToMeters(segEnd.longitude - segStart.longitude, segStart.latitude)
        val by = latitudeToMeters(segEnd.latitude - segStart.latitude)

        val dx = bx - ax
        val dy = by - ay
        val segLenSq = dx * dx + dy * dy

        if (segLenSq < 1e-10) {
            // 线段退化为点
            val dist = haversineDistance(point, segStart)
            return Pair(dist, segStart)
        }

        // 计算投影参数 t ∈ [0, 1]
        var t = ((px - ax) * dx + (py - ay) * dy) / segLenSq
        t = t.coerceIn(0.0, 1.0)

        // 投影点在平面坐标中的位置
        val projX = ax + t * dx
        val projY = ay + t * dy

        // 转回经纬度
        val projLat = segStart.latitude + metersToLatitude(projY)
        val projLng = segStart.longitude + metersToLongitude(projX, segStart.latitude)

        val closestPoint = LatLng(projLat, projLng)
        val distance = haversineDistance(point, closestPoint)

        return Pair(distance, closestPoint)
    }

    /**
     * Haversine距离计算
     * @return 两点间距离（米）
     */
    fun haversineDistance(p1: LatLng, p2: LatLng): Double {
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * 简单检测：当前位置是否偏离路线超过阈值
     * （不维护连续计数状态，适用于一次性检测）
     *
     * @param currentLat 当前纬度
     * @param currentLng 当前经度
     * @param routePoints 路线点列表
     * @param thresholdMeters 偏离阈值（米）
     * @return 偏离检测结果
     */
    fun checkDeviation(
        currentLat: Double,
        currentLng: Double,
        routePoints: List<LatLng>,
        thresholdMeters: Double = DEFAULT_DEVIATION_THRESHOLD_METERS
    ): DeviationResult {
        if (routePoints.size < 2) {
            return DeviationResult(isDeviated = false, deviationDistance = 0.0)
        }

        val currentPoint = LatLng(currentLat, currentLng)
        val (distance, nearestPoint, segIndex) = findNearestPointOnRoute(currentPoint, routePoints)

        return DeviationResult(
            isDeviated = distance > thresholdMeters,
            deviationDistance = distance,
            nearestPointOnRoute = nearestPoint,
            consecutiveDeviationCount = if (distance > thresholdMeters) 1 else 0,
            nearestSegmentIndex = segIndex
        )
    }

    /**
     * 格式化偏离距离为用户可读字符串
     */
    fun formatDeviationDistance(meters: Double): String {
        return when {
            meters < 1.0 -> "在路线上"
            meters < 1000.0 -> "${meters.roundToInt()}米"
            else -> String.format("%.1f公里", meters / 1000.0)
        }
    }

    // ==================== 坐标转换辅助函数 ====================

    /**
     * 经度差转米（考虑纬度）
     * @param dLon 经度差（度）
     * @param latitude 参考纬度（度）
     * @return 距离（米）
     */
    internal fun longitudeToMeters(dLon: Double, latitude: Double): Double {
        return dLon * Math.toRadians(1.0) * EARTH_RADIUS_METERS * cos(Math.toRadians(latitude))
    }

    /**
     * 纬度差转米
     * @param dLat 纬度差（度）
     * @return 距离（米）
     */
    internal fun latitudeToMeters(dLat: Double): Double {
        return dLat * Math.toRadians(1.0) * EARTH_RADIUS_METERS
    }

    /**
     * 米转纬度差
     */
    internal fun metersToLatitude(meters: Double): Double {
        return meters / (Math.toRadians(1.0) * EARTH_RADIUS_METERS)
    }

    /**
     * 米转经度差（考虑纬度）
     */
    internal fun metersToLongitude(meters: Double, latitude: Double): Double {
        val cosLat = cos(Math.toRadians(latitude))
        if (cosLat < 1e-10) return 0.0
        return meters / (Math.toRadians(1.0) * EARTH_RADIUS_METERS * cosLat)
    }

    /**
     * Double的四舍五入到整数
     */
    private fun roundDoubleToInt(value: Double): Int = value.roundToInt()
}

/**
 * 路线偏离告警状态 - Phase 52
 * 用于UI层展示偏航提醒
 */
sealed class RouteDeviationState {
    /** 未检测/无偏离 */
    object Normal : RouteDeviationState()

    /** 检测到偏离但未达到连续阈值（GPS可能抖动） */
    data class Warning(
        val deviationDistance: Double,
        val consecutiveCount: Int
    ) : RouteDeviationState()

    /** 确认偏航（连续偏离超过阈值） */
    data class Deviated(
        val deviationDistance: Double,
        val nearestPointOnRoute: RouteDeviationDetector.LatLng?
    ) : RouteDeviationState()

    /** 已触发重规划 */
    object Replanning : RouteDeviationState()
}
