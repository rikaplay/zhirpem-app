package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = glassAlpha))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
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
