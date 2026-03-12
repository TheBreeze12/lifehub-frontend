package com.example.lifehub.ui.screen

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.ai.SpeechRecognitionService
import com.example.lifehub.data.SpeechRecognitionState
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.ui.components.GlassCard
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.TripViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 运动规划页（餐后运动规划）- MVP版本 用户输入运动需求，AI生成运动计划 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TripPlanningPage(navController: NavController, tripViewModel: TripViewModel = viewModel()) {
        var inputText by remember { mutableStateOf("") }
        val scrollState = rememberScrollState()
        val context = LocalContext.current

        // Phase 45: 语音识别服务
        val speechService = remember { SpeechRecognitionService(context) }
        val speechState by speechService.recognitionState.collectAsState()

        // Phase 45: 录音权限
        val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

        // Phase 45: 监听语音识别结果，填入输入框
        LaunchedEffect(speechState) {
                when (val state = speechState) {
                        is SpeechRecognitionState.Result -> {
                                if (state.text.isNotBlank()) {
                                        inputText = state.text
                                }
                                speechService.resetState()
                        }
                        is SpeechRecognitionState.PartialResult -> {
                                if (state.text.isNotBlank()) {
                                        inputText = state.text
                                }
                        }
                        else -> {}
                }
        }

        // Phase 45: 页面销毁时释放语音识别资源
        DisposableEffect(Unit) {
                onDispose {
                        speechService.close()
                }
        }

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

        Box(modifier = Modifier.fillMaxSize().background(HomeBackgroundGradient)) {
                Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                                TopAppBar(
                                        title = { Text("运动规划") },
                                        navigationIcon = {
                                                IconButton(onClick = { navController.navigateUp() }) {
                                                        Icon(
                                                                imageVector =
                                                                        Icons.AutoMirrored.Filled
                                                                                .KeyboardArrowLeft,
                                                                contentDescription = "返回"
                                                        )
                                                }
                                        },
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.surface
                                                                        .copy(alpha = 0.6f),
                                                        titleContentColor =
                                                                MaterialTheme.colorScheme.onSurface,
                                                        navigationIconContentColor =
                                                                MaterialTheme.colorScheme.onSurface
                                                )
                                )
                        }
                ) { innerPadding ->
                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .verticalScroll(scrollState)
                                                .padding(innerPadding)
                                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                                TripPlanningHeader()
                                Spacer(modifier = Modifier.height(14.dp))
                                InputCard(
                                        inputText = inputText,
                                        onInputChange = { inputText = it },
                                        isLoading =
                                                generateTripState is
                                                        com.example.lifehub.viewmodel
                                                                .GenerateTripState.Loading,
                                        speechState = speechState,
                                        onVoiceClick = {
                                                if (speechState is SpeechRecognitionState.Listening ||
                                                        speechState is SpeechRecognitionState.Processing
                                                ) {
                                                        speechService.stopListening()
                                                } else {
                                                        if (audioPermissionState.status.isGranted) {
                                                                speechService.startListening()
                                                        } else {
                                                                audioPermissionState
                                                                        .launchPermissionRequest()
                                                        }
                                                }
                                        },
                                        onGenerateClick = {
                                                if (inputText.isNotBlank()) {
                                                        if (isLoggedIn && userId != null) {
                                                                if (!locationPermissionsState
                                                                                .allPermissionsGranted
                                                                ) {
                                                                        locationPermissionsState
                                                                                .launchMultiplePermissionRequest()
                                                                        return@InputCard
                                                                }
                                                                tripViewModel.generateTrip(
                                                                        userId = userId,
                                                                        query = inputText,
                                                                        preferences = null,
                                                                        latitude = userLocation?.first,
                                                                        longitude = userLocation?.second
                                                                )
                                                        } else {
                                                                navController.navigate(Screen.Login.route)
                                                        }
                                                }
                                        }
                                )
                                if (generateTripState is
                                                com.example.lifehub.viewmodel.GenerateTripState.Error
                                ) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        GlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                elevation = 8.dp
                                        ) {
                                                Text(
                                                        text =
                                                                (generateTripState as
                                                                                com.example.lifehub
                                                                                        .viewmodel
                                                                                        .GenerateTripState
                                                                                        .Error)
                                                                        .message,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(12.dp)
                                                )
                                        }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HotRecommendations(onRecommendationClick = { text -> inputText = text })
                                Spacer(modifier = Modifier.height(20.dp))
                                if (isLoggedIn && userId != null) {
                                        RecentTrips(navController, recentTripsState, tripViewModel, userId)
                                } else {
                                        GlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                elevation = 10.dp
                                        ) {
                                                Column(
                                                        modifier =
                                                                Modifier.fillMaxWidth().padding(22.dp),
                                                        horizontalAlignment =
                                                                Alignment.CenterHorizontally
                                                ) {
                                                        Icon(
                                                                Icons.Default.Lock,
                                                                contentDescription = null,
                                                                tint = TextSecondary,
                                                                modifier = Modifier.size(28.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                                text = "登录后可查看最近规划并快速复用",
                                                                fontSize = 13.sp,
                                                                color = TextSecondary
                                                        )
                                                }
                                        }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                        }
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
private fun TripPlanningHeader() {
        GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = 14.dp
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                Brush.linearGradient(
                                                        colors =
                                                                listOf(
                                                                        FreshMint.copy(alpha = 0.65f),
                                                                        FreshBlue.copy(alpha = 0.55f)
                                                                )
                                                )
                                        )
                                        .padding(20.dp)
                ) {
                        Column {
                                Text(
                                        text = "智能运动规划",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                        text = "输入目标后，AI 会结合饮食和出行场景生成更合理的餐后计划",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Chip(text = "AI 生成")
                                        Chip(text = "位置感知")
                                        Chip(text = "可执行计划")
                                }
                        }
                }
        }
}

