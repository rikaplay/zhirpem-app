package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Изображение
                AsyncImage(
                    model = "https://i.imgur.com/uRjXp7D.png", // Замените на реальную ссылку или R.drawable
                    contentDescription = "What's New Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Что нового в версии 1.5.1?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val features = listOf(
                        "добавили автоматическую проверку обновлений (BETA)",
                        "немного обновили поиск",
                        "пытались сделать уведомления (работают только анонсы, но они приходят)",
                        "при повороте экрана приложение не перезапускается",
                        "верхняя панель теперь нормально работает",
                        "добавили опциональный сплэш-скрин (можно настроить в настройках)",
                        "убрана верхняя панель при горизонтальной ориентации",
                        "при прокрутке вниз, меню профиля скрывается. Чтобы оно появилось, просто прокрутите вверх",
                        "и наше самое главное — НОВАЯ НИЖНЯЯ ПАНЕЛЬ!"
                    )

                    features.forEach { feature ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = "•", modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Она работает по принципу Liquid Glass© от Apple со своей физикой и логикой интерфейса (попробуйте провести пальцем по ней, удивитесь), где можно настроить ее прозрачность в настройках или вовсе, убрать ее. Также мы убрали подписи над иконками т.к. они занимают лишнее пространство.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "На этом у нас все. Скоро должна выйти версия 1.5.2, где мы починим баги и добавим новые функции.\n\nДо встречи в новом обновлении.\n\nКоманда Жирпем.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Понятно", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
