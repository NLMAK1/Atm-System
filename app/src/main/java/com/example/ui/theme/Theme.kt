package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AtmEmerald, // Lilac 0xFFD0BCFF
    onPrimary = ElegantPurpleDark, // Deep Purple 0xFF381E72
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = AtmEmeraldGlow,
    secondary = AtmCyan, // Sky 0xFF7DD3FC
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFC2E7FF),
    tertiary = AtmAmber,
    onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF604100),
    onTertiaryContainer = Color(0xFFFFDEA8),
    background = AtmNavy900, // 0xFF131418
    onBackground = AtmTextPrimary,
    surface = AtmNavy800, // 0xFF1E2026
    onSurface = AtmTextPrimary,
    surfaceVariant = AtmNavy700, // 0xFF282B34
    onSurfaceVariant = AtmTextSecondary,
    outline = ElegantBorder,
    outlineVariant = Color(0xFF2E313D),
    error = AtmRed,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
