package com.example.lifehub.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
        darkColorScheme(
                primary = FreshMint,
                secondary = FreshBlue,
                tertiary = SkyBlue,
                background = BackgroundDark,
                surface = GlassSurfaceDark,
                onPrimary = TextOnPrimary,
                onSecondary = TextOnPrimary,
                onBackground = Color.White,
                onSurface = Color.White,
                error = ErrorRed
        )

private val LightColorScheme =
        lightColorScheme(
                primary = FreshMint,
                primaryContainer = ForestGreenLight,
                secondary = FreshBlue,
                secondaryContainer = SkyBlueLight,
                tertiary = ForestGreen,
                tertiaryContainer = SkyBlueLight,
                background = BackgroundBeige,
                surface = GlassSurfaceLight,
                surfaceVariant = CardBackgroundTint,
                onPrimary = TextOnPrimary,
                onSecondary = TextOnPrimary,
                onTertiary = TextOnPrimary,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
                onSurfaceVariant = TextSecondary,
                error = ErrorRed,
                onError = TextOnPrimary,
                outline = TextTertiary
        )

@Composable
fun LifeHubTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = false, // 禁用动态色彩以保持统一的品牌风格
        content: @Composable () -> Unit
) {
    val colorScheme =
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
