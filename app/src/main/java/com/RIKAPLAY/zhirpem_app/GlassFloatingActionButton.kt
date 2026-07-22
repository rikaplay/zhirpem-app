package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    isGlassEnabled: Boolean,
    glassAlpha: Float,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Динамическая логика для обеспечения видимости на светлой теме
    val dynamicAlpha = if (!isDark && isGlassEnabled) (glassAlpha + 0.15f).coerceAtMost(1f) else glassAlpha
    val dynamicBackground = if (isGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = dynamicAlpha)
        else Color.White.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val dynamicBorderColor = if (isGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.15f)
        else Color.Black.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 1. Динамический фон
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(
                    if (isGlassEnabled) {
                        Modifier
                            .blur(12.dp)
                            .background(dynamicBackground)
                            .border(1.dp, dynamicBorderColor, CircleShape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    }
                )
        )

        // 2. Иконка
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Создать",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}
