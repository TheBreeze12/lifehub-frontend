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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.lifehub.data.UserSession
import com.example.lifehub.navigation.Screen
import com.example.lifehub.viewmodel.BeforeMealUploadState
import com.example.lifehub.viewmodel.FoodViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*

private val VitalOrange = Color(0xFFFF6B35)
private val ForestGreen = Color(0xFF2D5A27)

/**
 * 餐前拍摄相机页面
 * Phase 13: 餐前拍摄功能
 * 实现拍摄引导提示（居中、光线、角度）
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BeforeMealCameraPage(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val foodViewModel: FoodViewModel = viewModel()

    // 相机权限
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // 状态管理
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    // 上传状态
    val uploadState by foodViewModel.beforeMealUploadState.collectAsState()

    // 相机控制器
    val cameraController = remember { BeforeMealCameraController(context) }

    // 获取用户ID
    val userId = try {
        if (UserSession.isLoggedIn()) UserSession.getUserId() else null
    } catch (e: Exception) {
        null
    }

    // 请求权限
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // 监听上传状态
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is BeforeMealUploadState.Success -> {
                isUploading = false
                // 上传成功，返回对比页面
                navController.popBackStack()
            }
            is BeforeMealUploadState.Error -> {
                isUploading = false
            }
            is BeforeMealUploadState.Loading -> {
                isUploading = true
            }
            else -> {}
        }
    }

    // 权限被拒绝时的处理
    if (!cameraPermissionState.status.isGranted) {
        BeforeMealPermissionDeniedContent(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onBack = { navController.navigateUp() }
        )
        return
    }

    // 显示照片预览
    if (showPreview && capturedImageUri != null) {
        BeforeMealImagePreviewScreen(
                imageUri = capturedImageUri!!,
                isUploading = isUploading,
                uploadError = (uploadState as? BeforeMealUploadState.Error)?.message,
                onConfirm = {
                    // 上传餐前图片
                    if (userId != null) {
                        foodViewModel.uploadBeforeMealImage(capturedImageUri!!, context, userId)
                    }
                },
                onRetake = {
                    showPreview = false
                    capturedImageUri = null
                    foodViewModel.resetBeforeMealUploadState()
                },
                onBack = {
                    navController.navigateUp()
                }
        )
        return
    }

    // 主相机界面
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        // 相机预览
        BeforeMealCameraPreview(
                controller = cameraController,
                modifier = Modifier.fillMaxSize()
        )

        // 顶部栏
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                )
            }

            // 标题
            Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = VitalOrange
            ) {
                Text(
                        text = "餐前拍摄",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                )
            }

            // 占位
            Spacer(modifier = Modifier.size(48.dp))
        }

        // 拍摄引导框和提示
        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            // 拍摄引导框
            Box(
                    modifier = Modifier
                            .size(300.dp, 240.dp)
                            .border(
                                    width = 3.dp,
                                    color = VitalOrange.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(16.dp)
                            ),
                    contentAlignment = Alignment.Center
            ) {
                // 四角标记
                BeforeMealCornerMarker(Alignment.TopStart)
                BeforeMealCornerMarker(Alignment.TopEnd)
                BeforeMealCornerMarker(Alignment.BottomStart)
                BeforeMealCornerMarker(Alignment.BottomEnd)

                // 中心十字准星
                Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 拍摄引导提示
            CaptureGuidanceOverlay()
        }

        // 底部拍摄按钮区域
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 提示文字
            Text(
                    text = "将食物居中放置在框内",
                    color = Color.White,
                    fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 拍照按钮
            Box(
                    modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(VitalOrange)
                            .clickable {
                                cameraController.takePicture(context) { uri ->
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
                        modifier = Modifier.size(36.dp)
                )
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

/** 拍摄引导提示覆盖层 */
@Composable
private fun CaptureGuidanceOverlay() {
    Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.7f)
    ) {
        Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                GuidanceItem(
                        icon = Icons.Default.CenterFocusStrong,
                        text = "居中"
                )
                GuidanceItem(
                        icon = Icons.Default.WbSunny,
                        text = "光线充足"
                )
                GuidanceItem(
                        icon = Icons.Default.Straighten,
                        text = "垂直俯拍"
                )
            }
        }
    }
}

