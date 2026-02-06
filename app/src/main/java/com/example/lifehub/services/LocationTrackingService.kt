package com.example.lifehub.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.lifehub.data.TrackPoint
import com.google.android.gms.location.*

/**
 * GPS位置追踪服务 - Phase 27
 *
 * 使用FusedLocationProviderClient持续追踪GPS位置，
 * 将位置更新转换为TrackPoint供ViewModel使用。
 *
 * @param context Application context
 * @param onLocationUpdate 位置更新回调
 * @param onError 错误回调
 */
class LocationTrackingService(
    private val context: Context,
    private val onLocationUpdate: (TrackPoint) -> Unit,
    private val onError: (String) -> Unit
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private var isTracking = false

    companion object {
        /** 位置更新间隔（毫秒） */
        const val UPDATE_INTERVAL_MS = 2000L
        /** 最快位置更新间隔（毫秒） */
        const val FASTEST_INTERVAL_MS = 1000L
        /** 最小位移（米），低于此值不触发更新 */
        const val MIN_DISPLACEMENT_METERS = 2f
    }

    /**
     * 开始位置追踪
     * 需要ACCESS_FINE_LOCATION权限
     */
    fun startTracking() {
        if (isTracking) return

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onError("缺少位置权限，无法开始追踪")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val trackPoint = locationToTrackPoint(location)
                    onLocationUpdate(trackPoint)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isTracking = true
        } catch (e: SecurityException) {
            onError("位置权限被拒绝: ${e.message}")
        } catch (e: Exception) {
            onError("位置追踪启动失败: ${e.message}")
        }
    }

    /**
     * 停止位置追踪
     */
    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        isTracking = false
    }

    /**
     * 是否正在追踪
     */
    fun isCurrentlyTracking(): Boolean = isTracking

    /**
     * 获取最后已知位置（用于初始定位）
     */
    fun getLastKnownLocation(onResult: (TrackPoint?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null)
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                onResult(location?.let { locationToTrackPoint(it) })
            }.addOnFailureListener {
                onResult(null)
            }
        } catch (e: SecurityException) {
            onResult(null)
        }
    }

    /**
     * 将Android Location转换为TrackPoint
     */
    private fun locationToTrackPoint(location: Location): TrackPoint {
        return TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            timestamp = location.time
        )
    }
}
