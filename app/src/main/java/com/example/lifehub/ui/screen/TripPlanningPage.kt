package com.example.lifehub.ui.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.TripViewModel

/** 行程规划页 - MVP版本 用户输入出行需求，AI生成行程 */
@Composable
fun TripPlanningPage(navController: NavController, tripViewModel: TripViewModel = viewModel()) {
        var inputText by remember { mutableStateOf("") }
        val scrollState = rememberScrollState()

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

        // 仅在已登录时加载最近行程
        LaunchedEffect(isLoggedIn, userId) {
                if (isLoggedIn && userId != null) {
                        tripViewModel.getRecentTrips(userId, limit = 5)
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
                                                        // 调用后端API生成行程
                                                        tripViewModel.generateTrip(
                                                                userId = userId,
                                                                query = inputText,
                                                                preferences = null // 可以从用户设置中获取
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
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                                Icon(
                                                        Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = Color(0xFF9CA3AF),
                                                        modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text = "请先登录以查看最近规划",
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

@Composable
private fun TripPlanningHeader(onBackClick: () -> Unit = {}) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(100.dp)
                                .background(
                                        Brush.linearGradient(
                                                colors =
                                                        listOf(Color(0xFF10B981), Color(0xFF059669))
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
                                modifier = Modifier.size(28.dp)
                        )
                }

                // 标题和标签
                Column(
                        modifier = Modifier.align(Alignment.Center).padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        Text(
                                text = "智能行程规划",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(text = "AI 生成")
                                Chip(text = "实时路况")
                        }
                }
        }
}

@Composable
private fun Chip(text: String) {
        Box(
                modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) { Text(text = text, fontSize = 10.sp, color = Color.White) }
}

@Composable
private fun InputCard(
        inputText: String,
        onInputChange: (String) -> Unit,
        isLoading: Boolean,
        onGenerateClick: () -> Unit
) {
        Card(
                modifier = Modifier.fillMaxWidth().offset(y = (-40).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // 麦克风图标（装饰用）
                        Box(
                                modifier =
                                        Modifier.size(80.dp)
                                                .clip(CircleShape)
                                                .background(ForestGreen),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "语音输入",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "点击说出您的出行需求", fontSize = 14.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(12.dp))

                        // 文本输入框
                        OutlinedTextField(
                                value = inputText,
                                onValueChange = onInputChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                        Text(
                                                text = "规划周末亲子游...",
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
                                text = "周末亲子游",
                                color = Color(0xFF10B981),
                                onClick = { onRecommendationClick("周末亲子游") }
                        )
                        RecommendationChip(
                                text = "三日自驾游",
                                color = Color(0xFF3B82F6),
                                onClick = { onRecommendationClick("三日自驾游") }
                        )
                        RecommendationChip(
                                text = "情侣约会",
                                color = Color(0xFFEC4899),
                                onClick = { onRecommendationClick("情侣约会") }
                        )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 第二行标签
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecommendationChip(
                                text = "毕业旅行",
                                color = Color(0xFFF59E0B),
                                onClick = { onRecommendationClick("毕业旅行") }
                        )
                        RecommendationChip(
                                text = "露营野餐",
                                color = Color(0xFF8B5CF6),
                                onClick = { onRecommendationClick("露营野餐") }
                        )
                }
        }
}

@Composable
private fun RecommendationChip(text: String, color: Color, onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(
                                        width = 1.dp,
                                        color = color.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                )
                                .clickable(onClick = onClick)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Text(text = text, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium) }
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
                                text = "最近规划",
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
                                                text = "暂无最近行程",
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
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                                                Modifier.size(40.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(ForestGreen.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Map,
                                                contentDescription = null,
                                                tint = ForestGreen,
                                                modifier = Modifier.size(20.dp)
                                        )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
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
                                                        fontSize = 11.sp,
                                                        color = TextSecondary,
                                                        maxLines = 1
                                                )
                                        }
                                        if (itemCount > 0) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                        text = "$itemCount 个节点",
                                                        fontSize = 10.sp,
                                                        color = ForestGreen
                                                )
                                        }
                                }
                        }

                        Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "查看详情",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                        )
                }
        }
}
