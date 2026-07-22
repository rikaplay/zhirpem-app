package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf

// Оригинальные параметры анимации из твоего проекта
val premiumSpring = spring<Float>(
    dampingRatio = 0.7f,
    stiffness = Spring.StiffnessLow
)

val LocalAnimationsEnabled = staticCompositionLocalOf { true }
val LocalFontSize = staticCompositionLocalOf { 1.0f }
