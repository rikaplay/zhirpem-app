package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var isLoggedIn by remember { mutableStateOf(false) }
            
            if (!isLoggedIn) {
                AuthScreenContent(onAuthSuccess = { isLoggedIn = true })
            } else {
                MainFeedScreen(
                    onUserClick = {},
                    onHashtagClick = {},
                    onMenuClick = {},
                    onAdminAccess = {},
                    onShowWhatsNew = {},
                    currentAvatarUrl = null,
                    currentName = "User",
                    showBackupWarning = false,
                    onNavigateToSecurity = {},
                    onDismissBackupWarning = {}
                )
            }
        }
    }
}
