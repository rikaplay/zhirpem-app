package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FluidSwipeBottomBar(
    isGlassEnabled: Boolean,
    glassAlpha: Float,
    items: List<Pair<String, String>>,
    selectedLabel: String,
    onTabSelected: (String) -> Unit
) {
    var barWidth by remember { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    var isPressing by remember { mutableStateOf(false) }
    
    val selectedIndex = items.indexOfFirst { it.second == selectedLabel }
    val activeIndex = if (hoveredIndex != -1) hoveredIndex else selectedIndex

    // Анимация цвета овала: становится плотнее при зажатии
    val indicatorColor by animateColorAsState(
        targetValue = if (isPressing) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                      else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        animationSpec = spring(),
        label = "indicatorColor"
    )

    // Анимация горизонтального паддинга: овал сжимается при зажатии
    val indicatorPadding by animateDpAsState(
        targetValue = if (isPressing) 14.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "indicatorPadding"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp)
            .onGloballyPositioned { barWidth = it.size.width.toFloat() }
            .pointerInput(items.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isPressing = true
                        val segmentWidth = barWidth / items.size
                        if (segmentWidth > 0) {
                            hoveredIndex = (offset.x / segmentWidth).toInt().coerceIn(0, items.size - 1)
                        }
                    },
                    onDrag = { change, _ ->
                        val segmentWidth = barWidth / items.size
                        if (segmentWidth > 0) {
                            hoveredIndex = (change.position.x / segmentWidth).toInt().coerceIn(0, items.size - 1)
                        }
                    },
                    onDragEnd = {
                        isPressing = false
                        if (hoveredIndex != -1) {
                            onTabSelected(items[hoveredIndex].second)
                        }
                        hoveredIndex = -1
                    },
                    onDragCancel = { 
                        isPressing = false
                        hoveredIndex = -1 
                    }
                )
            },
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

        // 2. Верхний слой (Контент)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val isSelected = index == activeIndex
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bounceClick { onTabSelected(label) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = indicatorPadding)
                                .height(36.dp)
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(
                                    if (isGlassEnabled) indicatorColor
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
