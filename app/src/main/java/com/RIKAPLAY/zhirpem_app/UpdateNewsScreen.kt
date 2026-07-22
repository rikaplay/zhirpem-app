package com.RIKAPLAY.zhirpem_app

import android.net.Uri
import android.text.Html
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.TimeZone

data class UpdateNews(
    val id: String = "",
    val title: String = "",
    val htmlBody: String = "",
    val imageUrl: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val version: String = "",
    val views: Int = 0,
    val likedBy: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNewsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""
    val db = FirebaseFirestore.getInstance()
    
    var newsList by remember { mutableStateOf(listOf<UpdateNews>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedNews by remember { mutableStateOf<UpdateNews?>(null) }
    var newsToDelete by remember { mutableStateOf<UpdateNews?>(null) }

    val lastReadVersion = remember { sharedPrefs.getString("last_read_news_version", "") ?: "" }
    val latestVersion = newsList.firstOrNull()?.version ?: ""

    // Обновляем статус прочтения при открытии списка (считаем все текущие новости увиденными)
    LaunchedEffect(newsList) {
        if (newsList.isNotEmpty()) {
            val topVersion = newsList.first().version
            if (topVersion != lastReadVersion) {
                sharedPrefs.edit().putString("last_read_news_version", topVersion).apply()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Добавление старой новости 20.07.2026, если её нет
        db.collection("update_news")
            .whereEqualTo("version", "1.5.1")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.set(2026, Calendar.JULY, 20, 12, 0, 0)
                    val oldNews = hashMapOf(
                        "title" to "Жирпем 1.5.1",
                        "version" to "1.5.1",
                        "htmlBody" to """
                            • добавили автоматическую проверку обновлений (BETA)<br>
                            • немного обновили поиск<br>
                            • пытались сделать уведомления (работают только анонсы, но они приходят)<br>
                            • при повороте экрана приложение не перезапускается<br>
                            • верхняя панель теперь нормально работает<br>
                            • добавили опциональный сплэш-скрин<br>
                            • убрана верхняя панель при горизонтальной ориентации<br>
                            • новая нижняя панель Liquid Glass©
                        """.trimIndent(),
                        "imageUrl" to "https://res.cloudinary.com/dcwp4nm3e/image/upload/v1715850000/update_banner_default.jpg",
                        "timestamp" to com.google.firebase.Timestamp(cal.time)
                    )
                    db.collection("update_news").add(oldNews)
                }
            }

        db.collection("update_news")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    newsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UpdateNews::class.java)?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val glassEnabled = LocalGlassEnabled.current
            GlassTopBar(
                isGlassEnabled = glassEnabled,
                title = { Text("Новости обновлений 🚀") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Секретная кнопка для @rikaplay
                    if (myUsername == "rikaplay") {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить новость", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (newsList.isEmpty()) {
                Text("Пока новостей нет...", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.outline)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newsList) { news ->
                        val isRead = sharedPrefs.getBoolean("news_read_${news.id}", false)
                        UpdateNewsCollapsedItem(
                            news = news,
                            myUsername = myUsername,
                            isRead = isRead,
                            onClick = {
                                if (!isRead) {
                                    sharedPrefs.edit().putBoolean("news_read_${news.id}", true).apply()
                                    db.collection("update_news").document(news.id).update("views", com.google.firebase.firestore.FieldValue.increment(1))
                                }
                                selectedNews = news
                            },
                            onLongClick = { newsToDelete = news }
                        )
                    }
                }
            }
        }
    }

    if (newsToDelete != null) {
        AlertDialog(
            onDismissRequest = { newsToDelete = null },
            title = { Text("Удалить новость?") },
            text = { Text("Вы действительно хотите удалить новость \"${newsToDelete?.title}\"? Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        newsToDelete?.let { news ->
                            db.collection("update_news").document(news.id).delete().addOnSuccessListener {
                                Toast.makeText(context, "Новость удалена", Toast.LENGTH_SHORT).show()
                                newsToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { newsToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (selectedNews != null) {
        NewsDetailDialog(
            news = selectedNews!!,
            myUsername = myUsername,
            onLikeClick = { newsId ->
                val newsRef = db.collection("update_news").document(newsId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(newsRef)
                    val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
                    if (myUsername !in likedBy) {
                        transaction.update(newsRef, "likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(myUsername))
                    } else {
                        transaction.update(newsRef, "likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(myUsername))
                    }
                }.addOnSuccessListener {
                    // Update the local selectedNews object to reflect the change
                    val updatedList = newsList.map { 
                        if (it.id == newsId) {
                            val newLikedBy = if (myUsername in it.likedBy) it.likedBy - myUsername else it.likedBy + myUsername
                            it.copy(likedBy = newLikedBy)
                        } else it
                    }
                    newsList = updatedList
                    selectedNews = updatedList.find { it.id == newsId }
                }
            },
            onDismiss = { selectedNews = null }
        )
    }

    if (showCreateDialog) {
        CreateNewsDialog(
            onDismiss = { showCreateDialog = false },
            onPost = { title, body, imgUrl, version ->
                val newsData = hashMapOf(
                    "title" to title,
                    "htmlBody" to body,
                    "imageUrl" to imgUrl,
                    "version" to version,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("update_news").add(newsData).addOnSuccessListener {
                    showCreateDialog = false
                    Toast.makeText(context, "Новость опубликована!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpdateNewsCollapsedItem(news: UpdateNews, myUsername: String, isRead: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (myUsername == "rikaplay") onLongClick else null
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = news.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                if (news.version.isNotEmpty()) {
                    Text(
                        text = "Версия ${news.version}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            news.timestamp?.let {
                val sdf = java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale("ru"))
                Text(
                    text = sdf.format(it.toDate()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun NewsDetailDialog(news: UpdateNews, myUsername: String, onLikeClick: (String) -> Unit, onDismiss: () -> Unit) {
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
                // Изображение сверху
                if (news.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = news.imageUrl,
                        contentDescription = "News Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = news.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (news.version.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = news.version,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 16f
                                setTextColor(onSurfaceColor)
                            }
                        },
                        update = {
                            it.text = Html.fromHtml(news.htmlBody, Html.FROM_HTML_MODE_COMPACT)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Лайки и просмотры
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isLiked = myUsername in news.likedBy
                            IconButton(onClick = { onLikeClick(news.id) }) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                    contentDescription = "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = news.likedBy.size.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(24.dp))
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Views",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = news.views.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Понятно", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    news.timestamp?.let {
                        val sdf = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("ru"))
                        Text(
                            text = sdf.format(it.toDate()),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewsDialog(onDismiss: () -> Unit, onPost: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var htmlBody by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            uploadImageToCloudinaryLocal(
                context = context,
                imageUri = it,
                cloudName = "dcwp4nm3e",
                uploadPreset = "ProfilePIC",
                onSuccess = { url ->
                    imageUrl = url
                    isUploading = false
                },
                onError = {
                    isUploading = false
                    Toast.makeText(context, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Новая новость") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                        },
                        actions = {
                            TextButton(
                                onClick = { onPost(title, htmlBody, imageUrl, version) },
                                enabled = title.isNotEmpty() && htmlBody.isNotEmpty() && !isUploading
                            ) {
                                Text("Опубликовать", fontWeight = FontWeight.Bold)
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
                        .verticalScroll(scrollState)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Заголовок") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Версия (напр. 1.5.2)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        NewsEditorButton("B") { htmlBody += "<b></b>" }
                        NewsEditorButton("I") { htmlBody += "<i></i>" }
                        NewsEditorButton("Link") { htmlBody += "<a href='URL'>Текст</a>" }
                        NewsEditorButton("Image") { galleryLauncher.launch("image/*") }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = htmlBody,
                        onValueChange = { htmlBody = it },
                        label = { Text("Текст (HTML)") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Предпросмотр:", fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (version.isNotEmpty()) {
                                Text("Версия: $version", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            AndroidView(
                                factory = { ctx -> TextView(ctx).apply { textSize = 14f } },
                                update = { it.text = Html.fromHtml(htmlBody, Html.FROM_HTML_MODE_COMPACT) }
                            )
                        }
                    }

                    if (isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsEditorButton(text: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, fontSize = 10.sp)
    }
}

private fun uploadImageToCloudinaryLocal(
    context: android.content.Context,
    imageUri: Uri,
    cloudName: String,
    uploadPreset: String,
    onSuccess: (String) -> Unit,
    onError: () -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val file = File(context.cacheDir, "news_upload_${System.currentTimeMillis()}.jpg")
        inputStream.use { input ->
            FileOutputStream(file).use { output -> input?.copyTo(output) }
        }

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { onError() }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val url = JSONObject(response.body?.string() ?: "{}").getString("secure_url")
                    onSuccess(url)
                } else { onError() }
            }
        })
    } catch (e: Exception) { onError() }
}
