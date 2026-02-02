package com.example.lifehub.ui.screen

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.TripViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 运动规划页（餐后运动规划）- MVP版本 用户输入运动需求，AI生成运动计划 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TripPlanningPage(navController: NavController, tripViewModel: TripViewModel = viewModel()) {
        var inputText by remember { mutableStateOf("") }
        val scrollState = rememberScrollState()
        val context = LocalContext.current

        // 位置权限
        val locationPermissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        listOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                } else {
                        listOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                }
        val locationPermissionsState = rememberMultiplePermissionsState(locationPermissions)

        // 用户位置状态
        var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

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

        // 仅在已登录时加载最近行程（每次进入页面时重新加载）
        LaunchedEffect(isLoggedIn, userId) {
                if (isLoggedIn && userId != null) {
                        // 先重置状态，确保清除旧数据
                        tripViewModel.resetRecentTripsState()
                        tripViewModel.getRecentTrips(userId, limit = 5)
                }
        }

        // 获取用户位置
        LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
                if (locationPermissionsState.allPermissionsGranted) {
                        try {
                                val location = getCurrentLocation(context)
                                location?.let { userLocation = Pair(it.latitude, it.longitude) }
                        } catch (e: Exception) {
                                // 获取位置失败，静默处理
                                println("获取位置失败: ${e.message}")
                        }
                }
        }

        val generateTripState by tripViewModel.generateTripState.collectAsState()
        val recentTripsState by tripViewModel.recentTripsState.collectAsState()

        // 监听生成行程结果
        LaunchedEffect(generateTripState) {
                when (val state = generateTripState) {
                        is com.example.lifehub.viewmodel.GenerateTripState.Success -> {
                                // 生成成功，跳转到行程详情页
                                navController.navigate(
                                        Screen.TripDetail.createRoute(
                                                state.tripPlan.tripId.toString()
                                        )
                                )
                                tripViewModel.resetGenerateTripState()
                        }
                        else -> {}
                }
        }

        Column(modifier = Modifier.fillMaxSize().background(BackgroundBeige)) {
                // 顶部绿色区域（固定不滚动）
                TripPlanningHeader(onBackClick = { navController.navigateUp() })

                // 主要内容区域（可滚动）
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(horizontal = 24.dp)
                                        .padding(bottom = 24.dp)
                ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 输入卡片
                        InputCard(
                                inputText = inputText,
                                onInputChange = { inputText = it },
                                isLoading =
                                        generateTripState is
                                                com.example.lifehub.viewmodel.GenerateTripState.Loading,
                                onGenerateClick = {
                                        if (inputText.isNotBlank()) {
                                                if (isLoggedIn && userId != null) {
                                                        // 如果还没有位置权限，先请求权限
                                                        if (!locationPermissionsState
                                                                        .allPermissionsGranted
                                                        ) {
                                                                locationPermissionsState
                                                                        .launchMultiplePermissionRequest()
                                                                // 权限请求后，位置会在LaunchedEffect中自动获取
                                                                // 这里先不生成计划，等待用户再次点击
                                                                return@InputCard
                                                        }

                                                        // 调用后端API生成运动计划，传递位置信息
                                                        // 如果没有位置信息，传递null，后端会使用默认值
                                                        tripViewModel.generateTrip(
                                                                userId = userId,
                                                                query = inputText,
                                                                preferences = null, // 可以从用户设置中获取
                                                                latitude = userLocation?.first,
                                                                longitude = userLocation?.second
                                                        )
                                                } else {
                                                        // 未登录，跳转到登录页面
                                                        navController.navigate(Screen.Login.route)
                                                }
                                        }
                                }
                        )

                        // 显示错误信息
                        if (generateTripState is
                                        com.example.lifehub.viewmodel.GenerateTripState.Error
                        ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text =
                                                (generateTripState as
                                                                com.example.lifehub.viewmodel.GenerateTripState.Error)
                                                        .message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 热门推荐
                        HotRecommendations(onRecommendationClick = { text -> inputText = text })

                        Spacer(modifier = Modifier.height(24.dp))

                        // 最近规划（仅在已登录时显示）
                        if (isLoggedIn && userId != null) {
                                RecentTrips(navController, recentTripsState, tripViewModel, userId)
                        } else {
                                // 未登录提示
                                Spacer(modifier = Modifier.height(24.dp))
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
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
                                                Icon(
                                                        Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = Color(0xFF9CA3AF),
                                                        modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text = "请先登录以查看最近运动计划",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF6B7280)
                                                )
                                        }
                                }
                        }

                        // 底部留白
                        Spacer(modifier = Modifier.height(16.dp))
                }
        }
}

