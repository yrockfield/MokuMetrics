package com.example.mokumetrics

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// テーマ別 ColorScheme の定義

// 1. Aurora Green (オーロラ)
val AuroraColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF059669),
    background = Color(0xFF07120E),
    onBackground = Color(0xFFE2F1EA),
    surface = Color(0xFF0D231A),
    onSurface = Color(0xFFE2F1EA),
    surfaceVariant = Color(0xFF133526),
    onSurfaceVariant = Color(0xFF8DAE9F),
    error = Color(0xFFEF4444)
)

// 2. Dark Neon (ダークネオン)
val DarkNeonColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7C3AED),
    background = Color(0xFF05050C),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEC4899)
)

// 3. Cyberpunk (サイバーパンク)
val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFFACC15),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFEAB308),
    background = Color(0xFF050505),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0C),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFFA1A1AA),
    error = Color(0xFFEC4899)
)

// カスタムテーマ情報の CompositionLocal
data class AppThemeColors(
    val accentShadow: Color,
    val cardBorder: Color,
    val cardBorderFocus: Color,
    val textSecondary: Color,
    val warningColor: Color
)

val LocalAppThemeColors = staticCompositionLocalOf {
    AppThemeColors(
        accentShadow = Color(0x4D10B981),
        cardBorder = Color(0x2610B981),
        cardBorderFocus = Color(0x6610B981),
        textSecondary = Color(0xFF8DAE9F),
        warningColor = Color(0xFFF59E0B)
    )
}

@Composable
fun MokuMetricsTheme(
    themeName: String,
    content: @Composable () -> Unit
) {
    val (colorScheme, customColors) = when (themeName) {
        "aurora" -> Pair(
            AuroraColorScheme,
            AppThemeColors(
                accentShadow = Color(0x4D10B981),
                cardBorder = Color(0x2610B981),
                cardBorderFocus = Color(0x6610B981),
                textSecondary = Color(0xFF8DAE9F),
                warningColor = Color(0xFFF59E0B)
            )
        )
        "neon" -> Pair(
            DarkNeonColorScheme,
            AppThemeColors(
                accentShadow = Color(0x738B5CF6),
                cardBorder = Color(0x338B5CF6),
                cardBorderFocus = Color(0x808B5CF6),
                textSecondary = Color(0xFF94A3B8),
                warningColor = Color(0xFFF59E0B)
            )
        )
        "cyberpunk" -> Pair(
            CyberpunkColorScheme,
            AppThemeColors(
                accentShadow = Color(0x66FACC15),
                cardBorder = Color(0x40FACC15),
                cardBorderFocus = Color(0xB3EC4899),
                textSecondary = Color(0xFFA1A1AA),
                warningColor = Color(0xFF06B6D4)
            )
        )
        else -> Pair(
            AuroraColorScheme,
            AppThemeColors(
                accentShadow = Color(0x4D10B981),
                cardBorder = Color(0x2610B981),
                cardBorderFocus = Color(0x6610B981),
                textSecondary = Color(0xFF8DAE9F),
                warningColor = Color(0xFFF59E0B)
            )
        )
    }

    CompositionLocalProvider(LocalAppThemeColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
