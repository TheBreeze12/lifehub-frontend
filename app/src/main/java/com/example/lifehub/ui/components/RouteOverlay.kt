package com.example.lifehub.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifehub.data.ParetoRoute
import com.example.lifehub.data.RouteWaypoint
import com.example.lifehub.ui.theme.*

/**
 * Phase 24: 运动路线叠加层组件
 * 
 * 在地图上展示帕累托最优路线信息，支持：
 * - 显示多条路线信息卡片
 * - 路线切换选择
 * - 路线详细信息展示（耗时、消耗、绿化评分）
 */

/** 路线颜色定义 */
object RouteColors {
    val GreenRoute = Color(0xFF4CAF50)   // 绿色 - 最佳绿化
    val BlueRoute = Color(0xFF2196F3)    // 蓝色 - 最短时间
    val OrangeRoute = Color(0xFFFF9800)  // 橙色 - 最大消耗
    
    /** 获取路线颜色（按索引） */
    fun getRouteColor(index: Int): Color {
        return when (index % 3) {
            0 -> BlueRoute
            1 -> OrangeRoute
            2 -> GreenRoute
            else -> BlueRoute
        }
    }
    
    /** 获取Android颜色值（用于地图绘制） */
    fun getAndroidColor(index: Int): Int {
        return when (index % 3) {
            0 -> AndroidColor.parseColor("#2196F3")
            1 -> AndroidColor.parseColor("#FF9800")
            2 -> AndroidColor.parseColor("#4CAF50")
            else -> AndroidColor.BLUE
        }
    }
}

/**
 * 路线叠加层 - 主组件
 * 
 * @param routes 帕累托最优路线列表
 * @param selectedIndex 当前选中的路线索引
 * @param onRouteSelected 路线选择回调
 * @param modifier 修饰符
 */
@Composable
fun RouteOverlay(
    routes: List<ParetoRoute>,
    selectedIndex: Int,
    onRouteSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (routes.isEmpty()) {
        EmptyRoutesHint(modifier)
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 路线选择器
        if (routes.size > 1) {
            RouteSelector(
                routes = routes,
                selectedIndex = selectedIndex,
                onRouteSelected = onRouteSelected
            )
        }
        
        // 当前选中路线的详情卡片
        val selectedRoute = routes.getOrNull(selectedIndex)
        selectedRoute?.let { route ->
            RouteInfoCard(
                route = route,
                routeIndex = selectedIndex,
                isSelected = true
            )
        }
    }
}

/**
 * 路线选择器 - 水平滚动的路线切换按钮
 */
@Composable
fun RouteSelector(
    routes: List<ParetoRoute>,
    selectedIndex: Int,
    onRouteSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        routes.forEachIndexed { index, route ->
            RouteSelectorChip(
                routeName = route.routeName,
                routeColor = RouteColors.getRouteColor(index),
                isSelected = index == selectedIndex,
                onClick = { onRouteSelected(index) }
            )
        }
    }
}

/**
 * 路线选择芯片
 */
@Composable
private fun RouteSelectorChip(
    routeName: String,
    routeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) routeColor else Color.White,
        label = "chipBgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else routeColor,
        label = "chipContentColor"
    )
    
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, routeColor)
        } else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor
                )
            }
            Text(
                text = routeName,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * 路线信息卡片 - 展示路线详细信息
 */
@Composable
fun RouteInfoCard(
    route: ParetoRoute,
    routeIndex: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val routeColor = RouteColors.getRouteColor(routeIndex)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = routeColor.copy(alpha = 0.2f),
                spotColor = routeColor.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(routeColor)
                    )
                    Text(
                        text = route.routeName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                
                // 运动类型标签
                route.exerciseType?.let { type ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = routeColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = getExerciseTypeLabel(type),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = routeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // 信息指标网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RouteMetricItem(
                    icon = Icons.Default.Timer,
                    value = formatTime(route.timeMinutes),
                    label = "耗时",
                    color = RouteColors.BlueRoute
                )
                RouteMetricItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "${route.caloriesBurn.toInt()}卡",
                    label = "消耗",
                    color = RouteColors.OrangeRoute
                )
                RouteMetricItem(
                    icon = Icons.Default.Park,
                    value = "${route.greeneryScore.toInt()}分",
                    label = "绿化",
                    color = RouteColors.GreenRoute
                )
                RouteMetricItem(
                    icon = Icons.Default.Straighten,
                    value = formatDistance(route.distanceMeters),
                    label = "距离",
                    color = TextSecondary
                )
            }
            
            // 强度指示器（如果有）
            route.intensity?.let { intensity ->
                IntensityIndicator(intensity = intensity, color = routeColor)
            }
        }
    }
}

/**
 * 路线指标项
 */
@Composable
private fun RouteMetricItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextTertiary
        )
    }
}

/**
 * 运动强度指示器
 */
@Composable
private fun IntensityIndicator(
    intensity: Double,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "运动强度",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Text(
                text = getIntensityLabel(intensity),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
        
        // 进度条
        LinearProgressIndicator(
            progress = { intensity.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

/**
 * 空路线提示
 */
@Composable
private fun EmptyRoutesHint(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundBeige)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSecondary
            )
            Text(
                text = "暂无推荐路线，请先生成运动计划",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

// ==================== 工具函数 ====================

/** 格式化时间 */
private fun formatTime(minutes: Double): String {
    val totalMinutes = minutes.toInt()
    return if (totalMinutes >= 60) {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        if (mins > 0) "${hours}时${mins}分" else "${hours}小时"
    } else {
        "${totalMinutes}分钟"
    }
}

/** 格式化距离 */
private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        val km = meters / 1000
        String.format("%.1f公里", km)
    } else {
        "${meters.toInt()}米"
    }
}

/** 获取运动类型标签 */
private fun getExerciseTypeLabel(type: String): String {
    return when (type.lowercase()) {
        "walking" -> "步行"
        "running" -> "跑步"
        "cycling" -> "骑行"
        "jogging" -> "慢跑"
        "hiking" -> "徒步"
        else -> type
    }
}

/** 获取强度标签 */
private fun getIntensityLabel(intensity: Double): String {
    return when {
        intensity < 0.3 -> "轻松"
        intensity < 0.5 -> "适中"
        intensity < 0.7 -> "中等"
        intensity < 0.85 -> "较强"
        else -> "高强度"
    }
}

/**
 * 将路径点列表转换为地图可用的LatLngPoint列表
 */
fun waypointsToLatLngPoints(waypoints: List<RouteWaypoint>): List<LatLngPoint> {
    return waypoints.map { wp ->
        LatLngPoint(
            latitude = wp.lat,
            longitude = wp.lng
        )
    }
}

/**
 * 将多条路线转换为PolylineData列表（用于地图绘制）
 */
fun routesToPolylines(
    routes: List<ParetoRoute>,
    selectedIndex: Int
): List<PolylineData> {
    return routes.mapIndexed { index, route ->
        PolylineData(
            points = waypointsToLatLngPoints(route.waypoints),
            color = RouteColors.getAndroidColor(index),
            width = if (index == selectedIndex) 14f else 8f
        )
    }
}
