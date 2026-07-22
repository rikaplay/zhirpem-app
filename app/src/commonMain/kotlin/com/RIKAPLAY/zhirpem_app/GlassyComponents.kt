package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalBackgroundBlurEnabled = staticCompositionLocalOf { true }
val LocalGlassAlpha = staticCompositionLocalOf { 0.3f }

@Composable
fun GlassyBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 15.dp,
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isGlassEnabled = LocalGlassEnabled.current
    val isBgBlurEnabled = LocalBackgroundBlurEnabled.current
    val glassAlpha = LocalGlassAlpha.current
    val isDark = isSystemInDarkTheme()

    val dynamicAlpha = if (!isDark && isGlassEnabled) (glassAlpha + 0.15f).coerceAtMost(1f) else glassAlpha
    val backgroundColor = containerColor ?: if (isGlassEnabled) {
        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = dynamicAlpha)
        else Color.White.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isGlassEnabled) {
        if (isDark) Color.White.copy(alpha = 0.1f)
        else Color.Black.copy(alpha = 0.05f)
    } else {
        Color.Transparent
    }

    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (isGlassEnabled) {
                        Modifier
                            .blur(if (isBgBlurEnabled) blurRadius else 0.dp)
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    }
                )
        )
        content()
    }
}
