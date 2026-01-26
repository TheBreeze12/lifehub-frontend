package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** 所有行程列表页 - 显示用户所有行程 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListPage(navController: NavController, tripViewModel: TripViewModel = viewModel()) {
    // 获取用户ID并加载行程列表
    val userId =
            try {
                UserSession.getUserId()
            } catch (e: Exception) {
                null
            }

    LaunchedEffect(userId) { userId?.let { tripViewModel.getTripList(it) } }

    val tripListState by tripViewModel.tripListState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundBeige)) {
        // 顶部工具栏
        TopAppBar(
                title = { Text(text = "我的行程", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { userId?.let { tripViewModel.getTripList(it) } }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // 内容区域
        when (val state = tripListState) {
            is com.example.lifehub.viewmodel.TripListState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreen)
                }
            }
            is com.example.lifehub.viewmodel.TripListState.Success -> {
                if (state.trips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(64.dp)
                            )
                            Text(
                                    text = "还没有行程计划",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                            )
                            Text(text = "开始规划您的第一次旅行吧", fontSize = 14.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                    onClick = { navController.navigate(Screen.TripPlanning.route) },
                                    colors =
                                            ButtonDefaults.buttonColors(
                                                    containerColor = ForestGreen
                                            )
                            ) { Text("开始规划") }
                        }
                    }
                } else {
                    LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.trips) { trip ->
                            // 复用出行界面的卡片样式
                            TripItem(
                                    title = trip.title,
                                    destination = trip.destination,
                                    dateRange = "${trip.startDate} - ${trip.endDate}",
                                    itemCount = trip.itemCount,
                                    onClick = {
                                        navController.navigate(
                                                Screen.TripDetail.createRoute(
                                                        trip.tripId.toString()
                                                )
                                        )
                                    }
                            )
                        }
                    }
                }
            }
            is com.example.lifehub.viewmodel.TripListState.Error -> {
                Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp
                        )
                        Button(onClick = { userId?.let { tripViewModel.getTripList(it) } }) {
                            Text("重试")
                        }
                    }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreen)
                }
            }
        }
    }
}

/** 行程卡片组件 - 复用出行界面的卡片样式 */
@Composable
fun TripItem(
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
                                            destination?.let { append(it) }
                                            if (destination != null && dateRange != null)
                                                    append(" · ")
                                            dateRange?.let { append(it) }
                                        },
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                        )
                    }
                    if (itemCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$itemCount 个节点", fontSize = 10.sp, color = ForestGreen)
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
