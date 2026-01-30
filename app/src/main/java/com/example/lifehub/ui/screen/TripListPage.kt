package com.example.lifehub.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

/** 所有运动计划列表页 - 显示用户所有运动计划 */
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

    // 每次进入页面时重新加载列表
    LaunchedEffect(userId) {
        userId?.let {
            // 先重置状态，确保清除旧数据
            tripViewModel.resetTripListState()
            tripViewModel.getTripList(it)
        }
    }

    val tripListState by tripViewModel.tripListState.collectAsState()

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(
                            brush = Brush.verticalGradient(
                                    colors = listOf(
                                            Color.White,
                                            BackgroundGradientStart,
                                            BackgroundBeige
                                    )
                            )
                    )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部工具栏
            TopAppBar(
                    title = {
                        Text(
                                text = "我的运动计划",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Box(
                                    modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ForestGreenLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "返回",
                                        tint = ForestGreenDark
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { userId?.let { tripViewModel.getTripList(it) } }) {
                            Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新",
                                    tint = ForestGreen
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                    )
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
                                    modifier = Modifier.padding(24.dp)
                            ) {
                                Box(
                                        modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors = listOf(
                                                                        ForestGreenLight.copy(alpha = 0.4f),
                                                                        ForestGreen.copy(alpha = 0.3f)
                                                                )
                                                        )
                                                ),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.DirectionsRun,
                                            contentDescription = null,
                                            tint = ForestGreen,
                                            modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                        text = "还没有运动计划",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "开始规划您的第一次运动吧",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                        onClick = { navController.navigate(Screen.TripPlanning.route) },
                                        modifier = Modifier
                                                .shadow(
                                                        elevation = 6.dp,
                                                        shape = RoundedCornerShape(14.dp),
                                                        ambientColor = ForestGreen.copy(alpha = 0.3f)
                                                ),
                                        colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                            modifier = Modifier
                                                    .background(
                                                            brush = Brush.horizontalGradient(
                                                                    colors = listOf(ForestGreen, ForestGreenDark)
                                                            )
                                                    )
                                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                                "开始运动规划",
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.trips) { trip ->
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
                            Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(48.dp)
                            )
                            Text(
                                    text = state.message,
                                    color = ErrorRed,
                                    fontSize = 14.sp
                            )
                            Button(
                                    onClick = { userId?.let { tripViewModel.getTripList(it) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                            ) {
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
}

/** 行程卡片组件 - 清新灵动的卡片样式 */
@Composable
fun TripItem(
        title: String,
        destination: String? = null,
        dateRange: String? = null,
        itemCount: Int = 0,
        onClick: () -> Unit
) {
    Card(
            modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = ForestGreen.copy(alpha = 0.08f),
                            spotColor = ForestGreen.copy(alpha = 0.1f)
                    )
                    .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
            ) {
                Box(
                        modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                        Brush.linearGradient(
                                                colors = listOf(
                                                        ForestGreenLight.copy(alpha = 0.3f),
                                                        ForestGreen.copy(alpha = 0.2f)
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1
                    )
                    if (destination != null || dateRange != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                                text = buildString {
                                    destination?.let { append(it) }
                                    if (destination != null && dateRange != null) append(" · ")
                                    dateRange?.let { append(it) }
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
                                    fontSize = 11.sp,
                                    color = ForestGreenDark,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Box(
                    modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ForestGreenLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "查看详情",
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
