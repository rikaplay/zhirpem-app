package com.RIKAPLAY.zhirpem_app

import android.net.Uri
import android.text.Html
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.viewinterop.AndroidView
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var currentTab by remember { mutableStateOf("Users") } // Вкладки: "Users", "Posts", "Notifications"
    
    // Списки данных
    var users by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var posts by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Загрузка данных
    LaunchedEffect(currentTab) {
        if (currentTab == "Notifications") return@LaunchedEffect
        
        isLoading = true
        if (currentTab == "Users") {
            db.collection("users").get().addOnSuccessListener { result ->
                users = result.map { it.data + ("id" to it.id) }
                isLoading = false
            }.addOnFailureListener { isLoading = false }
        } else if (currentTab == "Posts") {
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
                ) { Text("Юзеры", fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { currentTab = "Posts" }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == "Posts") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentTab == "Posts") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Посты", fontSize = 12.sp) }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { currentTab = "Notifications" }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == "Notifications") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (currentTab == "Notifications") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Пуши", fontSize = 12.sp) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Отображение контента
                when (currentTab) {
                    "Users" -> {
                        FastUserAssigner(db)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(users) { user ->
                                UserAdminItem(user, db) { 
                                    db.collection("users").get().addOnSuccessListener { result ->
                                        users = result.map { it.data + ("id" to it.id) }
                                    }
                                }
                            }
                        }
                    }
                    "Posts" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(posts) { post ->
                                PostAdminItem(post, db) { 
                                    db.collection("zhirpem_posts").get().addOnSuccessListener { result ->
                                        posts = result.map { it.data + ("id" to it.id) }
                                    }
                                }
                            }
                        }
                    }
                    "Notifications" -> {
                        NotificationAdminTab(db)
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

@Composable
fun NotificationAdminTab(db: FirebaseFirestore) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var htmlBody by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Загрузка в Cloudinary
            val requestId = MediaManager.get().upload(it)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        imageUrl = resultData["secure_url"] as String
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Toast.makeText(context, "Ошибка загрузки: ${error.description}", Toast.LENGTH_SHORT).show()
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        Text("Создание уведомления", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rich Text Editor Helpers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EditorButton("B") { htmlBody += "<b></b>" }
            EditorButton("I") { htmlBody += "<i></i>" }
            EditorButton("Link") { htmlBody += "<a href='URL'>Текст</a>" }
            EditorButton("Color") { htmlBody += "<font color='#FF0000'></font>" }
            EditorButton("Mono") { htmlBody += "<font face='monospace'></font>" }
            EditorButton("Image") { galleryLauncher.launch("image/*") }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = htmlBody,
            onValueChange = { htmlBody = it },
            label = { Text("Текст (HTML)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = { Text("URL картинки (опционально)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Предпросмотр:", fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                AndroidView(
                    factory = { ctx -> TextView(ctx).apply { textSize = 14f } },
                    update = { it.text = Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_COMPACT) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isNotEmpty() && htmlBody.isNotEmpty()) {
                    isSending = true
                    // Используем CoroutineScope для запуска в фоновом потоке
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = withContext(Dispatchers.IO) {
                            sendGlobalNotification(db, title, htmlBody, imageUrl)
                        }
                        isSending = false
                        if (success) {
                            Toast.makeText(context, "Отправлено всем пользователям!", Toast.LENGTH_SHORT).show()
                            title = ""
                            htmlBody = ""
                            imageUrl = ""
                        } else {
                            Toast.makeText(context, "Ошибка при отправке. Проверьте Logcat", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSending,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSending) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text("Отправить всем пользователям (Push + DB)")
        }
    }
}

@Composable
fun EditorButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, fontSize = 10.sp)
    }
}

suspend fun sendGlobalNotification(db: FirebaseFirestore, title: String, htmlBody: String, imageUrl: String): Boolean = withContext(Dispatchers.IO) {
    try {
        // 1. Сохранение в Firestore (выполняется синхронно в IO через tasks-play-services)
        val notificationData = hashMapOf(
            "title" to title,
            "htmlBody" to htmlBody,
            "bigPictureUrl" to imageUrl,
            "timestamp" to FieldValue.serverTimestamp(),
            "type" to "ADMIN",
            "receiverId" to "ALL",
            "senderName" to "Zhirpem"
        )

        // Дожидаемся завершения записи в БД
        com.google.android.gms.tasks.Tasks.await(db.collection("notifications").add(notificationData))

        // 2. Отправка через OneSignal REST API
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("app_id", "e52144a6-d4ea-46a4-870f-4089ec7a6af9")
        
        // Отправка ВСЕМ активным подписчикам
        json.put("included_segments", org.json.JSONArray(listOf("Subscribed Users")))
        
        val contents = JSONObject()
        val plainText = Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_COMPACT).toString()
        contents.put("en", plainText)
        json.put("contents", contents)
        
        val headings = JSONObject()
        headings.put("en", title)
        json.put("headings", headings)

        if (imageUrl.isNotEmpty()) {
            json.put("big_picture", imageUrl)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .post(requestBody)
                .addHeader("Authorization", "Basic os_v2_app_4uqujjwu5jdkjbypice6y6tk7hgycpqrjz7el4eil7itrf3xlinih6ikgpfq6o5l43izejzh4wdmjtrszqsdjvzj455p7mvulqousny")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            Log.d("PushDebug", "Sending to AppID: e52144a6-d4ea-46a4-870f-4089ec7a6af9")
            
            val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        val isSuccessful = response.isSuccessful
        
        if (!isSuccessful) {
            Log.e("PushError", "OneSignal API Error: Code ${response.code}, Body: $responseBody")
        }
        
        response.close()
        isSuccessful
    } catch (e: Exception) {
        Log.e("PushError", "Critical Failure: ${e.message}", e)
        false
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
