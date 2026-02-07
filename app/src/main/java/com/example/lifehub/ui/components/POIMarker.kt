package com.example.lifehub.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifehub.ui.theme.*

/**
 * Phase 58: 运动POI展示组件
 *
 * 在地图上展示运动相关兴趣点（公园、步道、健身区等），
 * 并提供POI列表展示和类型筛选功能。
 */

/** POI类型枚举 */
enum class POIType {
    PARK,           // 公园
    TRAIL,          // 步道
    FITNESS_AREA,   // 健身区（户外健身器材区）
    GYM,            // 健身房
    SPORTS_FIELD    // 运动场（篮球场、足球场等）
}

/** 运动POI数据模型 */
data class ExercisePOI(
    val id: String,
    val name: String,
    val type: POIType,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val distance: Double? = null  // 距离起点的距离（米）
)

/** 获取POI类型的中文标签 */
fun getPOITypeLabel(type: POIType): String {
    return when (type) {
        POIType.PARK -> "公园"
        POIType.TRAIL -> "步道"
        POIType.FITNESS_AREA -> "健身区"
        POIType.GYM -> "健身房"
        POIType.SPORTS_FIELD -> "运动场"
    }
}

/** 获取POI类型的图标 */
fun getPOITypeIcon(type: POIType): ImageVector {
    return when (type) {
        POIType.PARK -> Icons.Default.Park
        POIType.TRAIL -> Icons.Default.Terrain
        POIType.FITNESS_AREA -> Icons.Default.FitnessCenter
        POIType.GYM -> Icons.Default.SportsMartialArts
        POIType.SPORTS_FIELD -> Icons.Default.SportsSoccer
    }
}

/** 获取POI类型的主题颜色 */
private fun getPOITypeColor(type: POIType): Color {
    return when (type) {
        POIType.PARK -> Color(0xFF4CAF50)
        POIType.TRAIL -> Color(0xFF8D6E63)
        POIType.FITNESS_AREA -> Color(0xFFFF9800)
        POIType.GYM -> Color(0xFF2196F3)
        POIType.SPORTS_FIELD -> Color(0xFF9C27B0)
    }
}

/**
 * 基于中心坐标生成模拟运动POI
 * 真实场景应接入高德POI搜索API，此处基于坐标偏移模拟生成
 */
fun generateExercisePOIs(centerLat: Double, centerLng: Double): List<ExercisePOI> {
    // 模拟POI数据：在中心点周围生成不同类型的运动POI
    return listOf(
        ExercisePOI(
            id = "poi_park_1",
            name = "翠湖公园",
            type = POIType.PARK,
            latitude = centerLat + 0.005,
            longitude = centerLng + 0.003,
            description = "大型城市公园，环湖跑道约2.5公里",
            distance = 450.0
        ),
        ExercisePOI(
            id = "poi_park_2",
            name = "滨河花园",
            type = POIType.PARK,
            latitude = centerLat - 0.003,
            longitude = centerLng + 0.008,
            description = "河畔休闲公园，林荫步道环境优美",
            distance = 780.0
        ),
        ExercisePOI(
            id = "poi_trail_1",
            name = "绿道步道",
            type = POIType.TRAIL,
            latitude = centerLat + 0.008,
            longitude = centerLng - 0.004,
            description = "城市绿道，全长3公里，适合慢跑和骑行",
            distance = 620.0
        ),
        ExercisePOI(
            id = "poi_trail_2",
            name = "山间健步道",
            type = POIType.TRAIL,
            latitude = centerLat + 0.012,
            longitude = centerLng + 0.010,
            description = "坡度适中的山间步道，全程约4公里",
            distance = 1350.0
        ),
        ExercisePOI(
            id = "poi_fitness_1",
            name = "社区健身广场",
            type = POIType.FITNESS_AREA,
            latitude = centerLat - 0.002,
            longitude = centerLng - 0.005,
            description = "户外健身器材区，含拉伸、力量训练设施",
            distance = 350.0
        ),
        ExercisePOI(
            id = "poi_gym_1",
            name = "活力健身中心",
            type = POIType.GYM,
            latitude = centerLat - 0.006,
            longitude = centerLng - 0.002,
            description = "室内健身房，器械齐全，恶劣天气备选",
            distance = 550.0
        ),
        ExercisePOI(
            id = "poi_sports_1",
            name = "阳光运动场",
            type = POIType.SPORTS_FIELD,
            latitude = centerLat + 0.003,
            longitude = centerLng - 0.009,
            description = "含篮球场、羽毛球场，适合球类运动",
            distance = 820.0
        )
    )
}

/** 按类型筛选POI */
fun filterPOIsByType(pois: List<ExercisePOI>, type: POIType?): List<ExercisePOI> {
    if (type == null) return pois
    return pois.filter { it.type == type }
}

/** 将POI列表转换为地图标记数据 */
fun poisToMarkers(pois: List<ExercisePOI>): List<MarkerData> {
    return pois.map { poi ->
        MarkerData(
            latitude = poi.latitude,
            longitude = poi.longitude,
            title = poi.name,
            snippet = "${getPOITypeLabel(poi.type)} | ${poi.description ?: ""}"
        )
    }
}

/**
 * POI展示区域 - 主组件
 *
 * 展示运动相关POI列表，支持按类型筛选
 */
@Composable
fun POISection(
    pois: List<ExercisePOI>,
    modifier: Modifier = Modifier
) {
    if (pois.isEmpty()) return

    var selectedType by remember { mutableStateOf<POIType?>(null) }
    val filteredPois = filterPOIsByType(pois, selectedType)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "周边运动设施",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${filteredPois.size}个",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
            }

            // 类型筛选芯片
            POITypeFilterChips(
                selectedType = selectedType,
                onTypeSelected = { type ->
                    selectedType = if (selectedType == type) null else type
                }
            )

            // POI列表
            filteredPois.forEach { poi ->
                POIListItem(poi = poi)
            }
        }
    }
}

/**
 * POI类型筛选芯片行
 */
@Composable
private fun POITypeFilterChips(
    selectedType: POIType?,
    onTypeSelected: (POIType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        POIType.values().forEach { type ->
            val isSelected = selectedType == type
            val typeColor = getPOITypeColor(type)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) typeColor else Color(0xFFF5F5F5),
                label = "poiChipBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextSecondary,
                label = "poiChipContent"
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTypeSelected(type) },
                color = bgColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getPOITypeIcon(type),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentColor
                    )
                    Text(
                        text = getPOITypeLabel(type),
                        fontSize = 12.sp,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 单个POI列表项
 */
@Composable
private fun POIListItem(
    poi: ExercisePOI,
    modifier: Modifier = Modifier
) {
    val typeColor = getPOITypeColor(poi.type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(typeColor.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 类型图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(typeColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getPOITypeIcon(poi.type),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = typeColor
            )
        }

        // 名称与描述
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = poi.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            poi.description?.let { desc ->
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 距离标签
        poi.distance?.let { dist ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = typeColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = if (dist >= 1000) String.format("%.1fkm", dist / 1000) else "${dist.toInt()}m",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = typeColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
