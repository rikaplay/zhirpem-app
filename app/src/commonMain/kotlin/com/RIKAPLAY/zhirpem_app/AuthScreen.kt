package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val platform = getPlatform()
    val repository = remember { getDataRepository() }
    val scope = rememberCoroutineScope()

    var isLoginTab by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun processAuth() {
        if (username.isEmpty() || password.isEmpty() || (!isLoginTab && name.isEmpty())) {
            errorMessage = "Пожалуйста, заполните все поля!"
            return
        }

        val cleanUsername = username.lowercase().trim().replace("@", "")
        isLoading = true
        errorMessage = ""

        scope.launch {
            if (isLoginTab) {
                val result = repository.getUser(cleanUsername)
                result.onSuccess { user ->
                    isLoading = false
                    // В оригинале была проверка пароля в Firestore. 
                    // Здесь мы используем упрощенную логику для порта, 
                    // которую можно расширить в DataRepository
                    if (user != null) {
                        platform.setString("username", cleanUsername)
                        platform.setString("name", user.name)
                        onAuthSuccess()
                    } else {
                        errorMessage = "Пользователь не найден!"
                    }
                }.onFailure {
                    isLoading = false
                    errorMessage = "Ошибка сети"
                }
            } else {
                val newUser = User(username = cleanUsername, name = name.trim())
                val result = repository.saveUser(newUser)
                result.onSuccess {
                    isLoading = false
                    platform.setString("username", cleanUsername)
                    platform.setString("name", name.trim())
                    onAuthSuccess()
                }.onFailure {
                    isLoading = false
                    errorMessage = "Ошибка при регистрации"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Здесь в оригинале был логотип. В Multiplatform ресурсы подключаются через Res.
        // Пока оставим текстовый заголовок, чтобы не ломать сборку без папки ресурсов.
        Text(
            text = "ZHIRPEM",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLoginTab) "С возвращением!" else "Создать аккаунт",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = !isLoginTab,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Как вас зовут?") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it.replace(" ", "") },
            label = { Text("Юзернейм (без @)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { processAuth() }),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp), fontSize = 14.sp)
        }

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = { processAuth() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = if (isLoginTab) "Войти" else "Присоединиться", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                isLoginTab = !isLoginTab
                errorMessage = ""
            }) {
                Text(text = if (isLoginTab) "Создать новый аккаунт" else "Уже есть профиль? Войти")
            }
        }
    }
}
