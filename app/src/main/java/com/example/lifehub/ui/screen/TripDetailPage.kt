package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.TripViewModel

/** 运动计划详情页 - MVP版本 展示完整的运动计划时间表 */
@Composable
fun TripDetailPage(
        tripId: String,
        navController: NavController,
        tripViewModel: TripViewModel = viewModel()
) {
        // 从ViewModel获取行程数据
        LaunchedEffect(tripId) { tripId.toIntOrNull()?.let { tripViewModel.getTripDetail(it) } }

        val tripDetailState by tripViewModel.tripDetailState.collectAsState()

        when (val state = tripDetailState) {
                is com.example.lifehub.viewmodel.TripDetailState.Loading -> {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = ForestGreen) }
                }
                is com.example.lifehub.viewmodel.TripDetailState.Success -> {
                        val tripPlan = state.tripPlan
                        // 将行程数据转换为UI需要的格式
                        val days =
                                groupItemsByDay(
                                        tripPlan.items,
                                        tripPlan.startDate,
                                        tripPlan.endDate
                                )

                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(
                                                        brush =
                                                                Brush.verticalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        Color.White,
                                                                                        BackgroundGradientStart,
                                                                                        BackgroundBeige
                                                                                )
                                                                )
                                                )
                        ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                        // 顶部工具栏
                                        TripDetailHeader(
                                                title = tripPlan.title,
                                                onBackClick = { navController.popBackStack() },
                                                onDownloadClick = { /* TODO: 下载离线包 */},
                                                onEditClick = { /* TODO: 编辑行程 */}
                                        )

                                        LazyColumn(
                                                modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(24.dp),
                                                verticalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                                // 天气卡片（暂时使用模拟数据，后续可以集成天气API）
                                                item {
                                                        WeatherCard(
                                                                temperature = 15,
                                                                weather = "晴转多云",
                                                                humidity = 65,
                                                                wind = "东风 2级",
                                                                destination = tripPlan.destination
                                                                                ?: "运动区域"
                                                        )
                                                }

                                                // 行程时间轴
                                                items(days) { dayData ->
                                                        DaySection(dayData = dayData)
                                                }
                                        }
                                }
                        }
                }
                is com.example.lifehub.viewmodel.TripDetailState.Error -> {
                        Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                        ) {
                                Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { navController.popBackStack() }) { Text("返回") }
                        }
                }
                else -> {
                        Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = ForestGreen) }
                }
        }
}

/** 将行程节点按天分组 */
fun groupItemsByDay(
        items: List<com.example.lifehub.data.TripItem>,
        startDate: String,
        endDate: String
): List<DayData> {
        val grouped = items.groupBy { it.dayIndex }
        val days = mutableListOf<DayData>()

        try {
                val start = java.time.LocalDate.parse(startDate)
                var currentDate = start

                grouped.keys.sorted().forEach { dayIndex ->
                        val dayItems = grouped[dayIndex] ?: emptyList()
                        val formattedDate = formatDate(currentDate)

                        days.add(
                                DayData(
                                        dayIndex = dayIndex,
                                        date = formattedDate,
                                        items =
                                                dayItems.map { item ->
                                                        TripItemData(
                                                                placeName = item.placeName,
                                                                placeType = item.placeType
                                                                                ?: "walking",
                                                                typeLabel =
                                                                        getTypeLabel(
                                                                                item.placeType
                                                                        ),
                                                                color =
                                                                        getTypeColor(
                                                                                item.placeType
                                                                        ),
                                                                icon = getTypeIcon(item.placeType),
                                                                time = item.startTime,
                                                                cost =
                                                                        item.cost?.let {
                                                                                // cost字段存储的是卡路里，显示为卡路里消耗
                                                                                "${it.toInt()}卡"
                                                                        },
                                                                duration =
                                                                        item.duration?.let {
                                                                                "${it}分钟"
                                                                        },
                                                                notes = item.notes
                                                        )
                                                }
                                )
                        )

                        currentDate = currentDate.plusDays(1)
                }
        } catch (e: Exception) {
                // 如果日期解析失败，使用默认格式
                grouped.keys.sorted().forEach { dayIndex ->
                        val dayItems = grouped[dayIndex] ?: emptyList()
                        days.add(
                                DayData(
                                        dayIndex = dayIndex,
                                        date = "第${dayIndex}天",
                                        items =
                                                dayItems.map { item ->
                                                        TripItemData(
                                                                placeName = item.placeName,
                                                                placeType = item.placeType
                                                                                ?: "walking",
                                                                typeLabel =
                                                                        getTypeLabel(
                                                                                item.placeType
                                                                        ),
                                                                color =
                                                                        getTypeColor(
                                                                                item.placeType
                                                                        ),
                                                                icon = getTypeIcon(item.placeType),
                                                                time = item.startTime,
                                                                cost =
                                                                        item.cost?.let {
                                                                                // cost字段存储的是卡路里，显示为卡路里消耗
                                                                                "${it.toInt()}卡"
                                                                        },
                                                                duration =
                                                                        item.duration?.let {
                                                                                "${it}分钟"
                                                                        },
                                                                notes = item.notes
                                                        )
                                                }
                                )
                        )
                }
        }

        return days
}

