package com.example.lifehub.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lifehub.data.*
import com.example.lifehub.ui.theme.*
import com.example.lifehub.viewmodel.OfflinePackageListState
import com.example.lifehub.viewmodel.OfflinePackageState
import com.example.lifehub.viewmodel.TripViewModel
import java.io.File

/**
 * 离线运动包管理页面（Phase 47）
 *
 * 功能：
 * 1. 显示已下载的离线包列表
 * 2. 针对指定planId生成/下载离线包
 * 3. 查看离线包内容（运动方案、POI列表）
 * 4. 删除已下载的离线包
 * 5. 显示下载进度
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePackagePage(
    navController: NavController,
    planId: Int? = null,
    tripViewModel: TripViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 初始化离线包管理器
    LaunchedEffect(Unit) {
        val storageDir = File(context.filesDir, "offline_packages")
        tripViewModel.initOfflinePackageManager(storageDir)
        tripViewModel.loadOfflinePackages()
    }

    val offlinePackageState by tripViewModel.offlinePackageState.collectAsState()
    val offlinePackageListState by tripViewModel.offlinePackageListState.collectAsState()

    // 当前查看详情的包ID
    var viewingPackageId by remember { mutableStateOf<String?>(null) }
    var viewingContents by remember { mutableStateOf<OfflinePackageContents?>(null) }
    // 确认删除对话框
    var deletingPackageId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "离线运动包",
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
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = ForestGreenDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, BackgroundGradientStart, BackgroundBeige)
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 操作状态提示
                item {
                    OfflinePackageStatusCard(
                        state = offlinePackageState,
                        planId = planId,
                        onGenerate = {
                            if (planId != null) {
                                tripViewModel.generateAndDownloadOfflinePackage(planId)
                            }
                        },
                        onReset = { tripViewModel.resetOfflinePackageState() }
                    )
                }

                // 已下载的离线包列表
                item {
                    Text(
                        text = "已下载的离线包",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                when (val listState = offlinePackageListState) {
                    is OfflinePackageListState.Success -> {
                        if (listState.packages.isEmpty()) {
                            item {
                                EmptyPackageHint()
                            }
                        } else {
                            items(listState.packages) { pkg ->
                                OfflinePackageCard(
                                    pkg = pkg,
                                    onView = {
                                        viewingPackageId = pkg.packageId
                                        viewingContents = tripViewModel.getOfflinePackageContents(pkg.packageId)
                                    },
                                    onDelete = { deletingPackageId = pkg.packageId }
                                )
                            }
                        }
                    }
                    else -> {
                        item {
                            EmptyPackageHint()
                        }
                    }
                }

                // 离线包内容详情（展开显示）
                if (viewingPackageId != null && viewingContents != null) {
                    item {
                        OfflinePackageContentsCard(
                            contents = viewingContents!!,
                            onClose = {
                                viewingPackageId = null
                                viewingContents = null
                            }
                        )
                    }
                }

                // 底部间距
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // 删除确认对话框
    if (deletingPackageId != null) {
        AlertDialog(
            onDismissRequest = { deletingPackageId = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除此离线包吗？删除后需重新下载。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tripViewModel.deleteOfflinePackage(deletingPackageId!!)
                        if (viewingPackageId == deletingPackageId) {
                            viewingPackageId = null
                            viewingContents = null
                        }
                        deletingPackageId = null
                    }
                ) {
                    Text("删除", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPackageId = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/** 操作状态卡片 - 显示当前操作状态和下载进度 */
@Composable
private fun OfflinePackageStatusCard(
    state: OfflinePackageState,
    planId: Int?,
    onGenerate: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = ForestGreen.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(SkyBlue, ForestGreenLight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "离线运动包",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "下载后可在无网络时查看运动计划和POI",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is OfflinePackageState.Idle -> {
                    if (planId != null) {
                        Button(
                            onClick = onGenerate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成并下载离线包（计划 #$planId）")
                        }
                    } else {
                        Text(
                            text = "从运动计划详情页点击下载按钮，可生成离线包",
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    }
                }
                is OfflinePackageState.Generating -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = ForestGreen
                        )
                        Text(
                            text = "正在生成离线包...",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
                is OfflinePackageState.Downloading -> {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "正在下载...",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(state.progress * 100).toInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ForestGreen,
                            trackColor = ForestGreenLight.copy(alpha = 0.3f)
                        )
                    }
                }
                is OfflinePackageState.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "离线包下载成功！",
                            fontSize = 14.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("完成")
                    }
                }
                is OfflinePackageState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            color = ErrorRed
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("重试")
                    }
                }
            }
        }
    }
}

/** 空状态提示 */
@Composable
private fun EmptyPackageHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "暂无离线包",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = "在运动计划详情页点击下载按钮即可生成",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
    }
}

