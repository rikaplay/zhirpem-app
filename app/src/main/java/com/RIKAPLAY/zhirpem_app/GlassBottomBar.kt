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
import androidx.compose.ui.unit.sp

@Composable
fun GlassBottomBar(
    isGlassEnabled: Boolean,
    items: List<Pair<String, String>>,
    selectedLabel: String,
    onItemClick: (String) -> Unit
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
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp),
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
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (icon, label) ->
                val isSelected = label == selectedLabel
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bounceClick { onItemClick(label) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGlassEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                        )
                    }
                    
                    Text(
                        text = icon,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
