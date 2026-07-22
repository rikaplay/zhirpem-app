package com.RIKAPLAY.zhirpem_app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.RIKAPLAY.zhirpem_app.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8DE3B5),
    background = Color(0xFF121413),
    surface = Color(0xFF1A1D1C),
    onBackground = Color(0xFFE2E3E1),
    onSurface = Color(0xFFE2E3E1),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006B44),
    background = Color(0xFFF7FBF8),
    surface = Color(0xFFEEF2EE),
    onBackground = Color(0xFF1A1D1C),
    onSurface = Color(0xFF1A1D1C),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

@Composable
fun Zhirpem_appTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    // В вебе мы используем стандартные схемы, в Android - можем использовать динамические
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK, AppThemeMode.AMOLED -> if (themeMode == AppThemeMode.AMOLED) 
            darkColorScheme(primary = Color(0xFF8DE3B5), background = Color.Black, surface = Color(0xFF0A0A0A)) 
            else DarkColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
