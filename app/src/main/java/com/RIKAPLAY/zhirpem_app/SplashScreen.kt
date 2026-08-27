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
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isEnabled: Boolean,
    onNavigateToMain: () -> Unit,
    isPremium: Boolean = false,
    viewModel: FeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var isScaled by remember { mutableStateOf(false) }
    var showBadge by remember { mutableStateOf(false) }

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

    // Анимации для премиум-версии
    val logoAlphaPremium by animateFloatAsState(
        targetValue = if (showBadge) 0f else 1f,
        animationSpec = tween(500),
        label = "logoAlphaPremium"
    )

    val badgeAlpha by animateFloatAsState(
        targetValue = if (showBadge && !isScaled) 1f else if (isScaled) 0f else 0f,
        animationSpec = tween(if (isScaled) 800 else 500),
        label = "badgeAlpha"
    )

    val badgeScale by animateFloatAsState(
        targetValue = if (isScaled) 4f else 1f,
        animationSpec = tween(800),
        label = "badgeScale"
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
            SoundManager.playSplashSound(context, isPremium)
        }
        
        if (isPremium) {
            // 1. Показываем лого (оно и так в начале 1f)
            delay(500)
            // 2. Скрываем лого и проявляем галочку
            showBadge = true
            delay(600)
            // 3. Увеличиваем галочку и скрываем
            isScaled = true
            delay(800)
        } else {
            // Обычная анимация
            isScaled = true
            delay(800)
        }
        
        onNavigateToMain()
    }

    if (isEnabled) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isPremium) {
                // Премиум сплэш: Лого -> Галочка
                Image(
                    painter = painterResource(id = R.drawable.jirpem_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .alpha(logoAlphaPremium)
                )

                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Premium Badge",
                    tint = Color.Blue,
                    modifier = Modifier
                        .size(150.dp) // Размер как у лого
                        .scale(badgeScale)
                        .alpha(badgeAlpha)
                )
            } else {
                // Обычный сплэш
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
}
