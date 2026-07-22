package com.RIKAPLAY.zhirpem_app

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiquidLens(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .graphicsLayer {
                // Эффект размытия того, что под линзой (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        25f, 25f, android.graphics.Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                }
            }
            .drawWithContent {
                // Рисуем содержимое
                drawContent()
                
                // Эффект преломления: градиентная обводка имитирующая стекло
                val strokeWidth = 2.dp.toPx()
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f),
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.4f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    style = Stroke(width = strokeWidth)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun FluidSwipeBottomBar(
    isGlassEnabled: Boolean,
    glassAlpha: Float,
    items: List<Pair<String, String>>,
    selectedLabel: String,
    onTabSelected: (String) -> Unit
) {
    var barWidth by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(-1f) }
    var isPressing by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val selectedIndex = items.indexOfFirst { it.second == selectedLabel }

    // 1. Динамическая логика прозрачности и цвета (для светлой темы делаем плотнее)
    val dynamicAlpha = if (!isDark && isGlassEnabled) (glassAlpha + 0.15f).coerceAtMost(1f) else glassAlpha
    val dynamicBackground = if (isGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = dynamicAlpha)
        else Color.White.copy(alpha = 0.7f) // Эффект матового стекла на светлой теме
    } else {
        MaterialTheme.colorScheme.surface
    }

    val dynamicBorderColor = if (isGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.15f)
        else Color.Black.copy(alpha = 0.1f) // Темная обводка для видимости на светлом фоне
    } else {
        Color.Transparent
    }

    // Вычисляем текущий активный индекс на основе позиции пальца или выбора
    val segmentWidthPx = if (items.isNotEmpty()) barWidth / items.size else 0f
    val activeIndex = if (dragX != -1f && segmentWidthPx > 0) {
        (dragX / segmentWidthPx).toInt().coerceIn(0, items.size - 1)
    } else {
        selectedIndex
    }

    // Анимация смещения и размера пузыря
    val targetOffsetPx = if (dragX != -1f) {
        dragX - segmentWidthPx / 2
    } else {
        segmentWidthPx * selectedIndex
    }

    val indicatorOffset by animateDpAsState(
        targetValue = with(density) { targetOffsetPx.coerceIn(0f, barWidth - segmentWidthPx).toDp() },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "indicatorOffset"
    )

    val indicatorSize by animateDpAsState(
        targetValue = if (isPressing) 52.dp else 46.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "indicatorSize"
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
                        dragX = offset.x
                    },
                    onDrag = { change, _ ->
                        dragX = change.position.x
                    },
                    onDragEnd = {
                        isPressing = false
                        if (dragX != -1f && segmentWidthPx > 0) {
                            val finalIndex = (dragX / segmentWidthPx).toInt().coerceIn(0, items.size - 1)
                            onTabSelected(items[finalIndex].second)
                        }
                        dragX = -1f
                    },
                    onDragCancel = { 
                        isPressing = false
                        dragX = -1f 
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Слой фона (Блюр и основная панель)
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

        // 2. Слой "Жидкой линзы"
        if (activeIndex != -1) {
            val segmentWidthDp = with(density) { segmentWidthPx.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset + (segmentWidthDp - indicatorSize) / 2)
                    .size(indicatorSize)
                    .align(Alignment.CenterStart)
            ) {
                LiquidLens(modifier = Modifier.fillMaxSize()) {}
            }
        }

        // 3. Слой контента (Только иконки)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (icon, label) ->
                val isTargeted = index == activeIndex
                
                // Анимация масштаба иконки
                val iconScale by animateFloatAsState(
                    targetValue = if (isTargeted) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "iconScale"
                )

                // Анимация цвета иконки
                val iconColor by animateColorAsState(
                    targetValue = if (isTargeted) MaterialTheme.colorScheme.primary 
                                  else Color.Gray.copy(alpha = 0.5f),
                    animationSpec = spring(),
                    label = "iconColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .bounceClick { onTabSelected(label) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 24.sp,
                        color = iconColor,
                        modifier = Modifier.scale(iconScale)
                    )
                }
            }
        }
    }
}
