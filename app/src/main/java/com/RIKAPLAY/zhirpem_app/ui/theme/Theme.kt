package com.RIKAPLAY.zhirpem_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.RIKAPLAY.zhirpem_app.AppThemeMode
import com.RIKAPLAY.zhirpem_app.ThemeManager

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
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val isSystemDark = isSystemInDarkTheme()
    
    val colorScheme = when (themeManager.themeType) {
        ThemeManager.TYPE_MY_LIGHT -> {
            themeManager.generateColorScheme(themeManager.getCustomColorObj(), false)
        }
        ThemeManager.TYPE_MY_DARK -> {
            themeManager.generateColorScheme(themeManager.getCustomColorObj(), true)
        }
        else -> {
            val darkTheme = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK, AppThemeMode.AMOLED -> true
                AppThemeMode.SYSTEM -> isSystemDark
                AppThemeMode.MATERIAL_YOU_LIGHT -> false
                AppThemeMode.MATERIAL_YOU_DARK -> true
            }

            when {
                themeMode == AppThemeMode.MATERIAL_YOU_LIGHT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    dynamicLightColorScheme(context)
                }
                themeMode == AppThemeMode.MATERIAL_YOU_DARK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    dynamicDarkColorScheme(context)
                }
                themeMode == AppThemeMode.AMOLED -> darkColorScheme(
                    primary = Color(0xFF8DE3B5),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0A0A0A)
                )
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
    }

    // Определяем darkTheme для статус-бара
    val darkThemeForStatus = when (themeManager.themeType) {
        ThemeManager.TYPE_MY_LIGHT -> false
        ThemeManager.TYPE_MY_DARK -> true
        else -> when (themeMode) {
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK, AppThemeMode.AMOLED -> true
            AppThemeMode.SYSTEM -> isSystemDark
            AppThemeMode.MATERIAL_YOU_LIGHT -> false
            AppThemeMode.MATERIAL_YOU_DARK -> true
        }
    }

    // Покраска статус-бара
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkThemeForStatus
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
