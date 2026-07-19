package com.RIKAPLAY.zhirpem_app

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var currentTab by remember { mutableStateOf("Users") } // Вкладки: "Users" или "Posts"
    
    // Списки данных
    var users by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var posts by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Загрузка данных
    LaunchedEffect(currentTab) {
        isLoading = true
        if (currentTab == "Users") {
            db.collection("users").get().addOnSuccessListener { result ->
                users = result.map { it.data + ("id" to it.id) }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        } else {
            db.collection("zhirpem_posts").get().addOnSuccessListener { result ->
                posts = result.map { it.data + ("id" to it.id) }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val glassEnabled = LocalGlassEnabled.current
            GlassTopBar(
                isGlassEnabled = glassEnabled,
                title = { Text("Админ-панель 🛠️") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Вкладки
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Button(
                    onClick = { currentTab = "Users" }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == "Users") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentTab == "Users") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Пользователи") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { currentTab = "Posts" }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == "Posts") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentTab == "Posts") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Посты") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Отображение контента
                if (currentTab == "Users") {
                    FastUserAssigner(db) // Блок быстрого поиска и выдачи прав
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users) { user ->
                            UserAdminItem(user, db) { 
                                // Обновляем список после действия
                                db.collection("users").get().addOnSuccessListener { result ->
                                    users = result.map { it.data + ("id" to it.id) }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(posts) { post ->
                            PostAdminItem(post, db) { 
                                // Обновляем список после удаления
                                db.collection("zhirpem_posts").get().addOnSuccessListener { result ->
                                    posts = result.map { it.data + ("id" to it.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FastUserAssigner(db: FirebaseFirestore) {
    var searchQuery by remember { mutableStateOf("") }
    var targetUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Быстрый поиск и выдача прав", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("По тегу (без @)", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.lowercase().trim() },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            db.collection("users").document(searchQuery).get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        targetUser = doc.data?.plus("id" to doc.id)
                                        statusMessage = "Пользователь найден!"
                                    } else {
                                        targetUser = null
                                        statusMessage = "Пользователь не найден"
                                    }
                                }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Найти")
                }
            }

            // Кнопки управления если нашли
            targetUser?.let { user ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Управление: ${user["name"]} (@${user["id"]})", fontWeight = FontWeight.Medium)
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { updateBadge(db, user["id"] as String, blue = true, yellow = false) { statusMessage = "Синяя выдана!" } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) { Text("Синяя", fontSize = 12.sp) }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Button(
                        onClick = { updateBadge(db, user["id"] as String, blue = false, yellow = true) { statusMessage = "Желтая выдана!" } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) { Text("Желтая", fontSize = 12.sp) }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Button(
                        onClick = { updateBadge(db, user["id"] as String, blue = false, yellow = false) { statusMessage = "Галочки сняты!" } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Снять", fontSize = 12.sp) }
                }
            }

            if (statusMessage.isNotEmpty()) {
                Text(statusMessage, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun UserAdminItem(user: Map<String, Any>, db: FirebaseFirestore, onAction: () -> Unit) {
    val userId = user["id"] as String
    var newColor by remember { mutableStateOf(user["nameColor"] as? String ?: "#000000") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Имя: ${user["name"] ?: "Noname"}", fontWeight = FontWeight.Bold)
                    Text("@${userId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Бан
            val isBanned = user["isBanned"] as? Boolean ?: false
            Button(
                onClick = { 
                    db.collection("users").document(userId).update("isBanned", !isBanned)
                        .addOnSuccessListener { onAction() } 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isBanned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            ) {
                Text(if (isBanned) "Разбанить пользователя" else "Забанить пользователя")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Смена цвета ника
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newColor, 
                    onValueChange = { newColor = it }, 
                    label = { Text("HEX Цвет") }, 
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { 
                    if (newColor.startsWith("#") && (newColor.length == 7 || newColor.length == 9)) {
                        db.collection("users").document(userId).update("nameColor", newColor)
                            .addOnSuccessListener { onAction() } 
                    }
                }) { Text("Цвет") }
            }
        }
    }
}

@Composable
fun PostAdminItem(post: Map<String, Any>, db: FirebaseFirestore, onAction: () -> Unit) {
    val postId = post["id"] as String
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("От: ${post["author"] ?: "Аноним"} (@${post["handle"] ?: "user"})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(4.dp))
            Text(post["text"] as? String ?: "Нет текста", maxLines = 3)
            
            if (!post["imageUrl"].toString().isNullOrEmpty()) {
                Text("📎 Содержит медиа", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { 
                    db.collection("zhirpem_posts").document(postId).delete()
                        .addOnSuccessListener { onAction() } 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Удалить этот пост")
            }
        }
    }
}

fun updateBadge(db: FirebaseFirestore, userId: String, blue: Boolean, yellow: Boolean, onSuccess: () -> Unit) {
    db.collection("users").document(userId).update(
        mapOf(
            "blueBadge" to blue,
            "yellowBadge" to yellow
        )
    ).addOnSuccessListener { onSuccess() }
}