@Composable
private fun Chip(text: String) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.35f)) {
                Text(
                        text = text,
                        fontSize = 11.sp,
                        color = TextPrimary,
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
        speechState: SpeechRecognitionState = SpeechRecognitionState.Idle,
        onVoiceClick: () -> Unit = {},
        onGenerateClick: () -> Unit
) {
        val isRecording = speechState is SpeechRecognitionState.Listening ||
                speechState is SpeechRecognitionState.Processing
        GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = 14.dp
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                VoiceInputButton(
                                        isRecording = isRecording,
                                        isProcessing = speechState is SpeechRecognitionState.Processing,
                                        onClick = onVoiceClick
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = "描述你的运动目标",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                text = when (speechState) {
                                                        is SpeechRecognitionState.Listening -> "正在聆听，请继续说"
                                                        is SpeechRecognitionState.Processing -> "语音识别中，请稍候"
                                                        is SpeechRecognitionState.Error -> speechState.message
                                                        else -> "可语音输入或手动输入，例如：餐后步行30分钟"
                                                },
                                                fontSize = 12.sp,
                                                color = when (speechState) {
                                                        is SpeechRecognitionState.Listening -> VitalOrange
                                                        is SpeechRecognitionState.Processing -> ForestGreen
                                                        is SpeechRecognitionState.Error -> MaterialTheme.colorScheme.error
                                                        else -> TextSecondary
                                                }
                                        )
                                }
                        }
                        OutlinedTextField(
                                value = inputText,
                                onValueChange = onInputChange,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                placeholder = {
                                        Text(
                                                text = "例如：晚饭后在家附近安排40分钟中等强度步行，目标消耗250卡",
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                        )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ForestGreen,
                                                unfocusedBorderColor = Color(0xFFCFD8E3)
                                        ),
                        )
                        Button(
                                onClick = onGenerateClick,
                                enabled = inputText.isNotBlank() && !isLoading,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = FreshBlue,
                                                disabledContainerColor = Color(0xFFBFC7D4)
                                        ),
                                shape = RoundedCornerShape(14.dp)
                        ) {
                                if (isLoading) {
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                )
                                                Text(
                                                        text = "正在生成计划...",
                                                        fontSize = 14.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Medium
                                                )
                                        }
                                } else {
                                        Text(
                                                text = "生成运动计划",
                                                fontSize = 15.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                        )
                                }
                        }
                }
        }
}

/** Phase 45: 语音输入按钮（带录音脉冲动画） */
@Composable
private fun VoiceInputButton(
        isRecording: Boolean,
        isProcessing: Boolean,
        onClick: () -> Unit
) {
        // 脉冲动画（录音中时显示）
        val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
        val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_alpha"
        )

        Box(contentAlignment = Alignment.Center) {
                // 脉冲光圈（仅录音中显示）
                if (isRecording && !isProcessing) {
                        Box(
                                modifier = Modifier
                                        .size(96.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(VitalOrange.copy(alpha = pulseAlpha))
                        )
                }

                // 主按钮
                Box(
                        modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                        Brush.linearGradient(
                                                colors = if (isRecording) {
                                                        listOf(VitalOrange, Color(0xFFE85D2C))
                                                } else {
                                                        listOf(ForestGreen, ForestGreenDark)
                                                }
                                        )
                                )
                                .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                ) {
                        if (isProcessing) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(34.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                )
                        } else {
                                Icon(
                                        imageVector = if (isRecording) Icons.Default.MicOff
                                                else Icons.Default.Mic,
                                        contentDescription = if (isRecording) "停止录音" else "语音输入",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                )
                        }
                }
        }
}

@Composable
private fun HotRecommendations(onRecommendationClick: (String) -> Unit) {
        GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 10.dp
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "热门推荐",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
        GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 10.dp,
                onClick = onClick
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
