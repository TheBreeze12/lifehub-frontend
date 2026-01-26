package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.TripViewModel

/** 首页 - 应用主入口 包含快速操作、今日饮食摘要、近期行程卡片 */
@Composable
fun HomePage(navController: NavController, tripViewModel: TripViewModel = viewModel()) {
        // 检查登录状态
        val isLoggedIn =
                try {
                        UserSession.isLoggedIn()
                } catch (e: Exception) {
                        false
                }

        // 获取用户ID（仅在已登录时）
        val userId =
                try {
                        UserSession.getUserId()
                } catch (e: Exception) {
                        null
                }

        // 仅在已登录时加载首页行程
        LaunchedEffect(isLoggedIn, userId) {
                if (isLoggedIn && userId != null) {
                        try {
                                tripViewModel.getHomeTrips(userId, limit = 3)
                        } catch (e: Exception) {
                                // 静默处理错误，避免崩溃
                        }
                }
        }

        val homeTripsState by tripViewModel.homeTripsState.collectAsState()
        val scrollState = rememberScrollState()

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(BackgroundBeige)
                                .verticalScroll(scrollState)
                                .padding(24.dp)
        ) {
                // 顶部问候语
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column {
                                Text(
                                        text = "早上好👋",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                )
                                Text(text = "欢迎使用智能生活", fontSize = 14.sp, color = Color(0xFF6B7280))
                        }

                        Box(
                                modifier =
                                        Modifier.size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "通知",
                                        tint = ForestGreen,
                                        modifier = Modifier.size(24.dp)
                                )
                        }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 快速操作卡片
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        QuickActionCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.CameraAlt,
                                title = "拍菜单",
                                subtitle = "识别营养成分",
                                gradientColors = listOf(Color(0xFFF472B6), Color(0xFFFDA4AF)),
                                onClick = { navController.navigate(Screen.Camera.route) }
                        )

                        QuickActionCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.TravelExplore,
                                title = "规划行程",
                                subtitle = "AI生成计划",
                                gradientColors = listOf(Color(0xFF34D399), Color(0xFF6EE7B7)),
                                onClick = { navController.navigate(Screen.TripPlanning.route) }
                        )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 今日饮食卡片
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "今日饮食",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                        )
                                        Text(
                                                text = "查看详情 →",
                                                fontSize = 12.sp,
                                                color = ForestGreen,
                                                fontWeight = FontWeight.Medium
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 简化的营养进度条
                                NutritionProgressBar("蛋白质", 0.7f, Color(0xFF10B981))
                                Spacer(modifier = Modifier.height(12.dp))
                                NutritionProgressBar("脂肪", 0.45f, Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.height(12.dp))
                                NutritionProgressBar("碳水", 0.6f, Color(0xFF3B82F6))
                        }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 近期行程卡片
                if (!isLoggedIn) {
                        // 未登录状态：显示提示卡片
                        Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                                Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                                text = "请先登录",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = "登录后即可查看和管理您的行程",
                                                fontSize = 12.sp,
                                                color = Color(0xFF6B7280)
                                        )
                                }
                        }
                } else {
                        when (val state = homeTripsState) {
                                is com.example.lifehub.viewmodel.HomeTripsState.Loading -> {
                                        Box(
                                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                                contentAlignment = Alignment.Center
                                        ) { CircularProgressIndicator(color = ForestGreen) }
                                }
                                is com.example.lifehub.viewmodel.HomeTripsState.Success -> {
                                        if (state.trips.isNotEmpty()) {
                                                // 显示第一个行程
                                                val firstTrip = state.trips[0]
                                                HomeTripCard(
                                                        trip = firstTrip,
                                                        onClick = {
                                                                navController.navigate(
                                                                        Screen.TripDetail
                                                                                .createRoute(
                                                                                        firstTrip
                                                                                                .tripId
                                                                                                .toString()
                                                                                )
                                                                )
                                                        }
                                                )

                                                // 如果有多个行程，显示横向滚动列表
                                                if (state.trips.size > 1) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        LazyRow(
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(12.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                                items(state.trips.drop(1)) { trip ->
                                                                        HomeTripCardSmall(
                                                                                trip = trip,
                                                                                onClick = {
                                                                                        navController
                                                                                                .navigate(
                                                                                                        Screen.TripDetail
                                                                                                                .createRoute(
                                                                                                                        trip.tripId
                                                                                                                                .toString()
                                                                                                                )
                                                                                                )
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else {
                                                // 没有行程时显示空状态
                                                EmptyTripCard(
                                                        onClick = {
                                                                navController.navigate(
                                                                        Screen.TripPlanning.route
                                                                )
                                                        }
                                                )
                                        }
                                }
                                is com.example.lifehub.viewmodel.HomeTripsState.Error -> {
                                        // 错误时显示空状态，点击可跳转到规划页面
                                        EmptyTripCard(
                                                onClick = {
                                                        navController.navigate(
                                                                Screen.TripPlanning.route
                                                        )
                                                }
                                        )
                                }
                                else -> {
                                        EmptyTripCard(
                                                onClick = {
                                                        navController.navigate(
                                                                Screen.TripPlanning.route
                                                        )
                                                }
                                        )
                                }
                        }
                }
        }
}

/** 快速操作卡片组件 */
@Composable
fun QuickActionCard(
        modifier: Modifier = Modifier,
        icon: ImageVector,
        title: String,
        subtitle: String,
        gradientColors: List<Color>,
        onClick: () -> Unit
) {
        Card(
                modifier = modifier.clickable(onClick = onClick),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                                modifier =
                                        Modifier.size(48.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(gradientColors)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        icon,
                                        contentDescription = title,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                        )
                        Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF9CA3AF))
                }
        }
}

/** 营养进度条组件 */
@Composable
fun NutritionProgressBar(label: String, progress: Float, color: Color) {
        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Text(text = label, fontSize = 12.sp, color = Color(0xFF6B7280))
                        Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                        )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                        progress = progress,
                        modifier =
                                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = Color(0xFFF3F4F6)
                )
        }
}