/** 获取当前位置 使用FusedLocationProviderClient获取用户当前位置 */
@Suppress("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
        return withContext(Dispatchers.IO) {
                try {
                        val fusedLocationClient =
                                LocationServices.getFusedLocationProviderClient(context)

                        // 先尝试获取最后一次已知位置（更快）
                        val lastLocation = fusedLocationClient.lastLocation.await()

                        // 检查位置是否有效（1分钟内的位置认为有效）
                        if (lastLocation != null) {
                                val locationTime = lastLocation.time
                                val currentTime = System.currentTimeMillis()
                                val timeDiff = currentTime - locationTime
                                if (timeDiff < 60000 && timeDiff >= 0) { // 1分钟内的位置认为有效
                                        return@withContext lastLocation
                                }
                        }

                        // 如果最后一次位置不可用或太旧，请求当前位置
                        val locationResult =
                                fusedLocationClient
                                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .await()

                        locationResult
                } catch (e: SecurityException) {
                        println("位置权限未授予: ${e.message}")
                        null
                } catch (e: Exception) {
                        println("获取位置异常: ${e.message}")
                        null
                }
        }
}

@Composable
private fun TripPlanningHeader(onBackClick: () -> Unit = {}) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(120.dp)
                                .background(
                                        Brush.linearGradient(
                                                colors = listOf(ForestGreen, ForestGreenDark)
                                        )
                                )
        ) {
                // 返回按钮
                IconButton(
                        onClick = onBackClick,
                        modifier =
                                Modifier.padding(16.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .align(Alignment.TopStart)
                ) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "返回",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                        )
                }

                // 标题和标签
                Column(
                        modifier = Modifier.align(Alignment.Center).padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                text = "餐后运动规划",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Chip(text = "✨ AI 生成")
                                Chip(text = "💚 健康管理")
                        }
                }
        }
}

@Composable
private fun Chip(text: String) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f)) {
                Text(
                        text = text,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
        }
}

@Composable
private fun InputCard(
        inputText: String,
        onInputChange: (String) -> Unit,
        isLoading: Boolean,
        onGenerateClick: () -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth().offset(y = (-50).dp).padding(horizontal = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // 麦克风图标（装饰用）- 带渐变效果
                        Box(
                                modifier =
                                        Modifier.size(76.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                ForestGreen,
                                                                                ForestGreenDark
                                                                        )
                                                        )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "语音输入",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                                text = "点击说出您的运动需求",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 文本输入框
                        OutlinedTextField(
                                value = inputText,
                                onValueChange = onInputChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                        Text(
                                                text = "规划餐后运动，消耗300卡路里...",
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                        )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ForestGreen,
                                                unfocusedBorderColor = Color(0xFFE5E7EB)
                                        ),
                                trailingIcon = {
                                        if (isLoading) {
                                                CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = ForestGreen,
                                                        strokeWidth = 2.dp
                                                )
                                        } else {
                                                Button(
                                                        onClick = onGenerateClick,
                                                        enabled = inputText.isNotBlank(),
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                VitalOrange,
                                                                        disabledContainerColor =
                                                                                Color.Gray
                                                                ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding =
                                                                PaddingValues(
                                                                        horizontal = 16.dp,
                                                                        vertical = 8.dp
                                                                )
                                                ) {
                                                        Text(
                                                                text = "生成",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }
                        )
                }
        }
}

