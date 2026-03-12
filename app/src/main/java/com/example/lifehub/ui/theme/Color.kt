package com.example.lifehub.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// LifeHub 清新自然主题色 - 灵动渐变风格
val ForestGreen = Color(0xFF4ADE80) // 主色调：清新薄荷绿
val ForestGreenLight = Color(0xFF86EFAC) // 浅薄荷绿
val ForestGreenDark = Color(0xFF22C55E) // 深薄荷绿
val VitalOrange = Color(0xFFFB923C) // 强调色：活力橙
val VitalOrangeLight = Color(0xFFFDBA74) // 浅活力橙
val CoralPink = Color(0xFFF472B6) // 珊瑚粉
val SkyBlue = Color(0xFF38BDF8) // 天空蓝
val SkyBlueLight = Color(0xFF7DD3FC) // 浅天空蓝
val LavenderPurple = Color(0xFFA78BFA) // 薰衣草紫

// 背景色系 - 柔和自然
val BackgroundBeige = Color(0xFFFAFDF7) // 背景色：淡绿白
val BackgroundGradientStart = Color(0xFFF0FDF4) // 渐变起始：薄荷白
val BackgroundGradientEnd = Color(0xFFECFCCB) // 渐变结束：嫩绿
val CardBackground = Color(0xFFFFFFFF) // 卡片背景：纯白
val CardBackgroundTint = Color(0xFFF7FEE7) // 卡片淡绿色调
val FreshBlue = Color(0xFF60A5FA) // 清新蓝
val FreshMint = Color(0xFF5EEAD4) // 清新薄荷
val BackgroundGlassTop = Color(0xFFF2FAFF) // 玻璃背景顶部
val BackgroundGlassBottom = Color(0xFFEFFAF2) // 玻璃背景底部
val GlassSurfaceLight = Color(0xCCFFFFFF) // 浅色玻璃面
val GlassSurfaceDark = Color(0x26FFFFFF) // 暗色玻璃面
val GlassBorderLight = Color(0x99FFFFFF) // 浅色玻璃边框
val GlassBorderDark = Color(0x40FFFFFF) // 暗色玻璃边框

// 文字颜色 - 层次分明
val TextPrimary = Color(0xFF1E293B) // 主文字：深灰蓝
val TextSecondary = Color(0xFF64748B) // 次要文字：灰蓝
val TextTertiary = Color(0xFF94A3B8) // 第三级文字：浅灰蓝
val TextOnPrimary = Color(0xFFFFFFFF) // 主色上的文字：白色

// 功能色
val SuccessGreen = Color(0xFF10B981) // 成功绿
val WarningYellow = Color(0xFFFBBF24) // 警告黄
val ErrorRed = Color(0xFFF43F5E) // 错误红
val InfoBlue = Color(0xFF3B82F6) // 信息蓝

// 营养色彩
val ProteinColor = Color(0xFF10B981) // 蛋白质：翠绿
val FatColor = Color(0xFFF59E0B) // 脂肪：琥珀
val CarbsColor = Color(0xFF3B82F6) // 碳水：蓝色
val CaloriesColor = Color(0xFFF472B6) // 卡路里：粉色

// 暗色主题（备用）
val BackgroundDark = Color(0xFF0F172A) // 深色背景
val CardBackgroundDark = Color(0xFF1E293B) // 深色卡片

// 渐变色预设
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(ForestGreen, ForestGreenLight)
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(CoralPink, VitalOrangeLight)
)

val CoolGradient = Brush.linearGradient(
    colors = listOf(SkyBlue, LavenderPurple)
)

val WarmGradient = Brush.linearGradient(
    colors = listOf(VitalOrange, CoralPink)
)

val HomeBackgroundGradient = Brush.verticalGradient(
    colors = listOf(BackgroundGlassTop, BackgroundBeige, BackgroundGlassBottom)
)
