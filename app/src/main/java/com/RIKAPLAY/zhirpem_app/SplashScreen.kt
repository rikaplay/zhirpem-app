package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isEnabled: Boolean,
    onNavigateToMain: () -> Unit,
    viewModel: FeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var isScaled by remember { mutableStateOf(false) }

    // Параллельная загрузка данных
    LaunchedEffect(Unit) {
        viewModel.fetchPosts()
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isScaled) 4f else 1f,
        animationSpec = tween(800),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isScaled) 0f else 1f,
        animationSpec = tween(800),
        label = "alpha"
    )

    // Автоматический запуск логики
    LaunchedEffect(isEnabled) {
        if (!isEnabled) {
            onNavigateToMain()
            return@LaunchedEffect
        }

        // Задержка перед началом анимации
        delay(200)
        
        // Запуск фонового звука через синглтон, если включен в настройках
        if (settingsManager.isSplashSoundEnabled) {
            SoundManager.playSplashSound(context)
        }
        
        // Запуск анимации
        isScaled = true
        
        // Ожидание завершения анимации (800мс) и переход
        delay(800)
        onNavigateToMain()
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
            )
        }
    }
}
