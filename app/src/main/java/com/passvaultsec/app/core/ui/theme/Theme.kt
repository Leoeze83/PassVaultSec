package com.passvaultsec.app.core.ui.theme

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

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF042F66),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFC2E7FF),
    onSecondary = Color(0xFF003355),
    background = Color(0xFF131314),
    surface = Color(0xFF1E1F20),
    surfaceVariant = Color(0xFF282A2C),
    surfaceContainer = Color(0xFF282A2C),
    surfaceContainerHigh = Color(0xFF333538),
    onBackground = Color(0xFFE3E3E3),
    onSurface = Color(0xFFE3E3E3),
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF444746)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00639B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F4F9),
    surfaceContainer = Color(0xFFF0F4F9),
    surfaceContainerHigh = Color(0xFFE9EEF6),
    onBackground = Color(0xFF1F1F1F),
    onSurface = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFF444746),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5)
)

@Composable
fun PassVaultSecTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