/** 引导项 */
@Composable
private fun GuidanceItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        text: String
) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
                icon,
                contentDescription = null,
                tint = VitalOrange,
                modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp
        )
    }
}

/** 四角标记 */
@Composable
private fun BoxScope.BeforeMealCornerMarker(alignment: Alignment) {
    val cornerSize = 24.dp
    val strokeWidth = 3.dp

    Box(
            modifier = Modifier
                    .size(cornerSize)
                    .align(alignment)
    ) {
        // 根据位置绘制角标
        when (alignment) {
            Alignment.TopStart -> {
                Box(
                        modifier = Modifier
                                .width(cornerSize)
                                .height(strokeWidth)
                                .background(VitalOrange)
                )
                Box(
                        modifier = Modifier
                                .width(strokeWidth)
                                .height(cornerSize)
                                .background(VitalOrange)
                )
            }
            Alignment.TopEnd -> {
                Box(
                        modifier = Modifier
                                .width(cornerSize)
                                .height(strokeWidth)
                                .align(Alignment.TopEnd)
                                .background(VitalOrange)
                )
                Box(
                        modifier = Modifier
                                .width(strokeWidth)
                                .height(cornerSize)
                                .align(Alignment.TopEnd)
                                .background(VitalOrange)
                )
            }
            Alignment.BottomStart -> {
                Box(
                        modifier = Modifier
                                .width(cornerSize)
                                .height(strokeWidth)
                                .align(Alignment.BottomStart)
                                .background(VitalOrange)
                )
                Box(
                        modifier = Modifier
                                .width(strokeWidth)
                                .height(cornerSize)
                                .align(Alignment.BottomStart)
                                .background(VitalOrange)
                )
            }
            Alignment.BottomEnd -> {
                Box(
                        modifier = Modifier
                                .width(cornerSize)
                                .height(strokeWidth)
                                .align(Alignment.BottomEnd)
                                .background(VitalOrange)
                )
                Box(
                        modifier = Modifier
                                .width(strokeWidth)
                                .height(cornerSize)
                                .align(Alignment.BottomEnd)
                                .background(VitalOrange)
                )
            }
        }
    }
}

/** 相机预览组件 */
@Composable
private fun BeforeMealCameraPreview(
        controller: BeforeMealCameraController,
        modifier: Modifier = Modifier
) {
    AndroidView(
            factory = { context ->
                PreviewView(context).apply { controller.previewView = this }
            },
            modifier = modifier
    )
}

/** 相机控制器 */
class BeforeMealCameraController(private val context: Context) {
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

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                                previewView.context as androidx.lifecycle.LifecycleOwner,
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

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "before_meal_$name")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build()

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

/** 权限被拒绝时的内容 */
@Composable
private fun BeforeMealPermissionDeniedContent(
        onRequestPermission: () -> Unit,
        onBack: () -> Unit
) {
    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E))
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
                text = "需要相机权限",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
                text = "请授予相机权限以使用餐前拍摄功能",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = VitalOrange)
        ) {
            Text("授权相机权限", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("返回", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

/** 图片预览界面 */
@Composable
private fun BeforeMealImagePreviewScreen(
        imageUri: Uri,
        isUploading: Boolean,
        uploadError: String?,
        onConfirm: () -> Unit,
        onRetake: () -> Unit,
        onBack: () -> Unit
) {
    Column(
            modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E))
    ) {
        // 顶部栏
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                )
            }

            Text(
                    text = "确认照片",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        // 图片预览
        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                    model = imageUri,
                    contentDescription = "餐前照片预览",
                    modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
            )

            // 上传中遮罩
            if (isUploading) {
                Box(
                        modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = VitalOrange)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "AI正在分析...",
                                color = Color.White,
                                fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // 错误提示
        uploadError?.let { error ->
            Surface(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f)
            ) {
                Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = error,
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 底部按钮
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                    )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重拍")
            }

            Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = VitalOrange)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("确认上传")
            }
        }
    }
}
