package com.example.lifehub.ui.screen

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.lifehub.data.DishItem
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.viewmodel.FoodViewModel
import com.example.lifehub.viewmodel.MenuRecognitionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

private val CoralOrange = Color(0xFFFF7F50)

/** 拍照识别页面 用户可以拍摄菜单照片，识别菜品并查看营养信息 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPage(navController: NavController) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val foodViewModel: FoodViewModel = viewModel()

        // 相机权限
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

        // 状态管理
        var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
        var showPreview by remember { mutableStateOf(false) }

        // 识别状态
        val recognitionState by foodViewModel.recognitionState.collectAsState()

        // 相机控制器
        val cameraController = remember { CameraController(context) }

        // 获取用户ID
        val userId =
                try {
                        if (UserSession.isLoggedIn()) UserSession.getUserId() else null
                } catch (e: Exception) {
                        null
                }

        // 请求权限并加载最新识别结果（仅在有用户时）
        LaunchedEffect(Unit) {
                if (!cameraPermissionState.status.isGranted) {
                        cameraPermissionState.launchPermissionRequest()
                } else {
                        // 权限已授予，只有在有用户ID时才加载最新的识别结果
                        if (userId != null) {
                                foodViewModel.getLatestRecognition(userId)
                        }
                }
        }

        // 权限被拒绝时的处理
        if (!cameraPermissionState.status.isGranted) {
                PermissionDeniedContent(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                        onBack = { navController.navigateUp() }
                )
                return
        }

        // 显示照片预览
        if (showPreview && capturedImageUri != null) {
                ImagePreviewScreen(
                        imageUri = capturedImageUri!!,
                        onConfirm = {
                                // 调用API识别菜单（传递userId以保存结果）
                                capturedImageUri?.let { uri ->
                                        foodViewModel.recognizeMenu(uri, context, userId)
                                        showPreview = false
                                }
                        },
                        onRetake = {
                                showPreview = false
                                capturedImageUri = null
                                foodViewModel.resetRecognitionState()
                        }
                )
                return
        }

        // 主相机界面
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
                // 相机预览区域
                Box(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        contentAlignment = Alignment.Center
                ) {
                        // 返回按钮
                        IconButton(
                                onClick = { navController.navigateUp() },
                                modifier =
                                        Modifier.align(Alignment.TopStart)
                                                .padding(16.dp)
                                                .background(
                                                        Color.White.copy(alpha = 0.2f),
                                                        CircleShape
                                                )
                        ) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回",
                                        tint = Color.White
                                )
                        }

                        // 相机预览
                        CameraPreview(
                                controller = cameraController,
                                modifier = Modifier.fillMaxSize()
                        )

                        // 取景框
                        Box(
                                modifier =
                                        Modifier.size(280.dp, 200.dp)
                                                .border(
                                                        width = 2.dp,
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        shape = RoundedCornerShape(8.dp)
                                                )
                        ) {
                                CornerMarker(Alignment.TopStart)
                                CornerMarker(Alignment.TopEnd)
                                CornerMarker(Alignment.BottomStart)
                                CornerMarker(Alignment.BottomEnd)
                        }

                        // 提示文字
                        Text(
                                text = "将菜单置于框内",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center).offset(y = 120.dp)
                        )

                        // 拍照按钮
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .padding(bottom = 40.dp)
                                                .size(70.dp)
                                                .border(4.dp, Color.White, CircleShape)
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .background(CoralOrange)
                                                .clickable {
                                                        cameraController.takePicture(context) { uri
                                                                ->
                                                                capturedImageUri = uri
                                                                showPreview = true
                                                        }
                                                },
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "拍照",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                )
                        }
                }

                // 识别结果区域
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                        color = Color.White
                ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = "识别结果",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                        )
                                        if (recognitionState is MenuRecognitionState.Success &&
                                                        userId != null
                                        ) {
                                                Text(
                                                        text =
                                                                "共识别${(recognitionState as MenuRecognitionState.Success).dishes.size}道菜品",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF9CA3AF)
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 根据识别状态显示不同内容
                                // 如果没有用户，不显示任何识别结果
                                when (val state = recognitionState) {
                                        is MenuRecognitionState.Loading -> {
                                                Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment
                                                                                .CenterHorizontally,
                                                                verticalArrangement =
                                                                        Arrangement.Center
                                                        ) {
                                                                CircularProgressIndicator(
                                                                        color = CoralOrange,
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text = "正在识别菜单...",
                                                                        color = Color(0xFF9CA3AF),
                                                                        fontSize = 14.sp
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        8.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text =
                                                                                "AI正在分析菜品，可能需要较长时间，请耐心等待",
                                                                        color = Color(0xFF9CA3AF),
                                                                        fontSize = 12.sp
                                                                )
                                                        }
                                                }
                                        }
                                        is MenuRecognitionState.Success -> {
                                                // 只有在有用户时才显示识别结果
                                                if (userId != null && state.dishes.isNotEmpty()) {
                                                        LazyColumn(
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                items(state.dishes) { dish ->
                                                                        DishItemCard(
                                                                                dish = dish,
                                                                                onClick = {
                                                                                        navController
                                                                                                .navigate(
                                                                                                        Screen.NutritionDetail
                                                                                                                .createRoute(
                                                                                                                        dish.name
                                                                                                                )
                                                                                                )
                                                                                }
                                                                        )
                                                                }
                                                        }
                                                } else if (userId == null) {
                                                        // 没有用户时显示提示
                                                        Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Column(
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "请先登录以保存识别结果",
                                                                                color =
                                                                                        Color(
                                                                                                0xFF9CA3AF
                                                                                        ),
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text = "点击拍照按钮开始识别",
                                                                                color =
                                                                                        Color(
                                                                                                0xFF9CA3AF
                                                                                        ),
                                                                                fontSize = 12.sp
                                                                        )
                                                                }
                                                        }
                                                } else {
                                                        Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Text(
                                                                        text = "未识别到菜品，请重试",
                                                                        color = Color(0xFF9CA3AF),
                                                                        fontSize = 14.sp
                                                                )
                                                        }
                                                }
                                        }
                                        is MenuRecognitionState.Error -> {
                                                Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Column(
                                                                horizontalAlignment =
                                                                        Alignment
                                                                                .CenterHorizontally,
                                                                verticalArrangement =
                                                                        Arrangement.Center
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.ErrorOutline,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFEF4444),
                                                                        modifier =
                                                                                Modifier.size(48.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                Text(
                                                                        text = state.message,
                                                                        color = Color(0xFFEF4444),
                                                                        fontSize = 14.sp
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                Button(
                                                                        onClick = {
                                                                                foodViewModel
                                                                                        .resetRecognitionState()
                                                                        },
                                                                        colors =
                                                                                ButtonDefaults
                                                                                        .buttonColors(
                                                                                                containerColor =
                                                                                                        CoralOrange
                                                                                        )
                                                                ) {
                                                                        Text(
                                                                                "重试",
                                                                                color = Color.White
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                        else -> {
                                                // 没有用户时显示提示，有用户时显示默认提示
                                                Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        if (userId == null) {
                                                                Column(
                                                                        horizontalAlignment =
                                                                                Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        Text(
                                                                                text =
                                                                                        "请先登录以保存识别结果",
                                                                                color =
                                                                                        Color(
                                                                                                0xFF9CA3AF
                                                                                        ),
                                                                                fontSize = 14.sp
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                        Text(
                                                                                text = "点击拍照按钮开始识别",
                                                                                color =
                                                                                        Color(
                                                                                                0xFF9CA3AF
                                                                                        ),
                                                                                fontSize = 12.sp
                                                                        )
                                                                }
                                                        } else {
                                                                Text(
                                                                        text = "点击拍照按钮开始识别",
                                                                        color = Color(0xFF9CA3AF),
                                                                        fontSize = 14.sp
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        // 初始化相机
        LaunchedEffect(Unit) {
                if (cameraPermissionState.status.isGranted) {
                        cameraController.startCamera(lifecycleOwner)
                }
        }

        // 清理资源
        DisposableEffect(Unit) { onDispose { cameraController.release() } }
}

/** 相机预览组件 */
@Composable
fun CameraPreview(controller: CameraController, modifier: Modifier = Modifier) {
        AndroidView(
                factory = { context ->
                        PreviewView(context).apply { controller.previewView = this }
                },
                modifier = modifier
        )
}

