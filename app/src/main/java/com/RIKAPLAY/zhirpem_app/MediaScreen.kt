@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.ui.unit.IntOffset
import kotlin.OptIn
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// 1. МОДЕЛИ ДАННЫХ
// ==========================================

enum class MediaType {
    IMAGE, GIF, VIDEO, NONE
}

enum class ChatInputMode {
    AUDIO, VIDEO
}

data class PollData(
    val question: String = "",
    val options: List<String> = emptyList(),
    val anonymous: Boolean = true,
    val multipleChoice: Boolean = false,
    val votes: Map<String, List<String>> = emptyMap() // Map of option index to list of user IDs
)

data class Post(
    val id: String = "",
    val author: String = "",
    val date: String = "",
    val handle: String = "",
    @get:com.google.firebase.firestore.PropertyName("isMedia")
    @set:com.google.firebase.firestore.PropertyName("isMedia")
    var isMedia: Boolean = false,
    val imageUrl: String? = null,
    val mediaUrl: String = "",        // Ссылка на файл (картинка, gif или mp4)
    val mediaType: MediaType = MediaType.NONE, // Тип контента
    val authorAvatarUrl: String? = null, // Ссылка на аватар автора
    val blueBadge: Boolean = false, // Синяя галочка
    val yellowBadge: Boolean = false, // Желтая галочка
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val text: String = "",
    val time: String = "",
    val views: Int = 0,
    val likedBy: List<String> = emptyList(),
    val bookmarkedBy: List<String> = emptyList(), // Кто сохранил
    val repostedBy: List<String> = emptyList(), // Кто сделал репост
    val timestamp: Timestamp? = null,
    val isAuthorBanned: Boolean = false, // Бан автора
    val authorNameColor: String? = null, // Цвет ника (VIP)
    val communityId: String? = null, // Если null — пост в общей ленте
    val poll: PollData? = null, // Опрос
    val authorStatus: String? = null // Статус автора
)

data class Community(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val creatorId: String = "",       // Кто создал
    val bannerColor: String = "#FF4CAF50", // HEX-цвет баннера по умолчанию (Зеленый)
    val avatarUrl: String = "",       // Ссылка на аватар сообщества
    val members: List<String> = emptyList() // Список ID участников
)

data class User(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,      // Ссылка на баннер
    val bannerColor: String = "#808080", // Цвет баннера
    val status: String = "",            // Текстовый статус
    val bio: String = "",               // Описание профиля
    val joinedCommunityId: String? = null, // ID сообщества, в котором он состоит
    val joinedCommunityAvatar: String? = null, // Ссылка на аватар этого сообщества
    val isOnline: Boolean = false,       // Статус в сети
    val notificationSetting: String = "all" // all, following, none
)

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(), // Юзернеймы участников
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: MediaType = MediaType.NONE,
    val timestamp: Long = 0L,
    val forwardedPostId: String? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val isRead: Boolean = false
)

data class Comment(
    val author: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val postId: String = "",
    val id: String = "",
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val replyToCommentId: String? = null,
    val replyToUsername: String? = null,
    @get:com.google.firebase.firestore.Exclude
    val isLikedByMe: Boolean = false
)

data class PostAnalytics(
    val postId: String = "",
    val titleOrText: String = "",
    val views: Int = 0,
    val likes: Int = 0,
    val reposts: Int = 0,
    val commentsCount: Int = 0,
    val timestamp: Long = 0L // Для фильтрации по времени
)

data class CommentAnalytics(
    val commentId: String = "",
    val postId: String = "",
    val authorName: String = "",
    val commentText: String = "",
    val likes: Int = 0
)

@Composable
fun GifPlayer(
    gifUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Настраиваем Coil с поддержкой декодирования GIF
    val imageRequest = ImageRequest.Builder(context)
        .data(gifUrl)
        .decoderFactory(
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoderDecoder.Factory()
            } else {
                GifDecoder.Factory()
            }
        )
        .crossfade(true)
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = "GIF Анимация",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun PresenceIndicator(
    username: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 12.dp
) {
    var isOnline by remember { mutableStateOf(false) }
    val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
    val cleanUsername = username.replace("@", "").trim()

    LaunchedEffect(cleanUsername) {
        if (cleanUsername.isNotEmpty()) {
            val statusRef = rtdb.getReference("status/$cleanUsername/state")
            statusRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    isOnline = snapshot.getValue(String::class.java) == "online"
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }

    if (isOnline) {
        Box(
            modifier = modifier
                .size(size)
                .background(Color(0xFF4CAF50), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
    }
}

@Composable
fun PresenceText(
    username: String,
    modifier: Modifier = Modifier
) {
    var status by remember { mutableStateOf("offline") }
    var lastChanged by remember { mutableLongStateOf(0L) }
    val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
    val cleanUsername = username.replace("@", "").trim()

    LaunchedEffect(cleanUsername) {
        if (cleanUsername.isNotEmpty()) {
            val statusRef = rtdb.getReference("status/$cleanUsername")
            statusRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    status = snapshot.child("state").getValue(String::class.java) ?: "offline"
                    lastChanged = snapshot.child("last_changed").getValue(Long::class.java) ?: 0L
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }

    Text(
        text = if (status == "online") "В сети" else formatLastSeen(lastChanged),
        fontSize = 12.sp,
        color = if (status == "online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = modifier
    )
}

fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0) return "был(а) недавно"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "был(а) только что"
        minutes < 60 -> "был(а) $minutes мин. назад"
        hours < 24 -> "был(а) $hours час. назад"
        days < 7 -> "был(а) $days дн. назад"
        else -> {
            val sdf = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            "был(а) " + sdf.format(Date(timestamp))
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    isFullScreenMode: Boolean = false // Флаг, запущен ли плеер уже на весь экран
) {
    val context = LocalContext.current
    var isFullScreenOpen by remember { mutableStateOf(false) }

    // Инициализируем ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false // Видео не стартует само, пока юзер не нажмет Play
        }
    }

    // Освобождаем ресурсы плеера, когда компонент исчезает с экрана
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        // Нативный плеер
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    
                    // 2. Делаем так, чтобы управление скрывалось автоматически через 1.5 секунды
                    setControllerShowTimeoutMs(1500) 
                    
                    // 3. Отключаем лишние элементы, оставляя только полосу прокрутки
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)

                    // В полноэкранном режиме вписываем видео полностью (FIT), в обычном — по ширине
                    resizeMode = if (isFullScreenMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // КНОПКА НАД ПЛЕЕРОМ (Показывается только если мы НЕ в режиме Fullscreen)
        if (!isFullScreenMode) {
            IconButton(
                onClick = { isFullScreenOpen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    .size(36.dp)
                    .bounceClick()
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "На весь экран",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // ПОЛНОЕКРАННЫЙ ДИАЛОГ (Разворачивает видео на 100% экрана смартфона)
    if (isFullScreenOpen) {
        Dialog(
            onDismissRequest = { isFullScreenOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false // Разрешаем диалогу занять абсолютно весь экран
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // Черный фон
            ) {
                // Вызываем этот же плеер, но передаем флаг полноэкранного режима
                VideoPlayer(
                    videoUrl = videoUrl,
                    modifier = Modifier.fillMaxSize(),
                    isFullScreenMode = true
                )

                // Кнопка "Закрыть полноэкранный режим" сверху слева
                IconButton(
                    onClick = { isFullScreenOpen = false },
                    modifier = Modifier
                        .padding(top = 40.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

@Composable
fun ShimmerPostItem() {
    val brush = shimmerBrush()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(brush))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(60.dp).height(10.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)).background(brush))
        }
    }
}

@Composable
fun ShimmerUserItem() {
    val brush = shimmerBrush()
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(brush))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(brush))
        }
    }
}

// ==========================================
// 2. ГЛАВНАЯ ЛЕНТА ПОСТОВ
// ==========================================
@Composable
fun MainMediaScreen(onUserClick: (String) -> Unit, onHashtagClick: (String) -> Unit = {}, header: @Composable (() -> Unit)? = null, currentTab: String = "Для вас", viewModel: FeedViewModel = viewModel()) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    var showOnboarding by remember { mutableStateOf(sharedPrefs.getBoolean("is_first_launch", true)) }

    val postsList by viewModel.postsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var followingList by remember { mutableStateOf(setOf<String>()) }

    val db = FirebaseFirestore.getInstance()

    // 2. Загрузка списка подписок
    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            db.collection("follows")
                .whereEqualTo("follower", myUsername)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        followingList = snapshot.documents.mapNotNull { it.getString("following") }.toSet()
                    }
                }
        }
    }

    val filteredPosts = when (currentTab) {
        "Медиа" -> postsList.filter { (it.isMedia || !it.imageUrl.isNullOrEmpty()) && !it.isAuthorBanned && it.communityId == null }
        "Вы читаете" -> postsList.filter { followingList.contains(it.handle.replace("@", "")) && !it.isAuthorBanned && it.communityId == null }
        "Популярное" -> postsList.filter { !it.isAuthorBanned && it.communityId == null }.sortedByDescending { it.likes }
        else -> postsList.filter { !it.isAuthorBanned && it.communityId == null }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(5) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ShimmerPostItem()
                    }
                }
            }
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center).padding(16.dp))
        } else if (filteredPosts.isEmpty() && header == null) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📭", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Здесь пока пусто. Напишите первый пост!", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (header != null) {
                    header()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PostItem(post = post, onUserClick = onUserClick, onHashtagClick = onHashtagClick)
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // ОНБОРДИНГ (Coach Marks)
        if (showOnboarding) {
            OnboardingOverlay(onDismiss = {
                showOnboarding = false
                sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
            })
        }
    }
}

