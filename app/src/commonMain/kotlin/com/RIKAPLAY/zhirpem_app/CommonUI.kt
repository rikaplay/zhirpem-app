package com.RIKAPLAY.zhirpem_app

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAnimationsEnabled = compositionLocalOf { true }
val LocalGlassEnabled = compositionLocalOf { true }
val LocalGlassAlpha = compositionLocalOf { 0.15f }
val LocalFontSize = compositionLocalOf { 1.0f }

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK, BLACK
}
