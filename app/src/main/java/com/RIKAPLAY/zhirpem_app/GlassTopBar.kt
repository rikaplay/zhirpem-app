package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassTopBar(
    isGlassEnabled: Boolean,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    val glassAlpha = LocalGlassAlpha.current
    val isDark = isSystemInDarkTheme()

    // Динамическая логика для обеспечения видимости на светлой теме
    val dynamicAlpha = if (!isDark && isGlassEnabled) (glassAlpha + 0.15f).coerceAtMost(1f) else glassAlpha
    val dynamicBackground = if (isGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = dynamicAlpha)
        else Color.White.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val dynamicBorderColor = if (isGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.15f)
        else Color.Black.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Нижний слой (Блюр и фон)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(
                    if (isGlassEnabled) {
                        Modifier
                            .blur(20.dp)
                            .background(dynamicBackground)
                            .border(1.dp, dynamicBorderColor, CircleShape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    }
                )
        )

        // 2. Верхний слой (Контент без блюра)
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                navigationIcon()
            }
            
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                title()
            }
            
            Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                actions()
            }
        }
    }
}