// ==========================================
// 3. ЭЛЕМЕНТ ПОСТА В ЛЕНТЕ
// ==========================================
@Composable
fun PostItem(post: Post, onUserClick: (String) -> Unit, onHashtagClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = prefs.getString("username", "") ?: ""
    val fontSizeMultiplier = LocalFontSize.current

    val isLiked = post.likedBy.contains(myUsername)
    val isMyPost = post.handle.replace("@", "") == myUsername
    var isExpanded by remember { mutableStateOf(false) }
    var isCommentsExpanded by rememberSaveable(post.id) { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var isImageExpanded by remember { mutableStateOf(false) }
    var isContextMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(post.text) }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать публикацию", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = { Text("Введите новый текст...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("zhirpem_posts").document(post.id).update("text", editedText)
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Отмена")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить пост?") },
            text = { Text("Это действие нельзя будет отменить. Вы уверены?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.collection("zhirpem_posts").document(post.id).delete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Да, удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Нет")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300))
            .combinedClickable(
                onClick = { isExpanded = !isExpanded },
                onLongClick = { if (isMyPost) isContextMenuExpanded = true }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                // ШАПКА ПОСТА
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .clickable { onUserClick(post.handle.replace("@", "")) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!post.authorAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = post.authorAvatarUrl,
                                    contentDescription = "Аватар автора",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = post.author.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        PresenceIndicator(
                            username = post.handle,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.clickable { onUserClick(post.handle.replace("@", "")) }.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val nameColor = if (!post.authorNameColor.isNullOrEmpty()) {
                                        try { Color(android.graphics.Color.parseColor(post.authorNameColor)) } 
                                        catch (e: Exception) { MaterialTheme.colorScheme.onSurface }
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                    
                                    Text(text = post.author, color = nameColor, fontWeight = FontWeight.Bold, fontSize = 16.sp * fontSizeMultiplier, maxLines = 1)
                                    VerifiedBadge(isBlue = post.blueBadge, isYellow = post.yellowBadge)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = post.handle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp * fontSizeMultiplier, maxLines = 1)
                                }
                                if (!post.authorStatus.isNullOrEmpty()) {
                                    Text(
                                        text = post.authorStatus!!,
                                        fontSize = 12.sp * fontSizeMultiplier,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!isMyPost) {
                                val targetUsername = post.handle.replace("@", "")
                                var isFollowing by remember { mutableStateOf(false) }

                                LaunchedEffect(myUsername, targetUsername) {
                                    if (myUsername.isNotEmpty()) {
                                        db.collection("follows")
                                            .whereEqualTo("follower", myUsername)
                                            .whereEqualTo("following", targetUsername)
                                            .addSnapshotListener { snapshot, _ ->
                                                isFollowing = snapshot != null && !snapshot.isEmpty
                                            }
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        val followsRef = db.collection("follows")
                                        if (isFollowing) {
                                            followsRef.whereEqualTo("follower", myUsername)
                                                      .whereEqualTo("following", targetUsername)
                                                      .get().addOnSuccessListener { docs ->
                                                          for (doc in docs) doc.reference.delete()
                                                      }
                                        } else {
                                            val followData = hashMapOf(
                                                "follower" to myUsername,
                                                "following" to targetUsername,
                                                "timestamp" to FieldValue.serverTimestamp()
                                            )
                                            followsRef.add(followData)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (isFollowing) "Читаю" else "Читать",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFollowing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (post.time.isNotEmpty() || post.date.isNotEmpty()) {
                            Text(text = "${post.date} в ${post.time}", fontSize = 12.sp * fontSizeMultiplier, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }

                        val isLongText = post.text.length > 150
                        
                        val annotatedText = buildAnnotatedString {
                            val hashtagRegex = Regex("#[a-zA-Zа-яА-Я0-9_]+")
                            var lastMatchEnd = 0
                            
                            hashtagRegex.findAll(post.text).forEach { match ->
                                append(post.text.substring(lastMatchEnd, match.range.first))
                                
                                pushStringAnnotation(tag = "HASHTAG", annotation = match.value)
                                withStyle(style = SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)) {
                                    append(match.value)
                                }
                                pop()
                                lastMatchEnd = match.range.last + 1
                            }
                            append(post.text.substring(lastMatchEnd))
                        }

                        ClickableText(
                            text = annotatedText,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp * fontSizeMultiplier,
                                lineHeight = 22.sp * fontSizeMultiplier
                            ),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        onHashtagClick(annotation.item)
                                    } ?: run {
                                        isExpanded = !isExpanded
                                    }
                            }
                        )

                        if (isLongText && !isExpanded) {
                            Text(
                                text = "Читать далее...",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { isExpanded = true }
                            )
                        }

                        // ОБРАБОТКА МЕДИА-КОНТЕНТА
                        val finalMediaUrl = if (post.mediaUrl.isNotEmpty()) post.mediaUrl else post.imageUrl

                        if (!finalMediaUrl.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val commonModifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))

                            when (post.mediaType) {
                                MediaType.VIDEO -> {
                                    VideoPlayer(videoUrl = finalMediaUrl, modifier = commonModifier)
                                }
                                MediaType.GIF -> {
                                    GifPlayer(gifUrl = finalMediaUrl, modifier = commonModifier.height(320.dp))
                                }
                                MediaType.IMAGE -> {
                                    AsyncImage(
                                        model = finalMediaUrl,
                                        contentDescription = "Фото поста",
                                        modifier = commonModifier.clickable { isImageExpanded = true },
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                else -> {
                                    AsyncImage(
                                        model = finalMediaUrl,
                                        contentDescription = "Фото поста",
                                        modifier = commonModifier.clickable { isImageExpanded = true },
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }
                        }

                        // ОБРАБОТКА ОПРОСА
                        if (post.poll != null && isExpanded) {
                            PollView(
                                poll = post.poll,
                                currentUserId = myUsername,
                                onVote = { index ->
                                    if (myUsername.isNotEmpty() && post.id.isNotEmpty()) {
                                        val postRef = db.collection("zhirpem_posts").document(post.id)
                                        db.runTransaction { transaction ->
                                            val snapshot = transaction.get(postRef)
                                            val pollMap = snapshot.get("poll") as? Map<String, Any>
                                            if (pollMap != null) {
                                                val votes = (pollMap["votes"] as? Map<String, List<String>>)?.toMutableMap() ?: mutableMapOf()
                                                val optionKey = index.toString()
                                                val optionVotes = votes[optionKey]?.toMutableList() ?: mutableListOf()
                                                
                                                if (!optionVotes.contains(myUsername)) {
                                                    optionVotes.add(myUsername)
                                                    votes[optionKey] = optionVotes
                                                    transaction.update(postRef, "poll.votes", votes)
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .bounceClick { isCommentsExpanded = !isCommentsExpanded }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val commentText = if (post.commentsCount > 0) "Ответить(${post.commentsCount})" else "Ответить"
                                val commentColor = if (post.commentsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                Text(commentText, color = commentColor, fontSize = 14.sp * fontSizeMultiplier, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .bounceClick {
                                        if (post.id.isNotEmpty() && myUsername.isNotEmpty()) {
                                            val postRef = db.collection("zhirpem_posts").document(post.id)
                                            db.runTransaction { transaction ->
                                                val snapshot = transaction.get(postRef)
                                                val currentLikes = snapshot.getLong("likes") ?: 0L
                                                if (post.likedBy.contains(myUsername)) {
                                                    transaction.update(postRef, "likedBy", FieldValue.arrayRemove(myUsername))
                                                    transaction.update(postRef, "likes", currentLikes - 1)
                                                } else {
                                                    transaction.update(postRef, "likedBy", FieldValue.arrayUnion(myUsername))
                                                    transaction.update(postRef, "likes", currentLikes + 1)
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Лайк",
                                    tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = post.likes.toString(),
                                    color = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 14.sp * fontSizeMultiplier,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val isBookmarked = post.bookmarkedBy.contains(myUsername)
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .bounceClick {
                                        if (post.id.isNotEmpty() && myUsername.isNotEmpty()) {
                                            val postRef = db.collection("zhirpem_posts").document(post.id)
                                            if (isBookmarked) {
                                                postRef.update("bookmarkedBy", FieldValue.arrayRemove(myUsername))
                                            } else {
                                                postRef.update("bookmarkedBy", FieldValue.arrayUnion(myUsername))
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Закладка",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("В закладки", color = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // РЕПОСТ
                                val isReposted = post.repostedBy.contains(myUsername)
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .bounceClick {
                                            if (post.id.isNotEmpty() && myUsername.isNotEmpty()) {
                                                val postRef = db.collection("zhirpem_posts").document(post.id)
                                                if (isReposted) {
                                                    postRef.update("repostedBy", FieldValue.arrayRemove(myUsername))
                                                } else {
                                                    postRef.update("repostedBy", FieldValue.arrayUnion(myUsername))
                                                }
                                            }
                                        }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Репост",
                                        tint = if (isReposted) Color.Green else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Репост", color = if (isReposted) Color.Green else Color.Gray, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // ПОДЕЛИТЬСЯ
                                if (showShareDialog) {
                                    SharePostDialog(
                                        postId = post.id,
                                        postText = post.text,
                                        onDismiss = { showShareDialog = false }
                                    )
                                }

                                IconButton(
                                    onClick = { showShareDialog = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .bounceClick()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Поделиться",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (isImageExpanded && !post.imageUrl.isNullOrEmpty()) {
                    Dialog(
                        onDismissRequest = { isImageExpanded = false },
                        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ZoomableImage(
                                imageUrl = post.imageUrl,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { isImageExpanded = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 40.dp, end = 20.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = Color.White)
                            }
                        }
                    }
                }

                val animationsEnabled = LocalAnimationsEnabled.current
                AnimatedVisibility(
                    visible = isCommentsExpanded,
                    enter = if (animationsEnabled) expandVertically() + fadeIn() else EnterTransition.None,
                    exit = if (animationsEnabled) shrinkVertically() + fadeOut() else ExitTransition.None
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        CommentList(
                            postId = post.id,
                            onUserClick = onUserClick,
                            onReply = { replyingTo = it }
                        )
                        CommentInput(
                            postId = post.id,
                            replyTo = replyingTo,
                            onCancelReply = { replyingTo = null }
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = isContextMenuExpanded,
                onDismissRequest = { isContextMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Редактировать") },
                    onClick = {
                        isContextMenuExpanded = false
                        editedText = post.text
                        showEditDialog = true
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        isContextMenuExpanded = false
                        showDeleteDialog = true
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}


// ==========================================
// 4. СПИСОК КОММЕНТАРИЕВ К ПОСТУ
// ==========================================
@Composable
fun CommentList(
    postId: String,
    onUserClick: (String) -> Unit,
    onReply: (Comment) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = prefs.getString("username", "") ?: ""

    var comments by remember { mutableStateOf(listOf<Comment>()) }
    val db = FirebaseFirestore.getInstance()
    val fontSizeMultiplier = LocalFontSize.current

    LaunchedEffect(postId, myUsername) {
        if (postId.isNotEmpty()) {
            db.collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        comments = snapshot.documents.mapNotNull { doc ->
                            val comment = doc.toObject(Comment::class.java)?.copy(id = doc.id)
                            comment?.copy(isLikedByMe = comment.likedBy.contains(myUsername))
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier.heightIn(max = 250.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (comments.isEmpty()) {
            Text("Пока нет ответов. Станьте первым!", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        } else {
            comments.forEach { comment ->
                val isReply = !comment.replyToCommentId.isNullOrEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (isReply) 24.dp else 0.dp)
                        .combinedClickable(
                            onClick = { /* Можно открыть профиль или ветку */ },
                            onLongClick = { onReply(comment) }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Box(
                            modifier = Modifier.size(if (isReply) 24.dp else 30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable { onUserClick(comment.authorUsername.replace("@", "")) },
                            contentAlignment = Alignment.Center
                        ) { Text(comment.author.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = if (isReply) 10.sp else 12.sp, fontWeight = FontWeight.Bold) }
                        
                        PresenceIndicator(
                            username = comment.authorUsername,
                            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 1.dp, y = 1.dp),
                            size = if (isReply) 8.dp else 10.dp
                        )
                    }

                    Column(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(10.dp)) {
                        Row(modifier = Modifier.clickable { onUserClick(comment.authorUsername.replace("@", "")) }) {
                            Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 13.sp * fontSizeMultiplier, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(comment.authorUsername, fontSize = 12.sp * fontSizeMultiplier, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        
                        if (!comment.replyToUsername.isNullOrEmpty()) {
                            Text(
                                text = "в ответ ${comment.replyToUsername}",
                                fontSize = 11.sp * fontSizeMultiplier,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(comment.text, fontSize = 14.sp * fontSizeMultiplier, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Лайки комментария
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                if (comment.id.isNotEmpty() && myUsername.isNotEmpty()) {
                                    val commentRef = db.collection("comments").document(comment.id)
                                    if (comment.isLikedByMe) {
                                        commentRef.update(
                                            "likedBy", FieldValue.arrayRemove(myUsername),
                                            "likesCount", FieldValue.increment(-1)
                                        )
                                    } else {
                                        commentRef.update(
                                            "likedBy", FieldValue.arrayUnion(myUsername),
                                            "likesCount", FieldValue.increment(1)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (comment.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Лайк комментария",
                                tint = if (comment.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (comment.likesCount > 0) {
                            Text(
                                text = comment.likesCount.toString(),
                                fontSize = 11.sp,
                                color = if (comment.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ==========================================
// 5. ПОЛЕ ВВОДА КОММЕНТАРИЯ
// ==========================================
@Composable
fun CommentInput(
    postId: String,
    replyTo: Comment? = null,
    onCancelReply: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    var text by remember { mutableStateOf("") }

    // Автоматическая подстановка тега при выборе ответа
    LaunchedEffect(replyTo) {
        if (replyTo != null) {
            val tag = "${replyTo.authorUsername} "
            if (!text.contains(tag)) {
                text = tag + text.replace(Regex("^@[a-zA-Z0-9_]+\\s"), "")
            }
        }
    }

    fun sendComment() {
        if (text.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            val newComment = hashMapOf(
                "postId" to postId,
                "author" to (prefs.getString("name", "Аноним") ?: "Аноним"),
                "authorUsername" to "@${prefs.getString("username", "user")}",
                "text" to text.trim(),
                "timestamp" to FieldValue.serverTimestamp(),
                "likesCount" to 0,
                "likedBy" to emptyList<String>(),
                "replyToCommentId" to replyTo?.id,
                "replyToUsername" to replyTo?.authorUsername
            )
            
            db.collection("comments").add(newComment).addOnSuccessListener { docRef ->
                db.collection("zhirpem_posts").document(postId)
                    .update("commentsCount", FieldValue.increment(1))
                
                // Триггер уведомления
                val myName = prefs.getString("name", "Аноним") ?: "Аноним"
                val myUsernameReal = prefs.getString("username", "user") ?: "user"
                val myAvatar = prefs.getString("avatarUrl", "") ?: ""
                
                if (replyTo != null) {
                    // Уведомление автору комментария (ответ)
                    sendNotification(
                        db = db,
                        senderId = myUsernameReal,
                        senderName = myName,
                        senderAvatar = myAvatar,
                        receiverId = replyTo.authorUsername.replace("@", ""),
                        type = "COMMENT",
                        text = text.trim(),
                        postId = postId,
                        targetText = replyTo.text
                    )
                } else {
                    // Уведомление автору поста
                    db.collection("zhirpem_posts").document(postId).get().addOnSuccessListener { postDoc ->
                        val receiverId = postDoc.getString("senderId") ?: ""
                        if (receiverId.isNotEmpty()) {
                            sendNotification(
                                db = db,
                                senderId = myUsernameReal,
                                senderName = myName,
                                senderAvatar = myAvatar,
                                receiverId = receiverId,
                                type = "COMMENT",
                                text = text.trim(),
                                postId = postId,
                                targetText = postDoc.getString("text") ?: ""
                            )
                        }
                    }
                }
            }

            text = ""
            onCancelReply()
            focusManager.clearFocus()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (replyTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ответ пользователю ${replyTo.author}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onCancelReply, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Отмена", modifier = Modifier.size(14.dp))
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ваш ответ...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendComment() })
            )

            Button(
                onClick = { sendComment() },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(46.dp).bounceClick(),
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Отправить", modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ==========================================
// 6. СООБЩЕСТВА (ЭКОСИСТЕМА ГРУПП)
// ==========================================

@Composable
fun CommunityAvatar(url: String?, size: androidx.compose.ui.unit.Dp) {
    if (!url.isNullOrEmpty()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = Modifier.size(size).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Text("👥", fontSize = (size.value * 0.5).sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesScreen(
    onBack: () -> Unit,
    onCommunityClick: (Community) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("username", "") ?: ""
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinSection by remember { mutableStateOf(true) }
    
    var allCommunities by remember { mutableStateOf(listOf<Community>()) } 
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("communities").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                allCommunities = snapshot.documents.mapNotNull { it.toObject(Community::class.java)?.copy(id = it.id) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщества", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // ДВЕ БОЛЬШИЕ КНОПКИ СВЕРХУ
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showJoinSection = true },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (showJoinSection) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Вступить", fontSize = 14.sp)
                }

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Создать", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // МОИ СООБЩЕСТВА
            val myCommunities = allCommunities.filter { it.members.contains(currentUserId) || it.creatorId == currentUserId }
            if (myCommunities.isNotEmpty()) {
                Text("Ваши сообщества:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(0.4f)) {
                    items(myCommunities) { community ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onCommunityClick(community) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CommunityAvatar(url = community.avatarUrl, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(community.name, fontWeight = FontWeight.Bold)
                                    Text(community.description, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = 180f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // РАЗДЕЛ: ДОСТУПНЫЕ СООБЩЕСТВА ДЛЯ ВСТУПЛЕНИЯ
            if (showJoinSection) {
                Text("Рекомендуемые:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(0.6f)) {
                    val available = allCommunities.filter { !it.members.contains(currentUserId) && it.creatorId != currentUserId }
                    
                    items(available) { community ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onCommunityClick(community) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CommunityAvatar(url = community.avatarUrl, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(community.name, fontWeight = FontWeight.Bold)
                                    Text(community.description, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { 
                                        db.collection("communities").document(community.id)
                                            .update("members", FieldValue.arrayUnion(currentUserId))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Вступить", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ДИАЛОГ СОЗДАНИЯ СООБЩЕСТВА
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Новое сообщество") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание (Тематика)") }, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            val newComm = hashMapOf(
                                "name" to name,
                                "description" to desc,
                                "creatorId" to currentUserId,
                                "members" to listOf(currentUserId),
                                "bannerColor" to "#FF4CAF50",
                                "avatarUrl" to ""
                            )
                            db.collection("communities").add(newComm)
                            showCreateDialog = false
                        }
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailsScreen(
    communityId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("username", "") ?: ""
    val db = FirebaseFirestore.getInstance()

    var community by remember { mutableStateOf<Community?>(null) }
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(communityId) {
        db.collection("communities").document(communityId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                community = snapshot.toObject(Community::class.java)?.copy(id = snapshot.id)
            }
        }
        db.collection("zhirpem_posts")
            .whereEqualTo("communityId", communityId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
                        .sortedByDescending { it.timestamp }
                }
                isLoading = false
            }
    }

    if (community == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val comm = community!!
    val isAuthor = comm.creatorId == currentUserId
    val isMember = comm.members.contains(currentUserId) || isAuthor

    var showEditDialog by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(comm.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isAuthor) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Настройки")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 1. БАННЕР
            val parsedColor = try { Color(android.graphics.Color.parseColor(comm.bannerColor)) } catch (e: Exception) { MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(parsedColor)
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 80.dp)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp)
                ) {
                    CommunityAvatar(url = comm.avatarUrl, size = 72.dp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 2. ИНФОРМАЦИЯ
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = comm.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = comm.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))
                Text(text = "👥 ${comm.members.size} участников", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // 3. ПУБЛИКАЦИЯ ПОСТА
                if (isMember) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        TextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { Text("Напишите что-нибудь...", fontSize = 14.sp) },
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (postText.isNotBlank()) {
                                    val newPost = hashMapOf(
                                        "author" to (sharedPrefs.getString("name", "Аноним") ?: "Аноним"),
                                        "handle" to "@$currentUserId",
                                        "text" to postText.trim(),
                                        "date" to SimpleDateFormat("d MMM", Locale("ru")).format(Date()),
                                        "time" to SimpleDateFormat("HH:mm", Locale("ru")).format(Date()),
                                        "communityId" to comm.id,
                                        "authorStatus" to sharedPrefs.getString("status", ""),
                                        "timestamp" to FieldValue.serverTimestamp()
                                    )
                                    db.collection("zhirpem_posts").add(newPost)
                                    postText = ""
                                }
                            },
                            enabled = postText.isNotBlank(),
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.graphicsLayer(rotationZ = 180f))
                        }
                    }
                }

                // 4. ЛЕНТА ПОСТОВ СООБЩЕСТВА
                if (isLoading) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) { ShimmerPostItem() }
                    }
                } else if (posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Здесь пока нет записей", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                        items(posts, key = { it.id }) { post ->
                            PostItem(post = post, onUserClick = onUserClick, onHashtagClick = onHashtagClick)
                        }
                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var newDesc by remember { mutableStateOf(comm.description) }
        var newAvatar by remember { mutableStateOf(comm.avatarUrl) }
        var newBanner by remember { mutableStateOf(comm.bannerColor) }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Настройки сообщества") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Описание") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = newAvatar, onValueChange = { newAvatar = it }, label = { Text("URL Аватарки") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = newBanner, onValueChange = { newBanner = it }, label = { Text("Цвет баннера (HEX)") }, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("communities").document(comm.id).update(
                        mapOf(
                            "description" to newDesc,
                            "avatarUrl" to newAvatar,
                            "bannerColor" to newBanner
                        )
                    )
                    showEditDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    myPostsAnalytics: List<PostAnalytics>,
    allJirpemPosts: List<PostAnalytics>,
    popularCommunities: List<Community>,
    bestComments: List<CommentAnalytics>,
    onBack: () -> Unit
) {
    // Настраиваемый отрезок времени: 0 - День, 1 - Неделя, 2 - Месяц
    var selectedTimePeriod by remember { mutableIntStateOf(1) }
    val timePeriods = listOf("День", "Неделя", "Месяц")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ЗАГОЛОВОК И НАСТРОЙКА ВРЕМЕНИ
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Период:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    
                    // Переключатель отрезка времени
                    SingleChoiceSegmentedButtonRow {
                        timePeriods.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = timePeriods.size),
                                onClick = { selectedTimePeriod = index },
                                selected = selectedTimePeriod == index
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 1. ПАНЕЛЬ УПРАВЛЕНИЯ ТВОИМИ ПОСТАМИ (Общие цифры)
            item {
                val totalViews = myPostsAnalytics.sumOf { it.views }
                val totalLikes = myPostsAnalytics.sumOf { it.likes }
                val totalReposts = myPostsAnalytics.sumOf { it.reposts }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Панель управления постами", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetricBox(label = "Просмотры", value = totalViews.toString(), color = Color(0xFF4CAF50))
                            StatMetricBox(label = "Лайки", value = totalLikes.toString(), color = Color(0xFFE91E63))
                            StatMetricBox(label = "Репосты", value = totalReposts.toString(), color = Color(0xFF2196F3))
                        }
                    }
                }
            }

            // 2. ТВОЙ ЛУЧШИЙ ПОСТ (За выбранный период)
            item {
                // В реальной БД здесь будет фильтрация по timestamp в зависимости от selectedTimePeriod
                val bestMyPost = myPostsAnalytics.maxByOrNull { it.likes + it.views }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Твой лучший пост (${timePeriods[selectedTimePeriod].lowercase()})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (bestMyPost != null) {
                            Text(bestMyPost.titleOrText, maxLines = 2, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🔥 ${bestMyPost.views} просмотров", fontSize = 12.sp, color = Color.Gray)
                                Text("❤️ ${bestMyPost.likes} лайков", fontSize = 12.sp, color = Color.Gray)
                            }
                        } else {
                            Text("Нет данных за этот период", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 3. ЛУЧШИЙ ПОСТ В ЖИРПЕМ (Глобальный топ)
            item {
                val topJirpemPost = allJirpemPosts.maxByOrNull { it.views }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Лучший пост в Жирпем", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (topJirpemPost != null) {
                            Text(topJirpemPost.titleOrText, maxLines = 2, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Охвачено пользователей: ${topJirpemPost.views}", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 4. ПОПУЛЯРНЫЕ СООБЩЕСТВА (Топ-3 по участникам)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Популярные сообщества", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Сортируем по количеству участников и берем первые 3
                        val topCommunities = popularCommunities.sortedByDescending { it.members.size }.take(3)
                        
                        topCommunities.forEachIndexed { index, community ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${index + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                                
                                CommunityAvatar(url = community.avatarUrl, size = 32.dp)
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(community.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Участников: ${community.members.size}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // 5. ЛУЧШИЕ КОММЕНТАРИИ
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Лучшие комментарии", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val topComments = bestComments.sortedByDescending { it.likes }.take(2)
                        
                        topComments.forEach { comment ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("❤️ ${comment.likes}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text(comment.commentText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

// ==========================================
// 7. ЭКРАН ПРОФИЛЯ ПОЛЬЗОВАТЕЛЯ
// ==========================================
@Composable
fun UserProfileScreen(
    username: String, 
    onBack: () -> Unit, 
    onNavigateToChat: (String) -> Unit = {},
    onHashtagClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = prefs.getString("username", "") ?: ""

    var name by remember { mutableStateOf("Загрузка...") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var blueBadge by remember { mutableStateOf(false) }
    var yellowBadge by remember { mutableStateOf(false) }
    var bannerColor by remember { mutableStateOf("#808080") }
    var bio by remember { mutableStateOf("") }
    var joinedCommunityId by remember { mutableStateOf<String?>(null) }
    var joinedCommunityAvatar by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf<String?>(null) }
    var isEditingBio by remember { mutableStateOf(false) }
    var isEditingStatus by remember { mutableStateOf(false) }
    var isMoreMenuExpanded by remember { mutableStateOf(false) }
    
    var posts by remember { mutableStateOf(listOf<Post>()) }
    var repostedPosts by remember { mutableStateOf(listOf<Post>()) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val fontSizeMultiplier = LocalFontSize.current

    var isFollowing by remember { mutableStateOf(false) }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            uploadImageToCloudinary(
                context = context,
                imageUri = uri,
                cloudName = "dcwp4nm3e",
                uploadPreset = "ProfilePIC",
                onSuccess = { imageUrl ->
                    db.collection("users")
                        .document(username)
                        .update("bannerUrl", imageUrl)
                        .addOnSuccessListener {
                            isUploading = false
                            bannerUrl = imageUrl
                        }
                },
                onError = {
                    isUploading = false
                }
            )
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            uploadImageToCloudinary(
                context = context,
                imageUri = uri,
                cloudName = "dcwp4nm3e",
                uploadPreset = "ProfilePIC",
                onSuccess = { imageUrl ->
                    db.collection("users")
                        .document(username)
                        .update("avatarUrl", imageUrl)
                        .addOnSuccessListener {
                            isUploading = false
                            avatarUrl = imageUrl
                        }
                },
                onError = {
                    isUploading = false
                }
            )
        }
    }

    LaunchedEffect(username) {
        db.collection("users").document(username).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                name = doc.getString("name") ?: "Без имени"
                avatarUrl = doc.getString("avatarUrl")
                blueBadge = doc.getBoolean("blueBadge") ?: false
                yellowBadge = doc.getBoolean("yellowBadge") ?: false
                bannerColor = doc.getString("bannerColor") ?: "#808080"
                bio = doc.getString("bio") ?: ""
                joinedCommunityId = doc.getString("joinedCommunityId")
                joinedCommunityAvatar = doc.getString("joinedCommunityAvatar")
                status = doc.getString("status") ?: ""
                bannerUrl = doc.getString("bannerUrl")
            }
        }
        db.collection("zhirpem_posts")
            .whereEqualTo("handle", "@$username")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val unsortedPosts = snap.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
                    posts = unsortedPosts.sortedByDescending { it.timestamp }
                }
            }

        db.collection("zhirpem_posts")
            .whereArrayContains("repostedBy", username)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val unsortedPosts = snap.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
                    repostedPosts = unsortedPosts.sortedByDescending { it.timestamp }
                }
                isLoading = false
            }

        if (myUsername.isNotEmpty() && myUsername != username) {
            db.collection("follows")
                .whereEqualTo("follower", myUsername)
                .whereEqualTo("following", username)
                .addSnapshotListener { snapshot, _ ->
                    isFollowing = snapshot != null && !snapshot.isEmpty
                }
        }
    }

    // Обновление FCM-токена при входе в свой профиль
    LaunchedEffect(username, myUsername) {
        if (username == myUsername && myUsername.isNotEmpty()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    db.collection("users").document(myUsername).update("fcmToken", token)
                }
            }
        }
    }

    if (isEditingStatus) {
        var tempStatus by remember { mutableStateOf(status) }
        AlertDialog(
            onDismissRequest = { isEditingStatus = false },
            title = { Text("Обновить статус") },
            text = {
                OutlinedTextField(
                    value = tempStatus,
                    onValueChange = { tempStatus = it },
                    placeholder = { Text("Ваш статус с эмодзи...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 1
                )
            },
            confirmButton = {
                Button(onClick = {
                    db.collection("users").document(username).update("status", tempStatus)
                    if (username == myUsername) {
                        prefs.edit().putString("status", tempStatus).apply()
                    }
                    isEditingStatus = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingStatus = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 12.dp, bottom = 12.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("${posts.size} записей", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))

                }

                if (myUsername.isNotEmpty() && myUsername != username) {
                    Box {
                        IconButton(onClick = { isMoreMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = isMoreMenuExpanded,
                            onDismissRequest = { isMoreMenuExpanded = false }
                        ) {
                            if (isFollowing) {
                                DropdownMenuItem(
                                    text = { Text("Перестать читать") },
                                    onClick = {
                                        isMoreMenuExpanded = false
                                        val followsRef = db.collection("follows")
                                        followsRef.whereEqualTo("follower", myUsername)
                                                  .whereEqualTo("following", username)
                                                  .get().addOnSuccessListener { docs ->
                                                      for (doc in docs) doc.reference.delete()
                                                  }
                                    },
                                    leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    colors = MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }

                if (myUsername == username) {
                    var showColorPicker by remember { mutableStateOf(false) }
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Цвет баннера", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (showColorPicker) {
                        var tempColor by remember { mutableStateOf(bannerColor) }
                        Dialog(onDismissRequest = { showColorPicker = false }) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Настройка баннера", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    ColorPicker(
                                        initialColor = bannerColor,
                                        onColorChanged = { tempColor = it }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { showColorPicker = false }) {
                                            Text("Отмена")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                db.collection("users").document(username).update("bannerColor", tempColor)
                                                showColorPicker = false
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Применить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues).verticalScroll(rememberScrollState())) {
            val parsedColor = try { Color(android.graphics.Color.parseColor(bannerColor)) } catch (e: Exception) { MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) }
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(parsedColor)) {
                if (!bannerUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = "Баннер",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (myUsername == username) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!bannerUrl.isNullOrEmpty()) {
                            IconButton(
                                onClick = {
                                    db.collection("users").document(username).update("bannerUrl", null)
                                    bannerUrl = null
                                },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить баннер", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        IconButton(
                            onClick = { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Загрузить баннер", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(100.dp)
                        .align(Alignment.BottomStart)
                        .offset(y = 50.dp)
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Аватар",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Точка "В сети"
                    PresenceIndicator(
                        username = username,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp),
                        size = 18.dp
                    )

                    // Бейдж сообщества
                    if (joinedCommunityId != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(2.dp)
                        ) {
                            CommunityAvatar(url = joinedCommunityAvatar, size = 28.dp)
                        }
                    }

                    if (myUsername == username) {
                        IconButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Изменить", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontSize = 28.sp * fontSizeMultiplier, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(isBlue = blueBadge, isYellow = yellowBadge)
                        }
                        Text("@$username", fontSize = 16.sp * fontSizeMultiplier, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        
                        if (status.isNotEmpty() || myUsername == username) {
                            Text(
                                text = if (status.isEmpty()) "Установить статус..." else status,
                                fontSize = 14.sp * fontSizeMultiplier,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable(enabled = myUsername == username) { isEditingStatus = true }
                            )
                        }
                    }

                    if (myUsername.isNotEmpty() && myUsername != username) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    openOrCreateChat(context, myUsername, username) { chatId ->
                                        onNavigateToChat(chatId)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Написать", fontWeight = FontWeight.Bold)
                            }

                            if (!isFollowing) {
                                Button(
                                    onClick = {
                                        val followData = hashMapOf(
                                            "follower" to myUsername,
                                            "following" to username,
                                            "timestamp" to FieldValue.serverTimestamp()
                                        )
                                        db.collection("follows").add(followData)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Читать", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // РАЗДЕЛ БИО (Описание профиля)
                val animationsEnabled = LocalAnimationsEnabled.current
                Box(modifier = Modifier.then(
                    if (animationsEnabled) {
                        Modifier.animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    } else Modifier
                )) {
                    if (isEditingBio) {
                        var tempBio by remember { mutableStateOf(bio) }
                        Column {
                            OutlinedTextField(
                                value = tempBio,
                                onValueChange = { tempBio = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("О себе") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { isEditingBio = false }) { Text("Отмена") }
                                TextButton(onClick = {
                                    db.collection("users").document(username).update("bio", tempBio)
                                    isEditingBio = false
                                }) { Text("Сохранить") }
                            }
                        }
                    } else {
                        Text(
                            text = if (bio.isEmpty()) "Нажмите, чтобы добавить описание..." else bio,
                            modifier = Modifier.fillMaxWidth().clickable { if (myUsername == username) isEditingBio = true },
                            fontSize = 14.sp * fontSizeMultiplier,
                            color = if (bio.isEmpty()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Секция подписчиков/подписок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    var followingCount by remember { mutableIntStateOf(0) }
                    var followersCount by remember { mutableIntStateOf(0) }

                    LaunchedEffect(username) {
                        db.collection("follows").whereEqualTo("follower", username).addSnapshotListener { snap, _ ->
                            followingCount = snap?.size() ?: 0
                        }
                        db.collection("follows").whereEqualTo("following", username).addSnapshotListener { snap, _ ->
                            followersCount = snap?.size() ?: 0
                        }
                    }

                    Row(modifier = Modifier.clickable { /* Навигация в список подписок */ }) {
                        Text(text = followingCount.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "читает", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))

                    Row(modifier = Modifier.clickable { /* Навигация в список подписчиков */ }) {
                        Text(text = followersCount.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "читателей", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Вкладки
            val tabs = listOf("Записи", "Репосты")
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

            if (isLoading) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(3) { ShimmerPostItem() }
                }
            } else {
                val currentList = if (selectedTabIndex == 0) posts else repostedPosts
                if (currentList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(if (selectedTabIndex == 0) "У пользователя пока нет записей 🤷‍♂️" else "У пользователя пока нет репостов 🤷‍♂️", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    // Используем Column вместо LazyColumn внутри прокручиваемого Column, либо Box с фикс высотой
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        currentList.forEach { post ->
                            PostItem(post = post, onUserClick = { if (it != username) onBack(); }, onHashtagClick = onHashtagClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPicker(initialColor: String, onColorChanged: (String) -> Unit) {
    var selectedColor by remember { mutableStateOf(Color(android.graphics.Color.parseColor(initialColor))) }
    var handleOffset by remember { mutableStateOf(Offset.Zero) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .padding(10.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val radius = size.width / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchPos = change.position - center
                            
                            val angle = atan2(touchPos.y, touchPos.x)
                            val distance = touchPos.getDistance().coerceIn(0f, radius)
                            
                            val correctedX = cos(angle) * distance
                            val correctedY = sin(angle) * distance
                            
                            handleOffset = Offset(correctedX + center.x, correctedY + center.y)
                            
                            val hue = ((Math.toDegrees(angle.toDouble()) + 360f) % 360f).toFloat()
                            val saturation = distance / radius
                            
                            selectedColor = Color.hsv(hue, saturation, 1f)
                            onColorChanged(selectedColor.toHex())
                        }
                    }
            ) {
                val radius = size.width / 2f
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red
                        )
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        radius = radius
                    )
                )
                
                // Рисуем белую точку (манипулятор)
                if (handleOffset == Offset.Zero) {
                    // Инициализируем положение точки при первом запуске
                    handleOffset = Offset(center.x, center.y)
                }
                
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = handleOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = selectedColor,
                    radius = 10.dp.toPx(),
                    center = handleOffset
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Превью и HEX
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(selectedColor.toHex(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Быстрые пресеты
        val presets = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#000000", "#FFFFFF")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .clickable { 
                            val color = Color(android.graphics.Color.parseColor(hex))
                            selectedColor = color
                            onColorChanged(hex)
                            // Мы не обновляем handleOffset для пресетов для простоты, 
                            // либо можно сбросить в центр
                            handleOffset = Offset.Zero 
                        }
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                )
            }
        }
    }
}

fun Color.toHex(): String {
    return String.format("#%06X", (0xFFFFFF and this.toArgb()))
}

@Composable
fun VerifiedBadge(isBlue: Boolean, isYellow: Boolean) {
    var showDialog by remember { mutableStateOf<String?>(null) }

    if (showDialog != null) {
        AlertDialog(
            onDismissRequest = { showDialog = null },
            text = {
                Text(
                    text = showDialog!!,
                    textAlign = TextAlign.Center, // Отцентрировано
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = null }) { Text("Понятно") }
            }
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isBlue) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "Blue Check",
                tint = Color.Blue,
                modifier = Modifier.size(18.dp).clickable { 
                    showDialog = "Это синяя галочка: знак, подтверждающий что пользователь с которым вы общаетесь — подтвердил свою личность кружком в Telegram. Чтобы сделать также — обратитесь в сообщения Telegram канала Жирпем-1.\n\nhttps://t.me/+M0X7pUKnlKs0NWNi" 
                }
            )
        }
        
        if (isYellow) {
            if (isBlue) Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Yellow Check",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(18.dp).clickable { 
                    showDialog = "Это желтая галочка. Она обозначает участника команды Жирпем-1. Получить ее можно только одним способом — быть постоянным участником команды Жирпем-1." 
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(onBack: () -> Unit, onUserClick: (String) -> Unit, onHashtagClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    var bookmarkedPosts by remember { mutableStateOf(listOf<Post>()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            db.collection("zhirpem_posts")
                .whereArrayContains("bookmarkedBy", myUsername)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        bookmarkedPosts = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Post::class.java)?.copy(id = doc.id)
                        }.sortedByDescending { it.timestamp }
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Закладки 🔖", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) { ShimmerPostItem() }
                }
            } else if (bookmarkedPosts.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("У вас пока нет закладок", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(bookmarkedPosts, key = { it.id }) { post ->
                        PostItem(post = post, onUserClick = onUserClick, onHashtagClick = onHashtagClick)
                    }
                }
            }
        }
    }
}

@Composable
fun SharePostDialog(
    postId: String,
    postText: String,
    onDismiss: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val currentUserId = sharedPrefs.getString("username", "") ?: ""

    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Загружаем список пользователей
    LaunchedEffect(Unit) {
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                allUsers = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    // ОПТИМИЗИРОВАННЫЙ ПОИСК: фильтрует список мгновенно и без учета регистра
    val filteredUsers by remember {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allUsers.filter { it.id != currentUserId }
            } else {
                allUsers.filter { user ->
                    user.id != currentUserId && (
                        user.name.contains(searchQuery, ignoreCase = true) ||
                        user.username.contains(searchQuery, ignoreCase = true) ||
                        user.id.contains(searchQuery, ignoreCase = true)
                    )
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переслать пост", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ПОЛЕ ВВОДА ПОИСКА
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск людей...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                
                if (isLoading) {
                    Column(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { ShimmerUserItem() }
                    }
                } else if (filteredUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        Text("Никого не найдено", color = Color.Gray, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredUsers, key = { it.id }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        sendForwardedPost(db, currentUserId, user.id, postId, postText)
                                        onDismiss()
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Аватар
                                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                                    if (!user.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            user.name.take(1).uppercase(),
                                            modifier = Modifier.align(Alignment.Center),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("@${user.id}", fontSize = 13.sp, color = Color.Gray)
                                }
                                
                                Button(
                                    onClick = {
                                        sendForwardedPost(db, currentUserId, user.id, postId, postText)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.height(36.dp).bounceClick()
                                ) {
                                    Text("Отправить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

fun sendForwardedPost(db: FirebaseFirestore, senderId: String, receiverId: String, postId: String, postText: String) {
    val participants = listOf(senderId, receiverId).sorted()
    val chatId = participants.joinToString("_")

    val chatRef = db.collection("chats").document(chatId)
    val messageRef = chatRef.collection("messages").document()

    val message = hashMapOf(
        "senderId" to senderId,
        "text" to postText,
        "timestamp" to System.currentTimeMillis(),
        "forwardedPostId" to postId
    )

    db.runTransaction { transaction ->
        transaction.set(chatRef, hashMapOf(
            "id" to chatId,
            "participants" to participants,
            "lastMessage" to "Пересланный пост",
            "lastMessageTimestamp" to System.currentTimeMillis()
        ))
        transaction.set(messageRef, message)
    }
}

// ==========================================
// 8. ОНБОРДИНГ И ПОДСКАЗКИ (COACH MARKS)
// ==========================================
@Composable
fun OnboardingOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = true, onClick = { /* Заглушка */ }, onClickLabel = null)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Text(
                "👋 Добро пожаловать!",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Жирпем — это место для общения без границ. Вот пара советов, как здесь все устроено:",
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OnboardingFeatureItem(
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFE91E63),
                    text = "Лайкай посты, которые тебе нравятся"
                )
                OnboardingFeatureItem(
                    icon = Icons.Default.Bookmark,
                    color = MaterialTheme.colorScheme.primary,
                    text = "Сохраняй важное в закладки одним нажатием"
                )
                OnboardingFeatureItem(
                    icon = Icons.Default.Repeat,
                    color = Color.Green,
                    text = "Делай репосты, чтобы поделиться с другими"
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Всё понятно!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OnboardingFeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun openOrCreateChat(context: Context, currentUserId: String, targetUserId: String, onChatReady: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("chats")
        .whereArrayContains("participants", currentUserId)
        .get()
        .addOnSuccessListener { snapshot ->
            // Ищем чат, где есть и второй участник (и их всего двое)
            val existingChat = snapshot.documents.find { doc ->
                val participants = doc.get("participants") as? List<String>
                participants?.contains(targetUserId) == true && (participants.size == 2 || currentUserId == targetUserId)
            }

            if (existingChat != null) {
                onChatReady(existingChat.id)
            } else {
                // Если чата нет, создаем новый с уникальным ID
                val newChatRef = db.collection("chats").document()
                val newChat = hashMapOf(
                    "id" to newChatRef.id,
                    "participants" to listOf(currentUserId, targetUserId),
                    "lastMessage" to "",
                    "lastMessageTimestamp" to System.currentTimeMillis()
                )
                newChatRef.set(newChat).addOnSuccessListener {
                    onChatReady(newChatRef.id)
                }.addOnFailureListener { e ->
                    android.widget.Toast.makeText(context, "Ошибка создания чата: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        .addOnFailureListener { e ->
            android.widget.Toast.makeText(context, "Ошибка сети: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
}
