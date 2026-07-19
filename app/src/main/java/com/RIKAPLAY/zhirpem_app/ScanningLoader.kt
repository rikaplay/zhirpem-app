package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScanningLoader(modifier: Modifier = Modifier) {
    // Бесконечная анимация смещения от 0 до 1 за 1.2 секунды внутри общего цикла
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    val accentColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val width = size.width
        val height = size.height
        val currentX = width * scanProgress

        // Рисуем мягкую горизонтальную линию-сканер со свечением по бокам
        drawCircle(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.6f),
                    accentColor,
                    accentColor.copy(alpha = 0.6f),
                    Color.Transparent
                ),
                startX = currentX - (width * 0.2f),
                endX = currentX + (width * 0.2f)
            ),
            radius = height / 2,
            center = Offset(currentX, height / 2)
        )
    }
}
