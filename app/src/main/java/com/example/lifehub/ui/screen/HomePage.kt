package com.example.lifehub.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.shadow
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
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.TripViewModel

/** 首页 - 应用主入口 包含快速操作、今日饮食摘要、近期运动计划卡片 */
@Composable
fun HomePage(
        navController: NavController,
        tripViewModel: TripViewModel = viewModel(),
        foodViewModel: FoodViewModel = viewModel()
) {
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

        // 仅在已登录时加载首页行程和今日饮食记录
        LaunchedEffect(isLoggedIn, userId) {
                if (isLoggedIn && userId != null) {
                        try {
                                tripViewModel.getHomeTrips(userId, limit = 3)
                                foodViewModel.getTodayDietRecords(userId)
                        } catch (e: Exception) {
                                // 静默处理错误，避免崩溃
                        }
                }
        }

        val homeTripsState by tripViewModel.homeTripsState.collectAsState()
        val todayDietRecordsState by foodViewModel.todayDietRecordsState.collectAsState()

        // 计算今日营养统计
        val nutritionStats =
                remember(todayDietRecordsState) {
                        when (val state = todayDietRecordsState) {
                                is com.example.lifehub.viewmodel.DietRecordsState.Success -> {
                                        // 获取今日的记录（通常只有一条日期记录）
                                        val todayRecords = state.records.values.flatten()

                                        // 计算总和
                                        val totalProtein =
                                                todayRecords
                                                        .fold(0.0) { acc, record ->
                                                                acc + record.protein
                                                        }
                                                        .toFloat()
                                        val totalFat =
                                                todayRecords
                                                        .fold(0.0) { acc, record ->
                                                                acc + record.fat
                                                        }
                                                        .toFloat()
                                        val totalCarbs =
                                                todayRecords
                                                        .fold(0.0) { acc, record ->
                                                                acc + record.carbs
                                                        }
                                                        .toFloat()

                                        // 设置目标值（可以根据用户偏好调整，这里使用默认值）
                                        val targetProtein = 80f // 80g蛋白质
                                        val targetFat = 60f // 60g脂肪
                                        val targetCarbs = 200f // 200g碳水化合物

                                        Triple(
                                                totalProtein / targetProtein.coerceAtLeast(1f),
                                                totalFat / targetFat.coerceAtLeast(1f),
                                                totalCarbs / targetCarbs.coerceAtLeast(1f)
                                        )
                                }
                                else -> Triple(0f, 0f, 0f)
                        }
                }
        val scrollState = rememberScrollState()

        // 动画效果
        val infiniteTransition = rememberInfiniteTransition(label = "greeting")
        val greetingAlpha by
                infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(2000),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "greetingAlpha"
                )

        // 获取当前时间段的问候语
        val greeting = remember {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when {
                        hour < 6 -> "夜深了 🌙"
                        hour < 9 -> "早上好 ☀️"
                        hour < 12 -> "上午好 🌤️"
                        hour < 14 -> "中午好 🌞"
                        hour < 18 -> "下午好 🌅"
                        hour < 22 -> "晚上好 🌆"
                        else -> "夜深了 🌙"
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
                // 渐变背景
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(
                                                brush =
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                BackgroundGradientStart,
                                                                                BackgroundBeige,
                                                                                BackgroundGradientEnd
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.3f
                                                                                        )
                                                                        )
                                                        )
                                        )
                )

                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 20.dp)
                                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                        // 顶部问候语
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column {
                                        Text(
                                                text = greeting,
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary.copy(alpha = greetingAlpha)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = "开启健康美好的一天",
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                        )
                                }

                                // 通知按钮
                                Surface(
                                        modifier = Modifier.size(44.dp),
                                        shape = CircleShape,
                                        color = ForestGreenLight.copy(alpha = 0.3f),
                                        shadowElevation = 2.dp
                                ) {
                                        Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                        Icons.Default.Notifications,
                                                        contentDescription = "通知",
                                                        tint = ForestGreenDark,
                                                        modifier = Modifier.size(22.dp)
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 快速操作卡片
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                                QuickActionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.CameraAlt,
                                        title = "拍菜单",
                                        subtitle = "智能识别营养",
                                        gradientColors = listOf(CoralPink, VitalOrangeLight),
                                        onClick = { navController.navigate(Screen.Camera.route) }
                                )

                                QuickActionCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.DirectionsRun,
                                        title = "运动规划",
                                        subtitle = "AI生成计划",
                                        gradientColors = listOf(ForestGreen, ForestGreenLight),
                                        onClick = {
                                                navController.navigate(Screen.TripPlanning.route)
                                        }
                                )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 餐前餐后对比入口卡片 (Phase 13)
                        QuickActionCard(
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.CompareArrows,
                                title = "餐前餐后对比",
                                subtitle = "精准计算实际摄入量",
                                gradientColors = listOf(LavenderPurple, SkyBlueLight),
                                onClick = { navController.navigate(Screen.MealComparison.route) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 今日饮食卡片
                        Card(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .shadow(
                                                        elevation = 8.dp,
                                                        shape = RoundedCornerShape(20.dp),
                                                        ambientColor =
                                                                ForestGreen.copy(alpha = 0.1f),
                                                        spotColor = ForestGreen.copy(alpha = 0.1f)
                                                ),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(32.dp)
                                                                                .clip(
                                                                                        RoundedCornerShape(
                                                                                                8.dp
                                                                                        )
                                                                                )
                                                                                .background(
                                                                                        ForestGreenLight
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                )
                                                                                ),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Restaurant,
                                                                        contentDescription = null,
                                                                        tint = ForestGreenDark,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                                text = "今日饮食",
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary
                                                        )
                                                }
                                                Text(
                                                        text = "查看详情 →",
                                                        fontSize = 12.sp,
                                                        color = ForestGreen,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier =
                                                                Modifier.clickable {
                                                                        navController.navigate(
                                                                                com.example.lifehub
                                                                                        .navigation
                                                                                        .Screen
                                                                                        .TodayDietRecords
                                                                                        .route
                                                                        )
                                                                }
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 营养进度条
                                        NutritionProgressBar(
                                                "蛋白质",
                                                nutritionStats.first.coerceIn(0f, 1f),
                                                ProteinColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        NutritionProgressBar(
                                                "脂肪",
                                                nutritionStats.second.coerceIn(0f, 1f),
                                                FatColor
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        NutritionProgressBar(
                                                "碳水",
                                                nutritionStats.third.coerceIn(0f, 1f),
                                                CarbsColor
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 近期运动计划卡片
                        if (!isLoggedIn) {
                                // 未登录状态：显示提示卡片
                                Card(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .shadow(
                                                                elevation = 6.dp,
                                                                shape = RoundedCornerShape(20.dp),
                                                                ambientColor =
                                                                        LavenderPurple.copy(
                                                                                alpha = 0.1f
                                                                        )
                                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor = Color.White
                                                )
                                ) {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        vertical = 48.dp,
                                                                        horizontal = 24.dp
                                                                ),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                        ) {
                                                Box(
                                                        modifier =
                                                                Modifier.size(56.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                                Brush.linearGradient(
                                                                                        colors =
                                                                                                listOf(
                                                                                                        SkyBlueLight
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.5f
                                                                                                                ),
                                                                                                        LavenderPurple
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.5f
                                                                                                                )
                                                                                                )
                                                                                )
                                                                        ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Icon(
                                                                Icons.Default.Lock,
                                                                contentDescription = null,
                                                                tint = LavenderPurple,
                                                                modifier = Modifier.size(28.dp)
                                                        )
                                                }
                                                Spacer(modifier = Modifier.height(14.dp))
                                                Text(
                                                        text = "请先登录",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                        text = "登录后即可查看和管理您的运动计划",
                                                        fontSize = 13.sp,
                                                        color = TextSecondary
                                                )
                                        }
                                }
                        } else {
                                when (val state = homeTripsState) {
                                        is com.example.lifehub.viewmodel.HomeTripsState.Loading -> {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(120.dp),
                                                        contentAlignment = Alignment.Center
                                                ) { CircularProgressIndicator(color = ForestGreen) }
                                        }
                                        is com.example.lifehub.viewmodel.HomeTripsState.Success -> {
                                                if (state.trips.isNotEmpty()) {
                                                        // 显示第一个运动计划
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

                                                        // 如果有多个运动计划，显示横向滚动列表
                                                        if (state.trips.size > 1) {
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                LazyRow(
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                12.dp
                                                                                        ),
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) {
                                                                        items(
                                                                                state.trips.drop(1)
                                                                        ) { trip ->
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
                                                        // 没有运动计划时显示空状态
                                                        EmptyTripCard(
                                                                onClick = {
                                                                        navController.navigate(
                                                                                Screen.TripPlanning
                                                                                        .route
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
                modifier =
                        modifier.shadow(
                                        elevation = 10.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = gradientColors.first().copy(alpha = 0.2f),
                                        spotColor = gradientColors.first().copy(alpha = 0.3f)
                                )
                                .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                                modifier =
                                        Modifier.size(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Brush.linearGradient(gradientColors)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        icon,
                                        contentDescription = title,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
                }
        }
}

/** 营养进度条组件 */
@Composable
fun NutritionProgressBar(label: String, progress: Float, color: Color) {
        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                        modifier =
                                                Modifier.size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                        Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                        )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                        progress = { progress },
                        modifier =
                                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = color.copy(alpha = 0.15f)
                )
        }
}

/** 首页运动计划卡片（大卡片） */
@Composable
fun HomeTripCard(trip: com.example.lifehub.data.TripSummary, onClick: () -> Unit) {
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = ForestGreen.copy(alpha = 0.3f),
                                        spotColor = ForestGreen.copy(alpha = 0.4f)
                                )
                                .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(ForestGreen, ForestGreenDark)
                                                )
                                        )
                ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        text = trip.title,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                        text =
                                                                "${trip.startDate} - ${trip.endDate}",
                                                        fontSize = 13.sp,
                                                        color = Color.White.copy(alpha = 0.85f)
                                                )
                                        }

                                        // 计算天数
                                        val days = calculateDays(trip.startDate, trip.endDate)
                                        if (days > 0) {
                                                Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color.White.copy(alpha = 0.2f)
                                                ) {
                                                        Text(
                                                                text = "${days}天",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color.White,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 12.dp,
                                                                                vertical = 6.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        trip.destination?.let {
                                                TripInfoChip(Icons.Default.Place, it)
                                        }
                                        TripInfoChip(
                                                Icons.Default.FitnessCenter,
                                                "${trip.itemCount}个节点"
                                        )
                                }
                        }
                }
        }
}