@Composable
private fun HotRecommendations(onRecommendationClick: (String) -> Unit) {
        Column {
                Text(
                        text = "热门推荐",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 第一行标签
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecommendationChip(
                                text = "餐后散步",
                                color = Color(0xFF10B981),
                                onClick = { onRecommendationClick("餐后散步30分钟") }
                        )
                        RecommendationChip(
                                text = "慢跑30分钟",
                                color = Color(0xFF3B82F6),
                                onClick = { onRecommendationClick("慢跑30分钟") }
                        )
                        RecommendationChip(
                                text = "公园健走",
                                color = Color(0xFFEC4899),
                                onClick = { onRecommendationClick("公园健走") }
                        )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 第二行标签
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecommendationChip(
                                text = "骑行路线",
                                color = Color(0xFFF59E0B),
                                onClick = { onRecommendationClick("骑行路线") }
                        )
                        RecommendationChip(
                                text = "户外运动",
                                color = Color(0xFF8B5CF6),
                                onClick = { onRecommendationClick("户外运动") }
                        )
                }
        }
}

@Composable
private fun RecommendationChip(text: String, color: Color, onClick: () -> Unit) {
        Surface(
                modifier = Modifier.clickable(onClick = onClick),
                shape = RoundedCornerShape(20.dp),
                color = color.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
                Text(
                        text = text,
                        fontSize = 13.sp,
                        color = color,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
        }
}

@Composable
private fun RecentTrips(
        navController: NavController,
        recentTripsState: com.example.lifehub.viewmodel.RecentTripsState,
        tripViewModel: TripViewModel,
        userId: Int
) {
        Column {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = "最近运动计划",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )

                        TextButton(onClick = { tripViewModel.getRecentTrips(userId) }) {
                                Text(text = "刷新", fontSize = 10.sp, color = TextSecondary)
                        }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (recentTripsState) {
                        is com.example.lifehub.viewmodel.RecentTripsState.Loading -> {
                                Box(
                                        modifier = Modifier.fillMaxWidth().height(60.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                        CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = ForestGreen,
                                                strokeWidth = 2.dp
                                        )
                                }
                        }
                        is com.example.lifehub.viewmodel.RecentTripsState.Success -> {
                                if (recentTripsState.trips.isEmpty()) {
                                        Text(
                                                text = "暂无最近运动计划",
                                                fontSize = 12.sp,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                } else {
                                        recentTripsState.trips.forEach { trip ->
                                                RecentTripItem(
                                                        title = trip.title,
                                                        destination = trip.destination,
                                                        dateRange =
                                                                "${trip.startDate} - ${trip.endDate}",
                                                        itemCount = trip.itemCount,
                                                        onClick = {
                                                                navController.navigate(
                                                                        Screen.TripDetail
                                                                                .createRoute(
                                                                                        trip.tripId
                                                                                                .toString()
                                                                                )
                                                                )
                                                        }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                        }
                                }
                        }
                        is com.example.lifehub.viewmodel.RecentTripsState.Error -> {
                                Text(
                                        text = recentTripsState.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                )
                        }
                        else -> {}
                }
        }
}

@Composable
fun RecentTripItem(
        title: String,
        destination: String? = null,
        dateRange: String? = null,
        itemCount: Int = 0,
        onClick: () -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(
                                                                Brush.linearGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        ForestGreenLight
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                ),
                                                                                        ForestGreen
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.2f
                                                                                                )
                                                                                )
                                                                )
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                tint = ForestGreen,
                                                modifier = Modifier.size(22.dp)
                                        )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1
                                        )
                                        if (destination != null || dateRange != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                        text =
                                                                buildString {
                                                                        destination?.let {
                                                                                append(it)
                                                                        }
                                                                        if (destination != null &&
                                                                                        dateRange !=
                                                                                                null
                                                                        )
                                                                                append(" · ")
                                                                        dateRange?.let {
                                                                                append(it)
                                                                        }
                                                                },
                                                        fontSize = 12.sp,
                                                        color = TextSecondary,
                                                        maxLines = 1
                                                )
                                        }
                                        if (itemCount > 0) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = ForestGreenLight.copy(alpha = 0.2f)
                                                ) {
                                                        Text(
                                                                text = "$itemCount 个节点",
                                                                fontSize = 10.sp,
                                                                color = ForestGreenDark,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 8.dp,
                                                                                vertical = 3.dp
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        }

                        Box(
                                modifier =
                                        Modifier.size(30.dp)
                                                .clip(CircleShape)
                                                .background(ForestGreenLight.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "查看详情",
                                        tint = ForestGreen,
                                        modifier = Modifier.size(18.dp)
                                )
                        }
                }
        }
}