/** 首页行程卡片（大卡片） */
@Composable
fun HomeTripCard(trip: com.example.lifehub.data.TripSummary, onClick: () -> Unit) {
        Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreen)
        ) {
                Box {
                        Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                        text = trip.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                )
                                Text(
                                        text = "${trip.startDate} - ${trip.endDate}",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        trip.destination?.let {
                                                TripInfoChip(Icons.Default.Place, it)
                                        }
                                        TripInfoChip(Icons.Default.List, "${trip.itemCount}个节点")
                                }
                        }

                        // 计算天数
                        val days = calculateDays(trip.startDate, trip.endDate)
                        if (days > 0) {
                                Surface(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                ) {
                                        Text(
                                                text = "${days}天",
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 12.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }
                        }
                }
        }
}

/** 首页行程卡片（小卡片） */
@Composable
fun HomeTripCardSmall(trip: com.example.lifehub.data.TripSummary, onClick: () -> Unit) {
        Card(
                modifier = Modifier.width(200.dp).clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreen)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = trip.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = "${trip.startDate} - ${trip.endDate}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                        )
                }
        }
}

/** 空行程卡片 */
@Composable
fun EmptyTripCard(onClick: () -> Unit) {
        Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
                Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Icon(
                                Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                                text = "还没有行程计划",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "点击开始规划", fontSize = 12.sp, color = ForestGreen)
                }
        }
}

/** 计算天数 */
fun calculateDays(startDate: String, endDate: String): Int {
        return try {
                val start = java.time.LocalDate.parse(startDate)
                val end = java.time.LocalDate.parse(endDate)
                java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
        } catch (e: Exception) {
                0
        }
}

/** 行程信息标签组件 */
@Composable
fun TripInfoChip(icon: ImageVector, text: String) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                )
                Text(text = text, fontSize = 12.sp, color = Color.White)
        }
}
