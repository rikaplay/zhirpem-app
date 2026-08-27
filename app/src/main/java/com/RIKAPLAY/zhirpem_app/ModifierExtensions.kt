package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Адаптивное ограничение размера изображения для горизонтальной ориентации.
 * В Landscape режиме ограничивает высоту, чтобы оставить место для интерфейса.
 */
fun Modifier.adaptiveImageSize(isLandscape: Boolean): Modifier {
    return if (isLandscape) {
        this
            .heightIn(max = 220.dp)
            .fillMaxHeight(0.5f)
    } else {
        this
    }
}

/**
 * Анимация нажатия (пружинный отскок)
 */
fun Modifier.bounceClick() = composed {
    var buttonState by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (buttonState) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(false)
                    true
                }
            }
        }
}
