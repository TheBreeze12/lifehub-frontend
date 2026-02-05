package com.example.lifehub.ui.components

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.maps.model.PolylineOptions

/**
 * 地图视图状态
 */
sealed class AMapViewState {
    object Loading : AMapViewState()
    data class Success(val map: AMap) : AMapViewState()
    data class Error(val message: String) : AMapViewState()
}

/**
 * 地图定位回调接口
 */
interface AMapLocationCallback {
    fun onLocationChanged(latitude: Double, longitude: Double)
    fun onLocationError(errorCode: Int, errorInfo: String)
}

/**
 * 高德地图Compose组件 - Phase 23
 * 
 * 基础地图组件，支持：
 * - 地图初始化和显示
 * - 定位功能
 * - 标记点添加
 * - 路线绘制（为Phase 24做准备）
 *
 * @param modifier 修饰符
 * @param initialLatitude 初始纬度（默认北京天安门）
 * @param initialLongitude 初始经度
 * @param initialZoom 初始缩放级别
 * @param showLocation 是否显示定位蓝点
 * @param onMapReady 地图准备就绪回调
 * @param onLocationCallback 定位回调
 * @param markers 要显示的标记点列表
 * @param polylines 要绘制的路线列表
 */
@Composable
fun AMapComposeView(
    modifier: Modifier = Modifier,
    initialLatitude: Double = 39.9042,  // 北京天安门默认位置
    initialLongitude: Double = 116.4074,
    initialZoom: Float = 15f,
    showLocation: Boolean = true,
    onMapReady: ((AMap) -> Unit)? = null,
    onLocationCallback: AMapLocationCallback? = null,
    markers: List<MarkerData> = emptyList(),
    polylines: List<PolylineData> = emptyList()
) {
    val context = LocalContext.current
    var mapViewState by remember { mutableStateOf<AMapViewState>(AMapViewState.Loading) }
    
    // 使用remember创建MapView，确保同步初始化
    val mapView = remember {
        try {
            MapView(context).apply {
                onCreate(Bundle())
            }
        } catch (e: Exception) {
            null
        }
    }

    // 地图View的生命周期管理
    DisposableEffect(mapView) {
        if (mapView == null) {
            mapViewState = AMapViewState.Error("地图初始化失败")
            return@DisposableEffect onDispose { }
        }
        
        mapView.getMapAsync { aMap ->
                try {
                    // 配置地图
                    setupMap(
                        aMap = aMap,
                        showLocation = showLocation,
                        initialLat = initialLatitude,
                        initialLng = initialLongitude,
                        initialZoom = initialZoom,
                        onLocationCallback = onLocationCallback
                    )

                    // 添加标记点
                    addMarkersToMap(aMap, markers)

                    // 绘制路线
                    addPolylinesToMap(aMap, polylines)

                    mapViewState = AMapViewState.Success(aMap)
                    onMapReady?.invoke(aMap)
                } catch (e: Exception) {
                    mapViewState = AMapViewState.Error("地图配置失败: ${e.message}")
                }
            }
        }

        onDispose {
            mapView.onDestroy()
        }
    }

    // 更新标记点和路线
    LaunchedEffect(markers, polylines) {
        (mapViewState as? AMapViewState.Success)?.let { state ->
            state.map.clear() // 清除现有标记
            addMarkersToMap(state.map, markers)
            addPolylinesToMap(state.map, polylines)
        }
    }

    // UI显示
    Box(modifier = modifier.fillMaxSize()) {
        when (val state = mapViewState) {
            is AMapViewState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is AMapViewState.Error -> {
                // 可以在这里显示错误UI
            }
            is AMapViewState.Success -> {
                mapView?.let { mv ->
                    AndroidView(
                        factory = { _ -> mv },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 配置地图基本设置
 */
private fun setupMap(
    aMap: AMap,
    showLocation: Boolean,
    initialLat: Double,
    initialLng: Double,
    initialZoom: Float,
    onLocationCallback: AMapLocationCallback?
) {
    // 设置初始位置
    val latLng = LatLng(initialLat, initialLng)
    val cameraUpdate = com.amap.api.maps.CameraUpdateFactory.newCameraPosition(
        CameraPosition(latLng, initialZoom, 0f, 0f)
    )
    aMap.moveCamera(cameraUpdate)

    // 配置定位
    if (showLocation) {
        setupLocation(aMap, onLocationCallback)
    }

    // 设置UI控件
    aMap.uiSettings.apply {
        isZoomControlsEnabled = true      // 显示缩放控件
        isCompassEnabled = true          // 显示指南针
        isScaleControlsEnabled = true    // 显示比例尺
        isMyLocationButtonEnabled = true // 显示定位按钮
    }
}

/**
 * 配置定位功能
 */
private fun setupLocation(aMap: AMap, callback: AMapLocationCallback?) {
    try {
        // 设置定位样式
        val myLocationStyle = MyLocationStyle().apply {
            // 定位蓝点样式
            strokeColor(Color.BLUE)
            radiusFillColor(Color.argb(100, 0, 0, 255))
            strokeWidth(2f)
        }
        aMap.myLocationStyle = myLocationStyle

        // 启用定位层
        aMap.isMyLocationEnabled = true

        // 设置定位回调
        aMap.setOnMyLocationChangeListener { location ->
            location?.let {
                callback?.onLocationChanged(it.latitude, it.longitude)
            }
        }
    } catch (e: Exception) {
        callback?.onLocationError(-1, "定位初始化失败: ${e.message}")
    }
}

/**
 * 添加标记点到地图
 */
private fun addMarkersToMap(aMap: AMap, markers: List<MarkerData>) {
    markers.forEach { markerData ->
        val markerOptions = MarkerOptions()
            .position(LatLng(markerData.latitude, markerData.longitude))
            .title(markerData.title)
            .snippet(markerData.snippet)
            .draggable(markerData.draggable)
        
        // 设置自定义图标（如果有）
        markerData.iconResId?.let {
            // 可以在这里设置自定义图标
        }
        
        aMap.addMarker(markerOptions)
    }
}

/**
 * 添加路线到地图
 */
private fun addPolylinesToMap(aMap: AMap, polylines: List<PolylineData>) {
    polylines.forEach { polylineData ->
        val latLngList = polylineData.points.map { LatLng(it.latitude, it.longitude) }
        
        val polylineOptions = PolylineOptions()
            .addAll(latLngList)
            .width(polylineData.width)
            .color(polylineData.color)
            .geodesic(true)
        
        aMap.addPolyline(polylineOptions)
    }
}

/**
 * 标记点数据类
 */
data class MarkerData(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val snippet: String = "",
    val draggable: Boolean = false,
    val iconResId: Int? = null
)

/**
 * 路线数据类
 */
data class PolylineData(
    val points: List<LatLngPoint>,
    val color: Int = Color.BLUE,
    val width: Float = 10f
)

/**
 * 经纬度点
 */
data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * 简化的地图组件 - 仅显示地图和定位
 * 适用于快速集成场景
 */
@Composable
fun SimpleMapView(
    modifier: Modifier = Modifier,
    onMapReady: ((AMap) -> Unit)? = null
) {
    AMapComposeView(
        modifier = modifier,
        showLocation = true,
        onMapReady = onMapReady
    )
}

/**
 * 带路线的地图组件 - 为运动路线展示准备（Phase 24）
 */
@Composable
fun RouteMapView(
    modifier: Modifier = Modifier,
    routePoints: List<List<LatLngPoint>>,
    routeColors: List<Int> = listOf(Color.BLUE, Color.GREEN, Color.RED),
    onRouteSelected: ((Int) -> Unit)? = null
) {
    val polylines = routePoints.mapIndexed { index, points ->
        PolylineData(
            points = points,
            color = routeColors.getOrElse(index) { Color.BLUE },
            width = 12f
        )
    }

    AMapComposeView(
        modifier = modifier,
        showLocation = true,
        polylines = polylines
    )
}