/** 格式化日期 */
fun formatDate(date: java.time.LocalDate): String {
        return "${date.monthValue}月${date.dayOfMonth}日"
}

/** 获取类型标签 */
fun getTypeLabel(placeType: String?): String {
        return when (placeType) {
                // 运动类型
                "walking" -> "散步"
                "running" -> "跑步"
                "cycling" -> "骑行"
                "park" -> "公园"
                "gym" -> "健身房"
                "indoor" -> "室内运动"
                "outdoor" -> "户外运动"
                // 兼容旧数据
                "attraction" -> "景点"
                "dining" -> "餐饮"
                "transport" -> "交通"
                "accommodation" -> "住宿"
                else -> "其他"
        }
}

/** 获取类型颜色 */
fun getTypeColor(placeType: String?): Color {
        return when (placeType) {
                // 运动类型
                "walking" -> Color(0xFF10B981) // 绿色
                "running" -> Color(0xFF3B82F6) // 蓝色
                "cycling" -> Color(0xFFF59E0B) // 橙色
                "park" -> ForestGreen
                "gym" -> Color(0xFF8B5CF6) // 紫色
                "indoor" -> Color(0xFF6366F1) // 靛蓝色
                "outdoor" -> Color(0xFF10B981) // 绿色
                // 兼容旧数据
                "attraction" -> ForestGreen
                "dining" -> VitalOrange
                "transport" -> Color(0xFF3B82F6)
                "accommodation" -> Color(0xFF8B5CF6)
                else -> Color(0xFF6B7280)
        }
}

/** 获取类型图标 */
fun getTypeIcon(placeType: String?): ImageVector {
        return when (placeType) {
                // 运动类型
                "walking" -> Icons.Default.DirectionsWalk // 散步图标（需要material-icons-extended）
                "running" -> Icons.Default.DirectionsRun // 跑步图标（已存在）
                "cycling" -> Icons.Default.DirectionsBike // 骑行图标（需要material-icons-extended）
                "park" -> Icons.Default.Park // 公园图标（需要material-icons-extended）
                "gym" -> Icons.Default.FitnessCenter // 健身房图标（需要material-icons-extended）
                "indoor" -> Icons.Default.Home // 室内运动
                "outdoor" -> Icons.Default.WbSunny // 户外运动
                // 兼容旧数据
                "attraction" -> Icons.Default.Place
                "dining" -> Icons.Default.Restaurant
                "transport" -> Icons.Default.DirectionsCar
                "accommodation" -> Icons.Default.Hotel
                else -> Icons.Default.LocationOn
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDetailHeader(
        title: String,
        onBackClick: () -> Unit,
        onDownloadClick: () -> Unit,
        onEditClick: () -> Unit
) {
        TopAppBar(
                title = {
                        Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )
                },
                navigationIcon = {
                        IconButton(onClick = onBackClick) {
                                Box(
                                        modifier =
                                                Modifier.size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                ForestGreenLight.copy(alpha = 0.2f)
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.ArrowBack,
                                                contentDescription = "返回",
                                                tint = ForestGreenDark
                                        )
                                }
                        }
                },
                actions = {
                        IconButton(onClick = onDownloadClick) {
                                Box(
                                        modifier =
                                                Modifier.size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                SkyBlueLight.copy(alpha = 0.2f)
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "下载离线包",
                                                tint = SkyBlue
                                        )
                                }
                        }
                        IconButton(onClick = onEditClick) {
                                Box(
                                        modifier =
                                                Modifier.size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                VitalOrangeLight.copy(alpha = 0.2f)
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "编辑",
                                                tint = VitalOrange
                                        )
                                }
                        }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
}