/** 首页运动计划卡片（小卡片） */
@Composable
fun HomeTripCardSmall(trip: com.example.lifehub.data.TripSummary, onClick: () -> Unit) {
        Card(
                modifier =
                        Modifier.width(180.dp)
                                .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = ForestGreen.copy(alpha = 0.2f)
                                )
                                .clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        ForestGreenLight,
                                                                        ForestGreen
                                                                )
                                                )
                                        )
                                        .padding(14.dp)
                ) {
                        Column {
                                Text(
                                        text = trip.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                        text = "${trip.startDate} - ${trip.endDate}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                )
                        }
                }
        }
}

/** 空运动计划卡片 */
@Composable
fun EmptyTripCard(onClick: () -> Unit) {
        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        ambientColor = ForestGreen.copy(alpha = 0.1f)
                                )
                                .clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Box(
                                modifier =
                                        Modifier.size(64.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                ForestGreenLight
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.4f
                                                                                        ),
                                                                                ForestGreen.copy(
                                                                                        alpha = 0.3f
                                                                                )
                                                                        )
                                                        )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(32.dp)
                                )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                                text = "还没有运动计划",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                                text = "点击开始规划 →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = ForestGreen
                        )
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
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.15f)) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                        )
                        Text(
                                text = text,
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                        )
                }
        }
}
