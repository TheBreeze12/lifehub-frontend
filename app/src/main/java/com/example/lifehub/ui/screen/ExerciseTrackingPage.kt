package com.example.lifehub.ui.screen

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.lifehub.data.ExerciseTrackingState
import com.example.lifehub.data.ExerciseTrackingUtils
import com.example.lifehub.navigation.Screen
import com.example.lifehub.ui.components.AMapComposeView
import com.example.lifehub.ui.components.LatLngPoint
import com.example.lifehub.ui.components.PolylineData
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.ExerciseViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 运动轨迹追踪页面 - Phase 27
 *
 * 功能：
 * - 使用GPS持续记录位置
 * - 在地图上实时绘制轨迹
 * - 显示实时配速、距离、时间
 * - 支持开始/暂停/恢复/停止
 *
 * @param navController 导航控制器
 * @param planId 关联运动计划ID（可选，从TripDetailPage跳转时传入）
 * @param exerciseType 运动类型（默认walking）
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTrackingPage(
    navController: NavController,
    planId: Int? = null,
    exerciseType: String = "walking",
    exerciseViewModel: ExerciseViewModel = viewModel()
) {
    val trackingState by exerciseViewModel.trackingState.collectAsState()
    val trackingData by exerciseViewModel.trackingData.collectAsState()
    val currentLocation by exerciseViewModel.currentLocation.collectAsState()

    // 权限请求
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // 请求权限和初始位置
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
        exerciseViewModel.fetchInitialLocation()
    }

    // 轨迹线数据
    val polylines = remember(trackingData.trackPoints) {
        if (trackingData.trackPoints.size >= 2) {
            listOf(
                PolylineData(
                    points = trackingData.trackPoints.map {
                        LatLngPoint(it.latitude, it.longitude)
                    },
                    color = android.graphics.Color.parseColor("#4ADE80"),
                    width = 12f
                )
            )
        } else emptyList()
    }

    // 地图中心位置
    val mapLat = currentLocation?.latitude ?: 39.9042
    val mapLng = currentLocation?.longitude ?: 116.4074

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图层（全屏）
        AMapComposeView(
            modifier = Modifier.fillMaxSize(),
            initialLatitude = mapLat,
            initialLongitude = mapLng,
            initialZoom = 17f,
            showLocation = true,
            polylines = polylines
        )

        // 顶部返回和标题
        TrackingTopBar(
            exerciseType = exerciseType,
            onBackClick = {
                if (trackingState is ExerciseTrackingState.Idle ||
                    trackingState is ExerciseTrackingState.Completed
                ) {
                    exerciseViewModel.resetTracking()
                    navController.popBackStack()
                }
            },
            trackingState = trackingState
        )

        // 权限未授予提示
        if (!locationPermissions.allPermissionsGranted) {
            PermissionDeniedOverlay(
                onRequestPermission = { locationPermissions.launchMultiplePermissionRequest() }
            )
        }

        // 底部控制面板
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // 数据面板
            TrackingDataPanel(
                distance = trackingData.totalDistance,
                duration = trackingData.elapsedTime,
                pace = trackingData.averagePace,
                calories = trackingData.caloriesBurned,
                currentPace = trackingData.currentPace,
                trackingState = trackingState
            )

            // 控制按钮
            TrackingControls(
                trackingState = trackingState,
                hasPermission = locationPermissions.allPermissionsGranted,
                onStart = {
                    exerciseViewModel.startTracking(exerciseType, planId)
                },
                onPause = { exerciseViewModel.pauseTracking() },
                onResume = { exerciseViewModel.resumeTracking() },
                onStop = { exerciseViewModel.stopTracking() },
                onFinish = {
                    // Phase 28: 导航到结算页，传递运动数据
                    val completed = trackingState as? ExerciseTrackingState.Completed
                    navController.navigate(
                        Screen.ExerciseSummary.createRoute(
                            planId = planId,
                            exerciseType = exerciseType,
                            distance = completed?.totalDistance ?: trackingData.totalDistance,
                            duration = completed?.totalDuration ?: trackingData.elapsedTime,
                            calories = trackingData.caloriesBurned,
                            pace = completed?.averagePace ?: trackingData.averagePace
                        )
                    )
                }
            )
        }
    }
}

/** 顶部栏 */
@Composable
private fun TrackingTopBar(
    exerciseType: String,
    onBackClick: () -> Unit,
    trackingState: ExerciseTrackingState
) {
    val title = when (exerciseType) {
        "walking" -> "散步"
        "running" -> "跑步"
        "cycling" -> "骑行"
        "hiking" -> "徒步"
        else -> "运动"
    }

    val stateLabel = when (trackingState) {
        is ExerciseTrackingState.Idle -> ""
        is ExerciseTrackingState.Tracking -> " · 进行中"
        is ExerciseTrackingState.Paused -> " · 已暂停"
        is ExerciseTrackingState.Completed -> " · 已完成"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮（仅在空闲或完成时可用）
        val canGoBack = trackingState is ExerciseTrackingState.Idle ||
                trackingState is ExerciseTrackingState.Completed
        IconButton(
            onClick = onBackClick,
            enabled = canGoBack
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = if (canGoBack) TextPrimary else TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$title$stateLabel",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

/** 实时数据面板 */
@Composable
private fun TrackingDataPanel(
    distance: Double,
    duration: Long,
    pace: Double,
    calories: Double,
    currentPace: Double,
    trackingState: ExerciseTrackingState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ambientColor = ForestGreen.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 主要数据：时间
            Text(
                text = ExerciseTrackingUtils.formatDuration(duration),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 次要数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataItem(
                    label = "距离",
                    value = ExerciseTrackingUtils.formatDistance(distance),
                    icon = Icons.Default.Straighten,
                    color = ForestGreen
                )
                DataItem(
                    label = "配速",
                    value = ExerciseTrackingUtils.formatPace(pace),
                    icon = Icons.Default.Speed,
                    color = SkyBlue
                )
                DataItem(
                    label = "热量",
                    value = "${calories.toInt()} kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    color = VitalOrange
                )
            }

            // 完成状态时显示摘要
            if (trackingState is ExerciseTrackingState.Completed) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BackgroundBeige)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "运动已完成",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
                    )
                }
            }
        }
    }
}

/** 单个数据项 */
@Composable
private fun DataItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/** 控制按钮区域 */
@Composable
private fun TrackingControls(
    trackingState: ExerciseTrackingState,
    hasPermission: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (trackingState) {
            is ExerciseTrackingState.Idle -> {
                // 开始按钮
                Button(
                    onClick = onStart,
                    enabled = hasPermission,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.6f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "开始运动",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is ExerciseTrackingState.Tracking -> {
                // 暂停按钮
                FloatingActionButton(
                    onClick = onPause,
                    containerColor = VitalOrange,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "暂停",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            is ExerciseTrackingState.Paused -> {
                // 停止按钮
                FloatingActionButton(
                    onClick = onStop,
                    containerColor = ErrorRed,
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "结束",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // 恢复按钮
                FloatingActionButton(
                    onClick = onResume,
                    containerColor = ForestGreen,
                    contentColor = Color.White,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "继续",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            is ExerciseTrackingState.Completed -> {
                // 完成按钮
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.6f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "完成",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** 权限未授予覆盖层 */
@Composable
private fun PermissionDeniedOverlay(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = VitalOrange,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "需要位置权限",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "运动轨迹记录需要访问您的位置信息，请授予位置权限后使用此功能。",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ForestGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("授予权限")
                }
            }
        }
    }
}
