package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.RIKAPLAY.zhirpem_app.ui.theme.Zhirpem_appTheme

@Composable
fun App() {
    val platform = getPlatform()
    
    // Подгружаем настройки через нашу кроссплатформенную обертку
    val isLoggedIn = remember { mutableStateOf(platform.getBoolean("is_logged_in", false)) }
    val isGlassEnabled = remember { mutableStateOf(platform.getBoolean("glass_enabled", true)) }
    val glassAlpha = remember { mutableStateOf(0.3f) }

    CompositionLocalProvider(
        LocalAnimationsEnabled provides true,
        LocalFontSize provides 1.0f,
        LocalGlassEnabled provides isGlassEnabled.value,
        LocalGlassAlpha provides glassAlpha.value
    ) {
        Zhirpem_appTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AnimatedContent(
                    targetState = isLoggedIn.value,
                    transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
                    label = "AuthTransition"
                ) { logged ->
                    if (!logged) {
                        AuthScreen(onAuthSuccess = { 
                            isLoggedIn.value = true 
                            platform.setBoolean("is_logged_in", true)
                        })
                    } else {
                        MainScreenContainer(onLogout = {
                            isLoggedIn.value = false
                            platform.setBoolean("is_logged_in", false)
                        })
                    }
                }
            }
        }
    }
}
