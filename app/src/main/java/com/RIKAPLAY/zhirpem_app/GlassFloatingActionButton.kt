package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = glassAlpha))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
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