@Composable
private fun WeatherCard(
        temperature: Int,
        weather: String,
        humidity: Int,
        wind: String,
        destination: String = "目的地"
) {
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        ambientColor = SkyBlue.copy(alpha = 0.2f),
                                        spotColor = SkyBlue.copy(alpha = 0.3f)
                                ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        SkyBlue,
                                                                        SkyBlueLight,
                                                                        ForestGreenLight
                                                                )
                                                )
                                        )
                                        .padding(20.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.WbSunny,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(40.dp)
                                        )

                                        Column {
                                                Text(
                                                        text = "$temperature°C",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                )
                                                Text(
                                                        text = "$destination·$weather",
                                                        fontSize = 10.sp,
                                                        color = Color.White.copy(alpha = 0.8f)
                                                )
                                        }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = "湿度 $humidity%",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                                text = wind,
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun DaySection(dayData: DayData) {
        Column {
                // Day 标题
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.width(6.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                ForestGreen,
                                                                                ForestGreenDark
                                                                        )
                                                        )
                                                )
                        )

                        Text(
                                text = "Day ${dayData.dayIndex} · ${dayData.date}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 时间轴
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        dayData.items.forEachIndexed { index, item ->
                                TimelineItem(item = item, isLast = index == dayData.items.size - 1)
                        }
                }
        }
}

@Composable
private fun TimelineItem(item: TripItemData, isLast: Boolean) {
        Row {
                // 时间轴指示器
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                ) {
                        Box(
                                modifier =
                                        Modifier.size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.radialGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color.White,
                                                                                item.color.copy(
                                                                                        alpha = 0.1f
                                                                                )
                                                                        )
                                                        )
                                                )
                                                .border(3.dp, item.color, CircleShape),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = item.color,
                                        modifier = Modifier.size(16.dp)
                                )
                        }

                        if (!isLast) {
                                Box(
                                        modifier =
                                                Modifier.width(3.dp)
                                                        .height(70.dp)
                                                        .background(
                                                                Brush.verticalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        item.color
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                ),
                                                                                        TextTertiary
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                )
                                                                                )
                                                                )
                                                        )
                                )
                        }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 内容卡片
                Card(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .shadow(
                                                elevation = 8.dp,
                                                shape = RoundedCornerShape(18.dp),
                                                ambientColor = item.color.copy(alpha = 0.1f),
                                                spotColor = item.color.copy(alpha = 0.15f)
                                        ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(16.dp)
//                                                .border(
//                                                        width = 0.dp,
//                                                        color = item.color,
//                                                        shape = RoundedCornerShape(16.dp)
//                                                )
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                ) {
                                        Text(
                                                text = item.placeName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                modifier = Modifier.weight(1f)
                                        )

                                        Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = item.color.copy(alpha = 0.15f)
                                        ) {
                                                Text(
                                                        text = item.typeLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = item.color,
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 10.dp,
                                                                        vertical = 4.dp
                                                                )
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        if (item.time != null) {
                                                InfoChip(
                                                        icon = Icons.Default.AccessTime,
                                                        text = item.time
                                                )
                                        }
                                        if (item.cost != null) {
                                                InfoChip(
                                                        icon = Icons.Default.LocalFireDepartment,
                                                        text = item.cost
                                                )
                                        }
                                        if (item.duration != null) {
                                                InfoChip(
                                                        icon = Icons.Default.HourglassEmpty,
                                                        text = item.duration
                                                )
                                        }
                                }

                                if (item.notes != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text = item.notes,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                )
                Text(text = text, fontSize = 10.sp, color = TextSecondary)
        }
}

// 数据模型
data class DayData(val dayIndex: Int, val date: String, val items: List<TripItemData>)

data class TripItemData(
        val placeName: String,
        val placeType: String,
        val typeLabel: String,
        val color: Color,
        val icon: ImageVector,
        val time: String?,
        val cost: String?,
        val duration: String?,
        val notes: String?
)
