package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.RIKAPLAY.zhirpem_app.ui.theme.Zhirpem_appTheme

@Composable
fun App() {
    val platform = getPlatform()
    val repository = remember { getDataRepository() }
    var isLoggedIn by remember { mutableStateOf(platform.getBoolean("is_logged_in", false)) }
    
    Zhirpem_appTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!isLoggedIn) {
                AuthScreen(onAuthSuccess = { 
                    isLoggedIn = true 
                    platform.setBoolean("is_logged_in", true)
                })
            } else {
                MainScreen(repository, onLogout = {
                    isLoggedIn = false
                    platform.setBoolean("is_logged_in", false)
                })
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Zhirpem Multiplatform", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Юзернейм") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                isLoading = true
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    onAuthSuccess()
                }
                isLoading = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Войти")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: DataRepository, onLogout: () -> Unit) {
    val posts by repository.getPosts().collectAsState(emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zhirpem Feed") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Выйти") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts) { post ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(post.author, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(post.text)
                    }
                }
            }
        }
    }
}
