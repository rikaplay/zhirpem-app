package com.RIKAPLAY.zhirpem_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
private fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 500)
    return ColorScheme(
        primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value,
        surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "surfaceContainerHighest").value,
        surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "surfaceContainerLowest").value,
        primaryFixed = animateColorAsState(targetColorScheme.primaryFixed, animationSpec, label = "primaryFixed").value,
        onPrimaryFixed = animateColorAsState(targetColorScheme.onPrimaryFixed, animationSpec, label = "onPrimaryFixed").value,
        primaryFixedDim = animateColorAsState(targetColorScheme.primaryFixedDim, animationSpec, label = "primaryFixedDim").value,
        onPrimaryFixedVariant = animateColorAsState(targetColorScheme.onPrimaryFixedVariant, animationSpec, label = "onPrimaryFixedVariant").value,
        secondaryFixed = animateColorAsState(targetColorScheme.secondaryFixed, animationSpec, label = "secondaryFixed").value,
        onSecondaryFixed = animateColorAsState(targetColorScheme.onSecondaryFixed, animationSpec, label = "onSecondaryFixed").value,
        secondaryFixedDim = animateColorAsState(targetColorScheme.secondaryFixedDim, animationSpec, label = "secondaryFixedDim").value,
        onSecondaryFixedVariant = animateColorAsState(targetColorScheme.onSecondaryFixedVariant, animationSpec, label = "onSecondaryFixedVariant").value,
        tertiaryFixed = animateColorAsState(targetColorScheme.tertiaryFixed, animationSpec, label = "tertiaryFixed").value,
        onTertiaryFixed = animateColorAsState(targetColorScheme.onTertiaryFixed, animationSpec, label = "onTertiaryFixed").value,
        tertiaryFixedDim = animateColorAsState(targetColorScheme.tertiaryFixedDim, animationSpec, label = "tertiaryFixedDim").value,
        onTertiaryFixedVariant = animateColorAsState(targetColorScheme.onTertiaryFixedVariant, animationSpec, label = "onTertiaryFixedVariant").value,
    )
}

@Composable
fun Zhirpem_appTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val isSystemDark = isSystemInDarkTheme()
    
    val targetColorScheme = when (themeManager.themeType) {
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

    val colorScheme = animateColorScheme(targetColorScheme)

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
