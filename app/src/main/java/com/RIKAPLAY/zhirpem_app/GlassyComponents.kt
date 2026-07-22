package com.RIKAPLAY.zhirpem_app

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

    Box(
        modifier = modifier
            .clip(shape)
    ) {
        // Background Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (isGlassEnabled) {
                        Modifier
                            .let { 
                                if (isBgBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    it.graphicsLayer {
                                        renderEffect = android.graphics.RenderEffect
                                            .createBlurEffect(blurRadius.value * 2, blurRadius.value * 2, android.graphics.Shader.TileMode.DECAL)
                                            .asComposeRenderEffect()
                                    }
                                } else if (isBgBlurEnabled) {
                                    it.blur(blurRadius)
                                } else it
                            }
                            .background(backgroundColor)
                            .border(1.dp, borderColor, shape)
                    } else {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    }
                )
        )

        // Content Layer
        content()
    }
}

@Composable
fun FullScreenGlassWrapper(
    content: @Composable () -> Unit
) {
    val isGlassEnabled = LocalGlassEnabled.current
    val isDark = isSystemInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isGlassEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) Color.Black.copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.15f)
                    )
            )
        }
        content()
    }
}
