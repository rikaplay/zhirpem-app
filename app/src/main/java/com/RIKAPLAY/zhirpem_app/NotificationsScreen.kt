@file:OptIn(ExperimentalMaterial3Api::class)

package com.RIKAPLAY.zhirpem_app

import android.text.Html
import android.widget.TextView
import android.widget.Toast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale

// ==========================================
// 1. МОДЕЛЬ ДАННЫХ И ТИПЫ
// ==========================================

enum class NotificationType {
    LIKE, COMMENT, FOLLOW, MESSAGE, ADMIN
}

data class NotificationModel(
    val id: String = "",
    val senderId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val targetText: String = "", // Текст поста или коммента, который оценили
    val userComment: String = "", // Сам комментарий, если это тип COMMENT или HTML тело для ADMIN
    val timestamp: Timestamp? = null,
    val receiverId: String = "",
    val postId: String? = null,
    val bigPictureUrl: String = ""
)

// ==========================================
// 2. ГЛАВНЫЙ ЭКРАН УВЕДОМЛЕНИЙ
// ==========================================

@Composable
fun NotificationsScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("username", "") ?: ""
    
    var notificationsList by remember { mutableStateOf(listOf<NotificationModel>()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()

    // Подтягиваем постоянно обновляющийся список из бэкенда
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("notifications")
                .whereIn("receiverId", listOf(currentUserId, "ALL"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        notificationsList = snapshot.documents.mapNotNull { doc ->
                            val typeStr = doc.getString("type") ?: "LIKE"
                            val type = try { NotificationType.valueOf(typeStr) } catch (e: Exception) { NotificationType.LIKE }
                            
                            NotificationModel(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: "",
                                username = doc.getString("senderName") ?: "Пользователь",
                                userAvatarUrl = doc.getString("senderAvatarUrl") ?: "",
                                type = type,
                                targetText = doc.getString("title") ?: doc.getString("targetText") ?: "",
                                userComment = doc.getString("htmlBody") ?: doc.getString("userComment") ?: doc.getString("text") ?: "",
                                timestamp = doc.getTimestamp("timestamp"),
                                receiverId = doc.getString("receiverId") ?: "",
                                postId = doc.getString("postId"),
                                bigPictureUrl = doc.getString("bigPictureUrl") ?: ""
                            )
                        }
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Уведомления", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (notificationsList.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Здесь пока пусто", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                val scope = rememberCoroutineScope()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(notificationsList, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.StartToEnd) {
                                    // Удаление из Firestore
                                    db.collection("notifications").document(item.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
                                        }
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromEndToStart = false, // Только свайп вправо
                            backgroundContent = {
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> Color.Red.copy(alpha = 0.8f)
                                        else -> Color.Transparent
                                    }, label = "dismiss_bg"
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = Color.White
                                    )
                                }
                            }
                        ) {
                            Column {
                                NotificationItem(
                                    notification = item,
                                    onFollowClick = { 
                                        if (currentUserId.isNotEmpty() && item.senderId.isNotEmpty()) {
                                            val followData = hashMapOf(
                                                "follower" to currentUserId,
                                                "following" to item.senderId,
                                                "timestamp" to FieldValue.serverTimestamp()
                                            )
                                            db.collection("follows").add(followData)
                                        }
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ЭЛЕМЕНТ СПИСКА УВЕДОМЛЕНИЙ
// ==========================================

@Composable
fun NotificationItem(
    notification: NotificationModel,
    onFollowClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. БЛОК АВАТАРКИ СО ЗНАЧКОМ ДЕЙСТВИЯ
        Box(modifier = Modifier.size(48.dp)) {
            // Главная аватарка пользователя
            AsyncImage(
                model = if (notification.type == NotificationType.ADMIN) R.drawable.jirpem_logo else notification.userAvatarUrl.ifEmpty { "https://placehold.co/100x100.png" },
                contentDescription = "Аватарка",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Маленький значок в правом нижнем углу аватарки
            val (badgeIcon, badgeColor) = when (notification.type) {
                NotificationType.LIKE -> Icons.Default.Favorite to Color(0xFFEF5350) // Красный лайк
                NotificationType.COMMENT -> Icons.Default.Comment to MaterialTheme.colorScheme.primary // Зеленый (Material You)
                NotificationType.FOLLOW -> Icons.Default.PersonAdd to Color(0xFF4CAF50) // Зеленый плюс
                NotificationType.MESSAGE -> Icons.Default.Email to Color(0xFF2196F3) // Синий конверт
                NotificationType.ADMIN -> Icons.Default.Notifications to Color(0xFFFF9800) // Оранжевый колокольчик
            }

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. ИНФОРМАЦИОННЫЙ ТЕКСТ (Имя + Действие)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val annotatedText = buildAnnotatedString {
                // Жирное имя пользователя
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)) {
                    append(if (notification.type == NotificationType.ADMIN) "Zhirpem" else notification.username)
                }
                append(" ")
                // Описание действия
                val actionText = when (notification.type) {
                    NotificationType.LIKE -> "оценил(а) вашу запись"
                    NotificationType.COMMENT -> "прокомментировал(а) вашу запись"
                    NotificationType.FOLLOW -> "подписался(-ась) на вас"
                    NotificationType.MESSAGE -> "отправил(а) вам сообщение"
                    NotificationType.ADMIN -> "" // Для админа заголовок в targetText
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 14.sp)) {
                    append(actionText)
                }
            }

            Text(text = annotatedText)

            // Заголовок для ADMIN
            if (notification.type == NotificationType.ADMIN && notification.targetText.isNotEmpty()) {
                Text(
                    text = notification.targetText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Детали (текст комментария или поста, или HTML)
            if (notification.type == NotificationType.ADMIN) {
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 14f
                            setTextColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { textView ->
                        textView.text = Html.fromHtml(notification.userComment, Html.FROM_HTML_MODE_COMPACT)
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (notification.bigPictureUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = notification.bigPictureUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (notification.type == NotificationType.COMMENT && notification.userComment.isNotEmpty()) {
                Text(
                    text = notification.userComment,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            } else if (notification.targetText.isNotEmpty()) {
                Text(
                    text = notification.targetText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            
            // Время уведомления (относительное)
            Text(
                text = formatRelativeTime(notification.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        // 3. КНОПКА «ПОДПИСАТЬСЯ В ОТВЕТ»
        if (notification.type == NotificationType.FOLLOW) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = "Подписаться", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==========================================
// 4. ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ==========================================

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "только что"
    val now = System.currentTimeMillis()
    val time = timestamp.toDate().time
    val diff = now - time
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "только что"
        minutes < 60 -> "${minutes}м"
        hours < 24 -> "${hours}ч"
        days < 30 -> "${days}д"
        else -> {
            val sdf = SimpleDateFormat("d MMM", Locale("ru"))
            sdf.format(timestamp.toDate())
        }
    }
}
