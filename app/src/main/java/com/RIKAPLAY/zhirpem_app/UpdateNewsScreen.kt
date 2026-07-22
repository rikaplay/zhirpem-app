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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

data class UpdateNews(
    val id: String = "",
    val title: String = "",
    val htmlBody: String = "",
    val imageUrl: String = "",
    val timestamp: com.google.firebase.Timestamp? = null,
    val version: String = ""
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

    LaunchedEffect(Unit) {
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
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(newsList) { news ->
                        UpdateNewsItem(news)
                    }
                }
            }
        }
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

@Composable
fun UpdateNewsItem(news: UpdateNews) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (news.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = news.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = news.title,
                        style = MaterialTheme.typography.headlineSmall,
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AndroidView(
                    factory = { ctx -> TextView(ctx).apply { 
                        textSize = 16f
                        setTextColor(android.graphics.Color.BLACK) // Or use current theme color
                    } },
                    update = { 
                        it.text = Html.fromHtml(news.htmlBody, Html.FROM_HTML_MODE_COMPACT)
                        // Note: If using dark theme, you might need to adjust text color dynamically
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                news.timestamp?.let {
                    val sdf = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("ru"))
                    Text(
                        text = sdf.format(it.toDate()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
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
            MediaManager.get().upload(it)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        imageUrl = resultData["secure_url"] as String
                        isUploading = false
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Toast.makeText(context, "Ошибка загрузки: ${error.description}", Toast.LENGTH_SHORT).show()
                        isUploading = false
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
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
                        EditorButton("B") { htmlBody += "<b></b>" }
                        EditorButton("I") { htmlBody += "<i></i>" }
                        EditorButton("Link") { htmlBody += "<a href='URL'>Текст</a>" }
                        EditorButton("Image") { galleryLauncher.launch("image/*") }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = htmlBody,
                        onValueChange = { htmlBody = it },
                        label = { Text("Текст (HTML)") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                    
                    if (imageUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Картинка выбрана ✅", color = Color.Green, fontWeight = FontWeight.Bold)
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