/** 离线包卡片 */
@Composable
private fun OfflinePackageCard(
    pkg: LocalOfflinePackage,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 状态图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (pkg.status) {
                                OfflinePackageStatus.DOWNLOADED -> SuccessGreen.copy(alpha = 0.15f)
                                OfflinePackageStatus.DOWNLOADING -> SkyBlue.copy(alpha = 0.15f)
                                OfflinePackageStatus.ERROR -> ErrorRed.copy(alpha = 0.15f)
                                else -> TextTertiary.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (pkg.status) {
                            OfflinePackageStatus.DOWNLOADED -> Icons.Default.OfflinePin
                            OfflinePackageStatus.DOWNLOADING -> Icons.Default.Downloading
                            OfflinePackageStatus.ERROR -> Icons.Default.ErrorOutline
                            else -> Icons.Default.CloudDownload
                        },
                        contentDescription = null,
                        tint = when (pkg.status) {
                            OfflinePackageStatus.DOWNLOADED -> SuccessGreen
                            OfflinePackageStatus.DOWNLOADING -> SkyBlue
                            OfflinePackageStatus.ERROR -> ErrorRed
                            else -> TextTertiary
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.planTitle ?: "运动计划 #${pkg.planId}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pkg.planDestination != null) {
                            Text(
                                text = pkg.planDestination,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "v${pkg.version}",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        Text(
                            text = pkg.fileSizeFormatted(),
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }

            // 下载进度条（仅下载中显示）
            if (pkg.status == OfflinePackageStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pkg.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SkyBlue,
                    trackColor = SkyBlueLight.copy(alpha = 0.3f)
                )
            }

            // 操作按钮行
            if (pkg.status == OfflinePackageStatus.DOWNLOADED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onView) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ForestGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("查看内容", color = ForestGreen, fontSize = 13.sp)
                    }
                    TextButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ErrorRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除", color = ErrorRed, fontSize = 13.sp)
                    }
                }
            }

            // 错误状态提示
            if (pkg.status == OfflinePackageStatus.ERROR && pkg.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pkg.errorMessage,
                    fontSize = 12.sp,
                    color = ErrorRed
                )
            }
        }
    }
}

/** 离线包内容详情卡片 */
@Composable
private fun OfflinePackageContentsCard(
    contents: OfflinePackageContents,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = ForestGreen.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "离线包内容",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = TextSecondary
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = TextTertiary.copy(alpha = 0.2f)
            )

            // 运动方案
            contents.plan?.let { plan ->
                Text(
                    text = "运动方案",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (plan.title.isNotEmpty()) {
                    InfoRow("标题", plan.title)
                }
                if (plan.destination.isNotEmpty()) {
                    InfoRow("地点", plan.destination)
                }
                if (plan.date.isNotEmpty()) {
                    InfoRow("日期", plan.date)
                }
                InfoRow("总时长", "${plan.totalDuration}分钟")
                InfoRow("总热量", "${plan.totalCalories}千卡")
                InfoRow("运动项目", "${plan.itemCount}个")

                // 详细运动项目列表
                if (plan.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    plan.items.forEachIndexed { index, item ->
                        PlanItemRow(index + 1, item)
                    }
                }
            }

            // POI列表
            if (contents.pois.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "POI 地点（${contents.pois.size}个）",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SkyBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                contents.pois.forEach { poi ->
                    PoiRow(poi)
                }
            }

            // 瓦片元数据
            contents.tilesMetadata?.let { meta ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "地图瓦片",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VitalOrange
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (meta.zoomLevels.isNotEmpty()) {
                    InfoRow("缩放级别", meta.zoomLevels.joinToString(", "))
                }
                InfoRow("预估瓦片数", "${meta.tileCountEstimate}")
                meta.center?.let { center ->
                    InfoRow("中心点", "${center.latitude}, ${center.longitude}")
                }
                meta.bounds?.let { bounds ->
                    InfoRow("覆盖范围",
                        "纬度 ${bounds.minLat}~${bounds.maxLat}\n经度 ${bounds.minLng}~${bounds.maxLng}")
                }
            }
        }
    }
}

/** 信息行 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(max = 220.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 运动项目行 */
@Composable
private fun PlanItemRow(index: Int, item: OfflinePlanItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(ForestGreenLight.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ForestGreenDark
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.placeName.ifEmpty { "未命名" },
                fontSize = 13.sp,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.startTime?.let {
                    Text(text = it, fontSize = 11.sp, color = TextTertiary)
                }
                Text(text = "${item.duration}分钟", fontSize = 11.sp, color = TextTertiary)
                Text(text = "${item.calories}千卡", fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

/** POI行 */
@Composable
private fun PoiRow(poi: OfflinePoiData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Place,
            contentDescription = null,
            tint = SkyBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = poi.name, fontSize = 13.sp, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = poi.type, fontSize = 11.sp, color = TextTertiary)
                if (poi.latitude != null && poi.longitude != null) {
                    Text(
                        text = "(${String.format("%.4f", poi.latitude)}, ${String.format("%.4f", poi.longitude)})",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}
