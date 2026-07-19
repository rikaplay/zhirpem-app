package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

// Оптимизированная мягкая пружина: высокая жесткость, среднее затухание (без эффекта желе)
val premiumSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

val premiumBoundsSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

fun Modifier.bounceClick(onClick: () -> Unit = {}): Modifier = this.composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = LocalAnimationsEnabled.current
    
    // Анимируем масштаб: при нажатии уменьшаем до 92%, при отпускании возвращаем 100%
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationsEnabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh // Высокая скорость отклика
        ),
        label = "bounceAnimation"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Убираем стандартную серую вспышку (ripple), оставляя чистый премиальный bounce
            onClick = onClick
        )
}

fun Modifier.liquidGlassEffect(isGlassEnabled: Boolean): Modifier = if (isGlassEnabled) {
    this
        .blur(16.dp)
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.15f),
            shape = CircleShape
        )
} else {
    this
}