/** 相机控制器 */
class CameraController(private val context: Context) {
        private var cameraProvider: ProcessCameraProvider? = null
        private var imageCapture: ImageCapture? = null
        var previewView: PreviewView? = null
                set(value) {
                        field = value
                        value?.let { startPreview(it) }
                }

        private fun startPreview(previewView: PreviewView) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener(
                        {
                                val provider = cameraProviderFuture.get()
                                cameraProvider = provider

                                val preview =
                                        Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                imageCapture =
                                        ImageCapture.Builder()
                                                .setCaptureMode(
                                                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                                                )
                                                .build()

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                                previewView.context as
                                                        androidx.lifecycle.LifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageCapture
                                        )
                                } catch (e: Exception) {
                                        e.printStackTrace()
                                }
                        },
                        ContextCompat.getMainExecutor(context)
                )
        }

        fun startCamera(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
                previewView?.let { startPreview(it) }
        }

        fun takePicture(context: Context, onImageSaved: (Uri) -> Unit) {
                val imageCapture = imageCapture ?: return

                val name =
                        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                                .format(System.currentTimeMillis())
                val contentValues =
                        android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                                put(
                                        android.provider.MediaStore.MediaColumns.MIME_TYPE,
                                        "image/jpeg"
                                )
                        }

                val outputFileOptions =
                        ImageCapture.OutputFileOptions.Builder(
                                        context.contentResolver,
                                        android.provider.MediaStore.Images.Media
                                                .EXTERNAL_CONTENT_URI,
                                        contentValues
                                )
                                .build()

                imageCapture.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        output.savedUri?.let { onImageSaved(it) }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                }
                        }
                )
        }

        fun release() {
                cameraProvider?.unbindAll()
        }
}

