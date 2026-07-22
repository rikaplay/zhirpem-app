package com.RIKAPLAY.zhirpem_app

import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isEnabled: Boolean,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 4f else 1f,
        animationSpec = tween(800),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 1f,
        animationSpec = tween(800),
        label = "alpha"
    )

    val mediaPlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.splash_sound)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    LaunchedEffect(Unit) {
        if (!isEnabled) {
            onNavigateToMain()
        }
    }

    if (isEnabled) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.jirpem_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!startAnimation) {
                            mediaPlayer?.start()
                            startAnimation = true
                        }
                    }
            )
        }

        LaunchedEffect(startAnimation) {
            if (startAnimation) {
                delay(800)
                onNavigateToMain()
            }
        }
    }
}