/** 照片预览界面 */
@Composable
fun ImagePreviewScreen(imageUri: Uri, onConfirm: () -> Unit, onRetake: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // 顶部操作栏
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        IconButton(
                                onClick = onRetake,
                                modifier =
                                        Modifier.background(
                                                Color.White.copy(alpha = 0.2f),
                                                CircleShape
                                        )
                        ) {
                                Icon(
                                        Icons.Default.Close,
                                        contentDescription = "重拍",
                                        tint = Color.White
                                )
                        }
                }

                // 照片预览
                Box(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                ) {
                        AsyncImage(
                                model = imageUri,
                                contentDescription = "预览",
                                modifier = Modifier.fillMaxSize()
                        )
                }

                // 底部按钮
                Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                        Button(
                                onClick = onRetake,
                                modifier = Modifier.weight(1f),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.2f)
                                        )
                        ) { Text("重拍", color = Color.White) }

                        Button(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CoralOrange)
                        ) { Text("确认", color = Color.White) }
                }
        }
}

/** 权限被拒绝时的界面 */
@Composable
fun PermissionDeniedContent(onRequestPermission: () -> Unit, onBack: () -> Unit) {
        Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
                Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                        text = "需要相机权限",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "请允许应用访问相机以拍摄菜单照片",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralOrange)
                ) { Text("授予权限", color = Color.White) }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBack) { Text("返回", color = Color.White.copy(alpha = 0.8f)) }
        }
}

/** 取景框角标组件 */
@Composable
fun BoxScope.CornerMarker(alignment: Alignment) {
        val cornerRadius = 4.dp
        val shape =
                when (alignment) {
                        Alignment.TopStart -> RoundedCornerShape(topStart = cornerRadius)
                        Alignment.TopEnd -> RoundedCornerShape(topEnd = cornerRadius)
                        Alignment.BottomStart -> RoundedCornerShape(bottomStart = cornerRadius)
                        Alignment.BottomEnd -> RoundedCornerShape(bottomEnd = cornerRadius)
                        else -> RoundedCornerShape(0.dp)
                }

        Box(
                modifier =
                        Modifier.align(alignment)
                                .size(24.dp)
                                .border(width = 4.dp, color = CoralOrange, shape = shape)
        )
}

/** 菜品卡片组件 */
@Composable
fun DishItemCard(dish: DishItem, onClick: () -> Unit) {
        // 根据菜品名称生成emoji（简化版，实际可以更智能）
        val emoji =
                when {
                        dish.name.contains("牛") || dish.name.contains("肉") -> "🥩"
                        dish.name.contains("鸡") || dish.name.contains("蛋") -> "🍳"
                        dish.name.contains("鱼") -> "🐟"
                        dish.name.contains("菜") ||
                                dish.name.contains("白菜") ||
                                dish.name.contains("豆芽") -> "🥬"
                        dish.name.contains("豆腐") -> "🧈"
                        else -> "🍽️"
                }
        Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF9FAFB),
                tonalElevation = 1.dp
        ) {
                Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Box(
                                modifier =
                                        Modifier.size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                        if (dish.isRecommended) Color(0xFFD1FAE5)
                                                        else Color(0xFFFED7AA)
                                                ),
                                contentAlignment = Alignment.Center
                        ) { Text(text = emoji, fontSize = 24.sp) }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        Text(
                                                text = dish.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                        )
                                        Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color =
                                                        if (dish.isRecommended) Color(0xFFD1FAE5)
                                                        else Color(0xFFFEF3C7)
                                        ) {
                                                Text(
                                                        text =
                                                                if (dish.isRecommended)
                                                                        "✓ ${dish.reason ?: "推荐"}"
                                                                else "⚠️ ${dish.reason ?: "注意"}",
                                                        fontSize = 10.sp,
                                                        color =
                                                                if (dish.isRecommended)
                                                                        Color(0xFF059669)
                                                                else Color(0xFFD97706),
                                                        modifier =
                                                                Modifier.padding(
                                                                        horizontal = 8.dp,
                                                                        vertical = 2.dp
                                                                )
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                                text = "${dish.calories.toInt()}kcal",
                                                fontSize = 10.sp,
                                                color = Color(0xFF9CA3AF)
                                        )
                                        Text(
                                                text = "• 蛋白质 ${dish.protein.toInt()}g",
                                                fontSize = 10.sp,
                                                color = Color(0xFF9CA3AF)
                                        )
                                }
                        }
                }
        }
}
