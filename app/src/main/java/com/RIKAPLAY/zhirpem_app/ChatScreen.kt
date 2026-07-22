@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)

package com.RIKAPLAY.zhirpem_app

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

import android.media.MediaRecorder
import android.net.Uri
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.QualitySelector
import androidx.camera.video.Quality
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.Recording
import androidx.camera.core.CameraSelector
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.io.File
import android.os.Build
import kotlinx.coroutines.delay

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(chatId: String, onBack: () -> Unit, onNavigateToPost: (String) -> Unit, onOpenCamera: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var peerName by remember { mutableStateOf("Чат") }
    var peerAvatarUrl by remember { mutableStateOf<String?>(null) }
    
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSendingMedia by remember { mutableStateOf(false) }
    var isPeerTyping by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val rtdb = FirebaseDatabase.getInstance()
    val peerId = chatId.split("_").firstOrNull { it != myUsername } ?: ""

    // Слушатель статуса "Печатает..."
    LaunchedEffect(chatId) {
        if (peerId.isNotEmpty()) {
            val typingRef = rtdb.getReference("chats/$chatId/typing/$peerId")
            typingRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    isPeerTyping = snapshot.getValue(Boolean::class.java) ?: false
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }

    // Пометка сообщений как прочитанных
    LaunchedEffect(messages) {
        val unreadMessages = messages.filter { it.senderId != myUsername && !it.isRead }
        if (unreadMessages.isNotEmpty()) {
            unreadMessages.forEach { msg ->
                db.collection("chats").document(chatId).collection("messages").document(msg.id)
                    .update("isRead", true)
            }
        }
    }

    // ПРАВА ДОСТУПА
    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val videoPermissionsState = rememberMultiplePermissionsState(
        listOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
    )
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    // СОСТОЯНИЯ ЗАПИСИ
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var videoFile by remember { mutableStateOf<File?>(null) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var currentCameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    val previewView = remember { androidx.camera.view.PreviewView(context) }

    // Инициализация CameraX для видео-сообщений
    LaunchedEffect(Unit) {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.LOWEST))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            activeRecording?.stop()
        }
    }

    // Лаунчеры для медиа в чате
    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isSendingMedia = true
            val isVideo = it.toString().contains("video")
            ChatRepository.uploadMediaToCloudinary(
                context = context,
                fileUri = it,
                messageType = if (isVideo) "video_square" else "image",
                chatId = chatId,
                currentUserId = myUsername,
                senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: ""
            )
            isSendingMedia = false 
        }
    }

    val gifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isSendingMedia = true
            ChatRepository.uploadMediaToCloudinary(
                context = context,
                fileUri = it,
                messageType = "image",
                chatId = chatId,
                currentUserId = myUsername,
                senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: ""
            )
            isSendingMedia = false
        }
    }

    LaunchedEffect(chatId) {
        val peerId = chatId.split("_").firstOrNull { it != myUsername } ?: ""
        if (peerId.isNotEmpty()) {
            db.collection("users").document(peerId).get().addOnSuccessListener {
                peerName = it.getString("name") ?: peerId
                peerAvatarUrl = it.getString("avatarUrl")
            }
        }

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)?.copy(id = doc.id)
                    }
                }
            }
    }

    val animationsEnabled = LocalAnimationsEnabled.current
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            if (animationsEnabled) {
                listState.animateScrollToItem(messages.size - 1)
            } else {
                listState.scrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            if (!peerAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = peerAvatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(peerName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            PresenceIndicator(
                                username = chatId.split("_").firstOrNull { it != myUsername } ?: "",
                                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                                size = 10.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(peerName, fontWeight = FontWeight.Bold)
                            if (isPeerTyping) {
                                Text(
                                    "печатает...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                PresenceText(username = peerId)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        val isMyMessage = message.senderId == myUsername
                        
                        MessageBubble(
                            chatId = chatId,
                            message = message,
                            isMyMessage = isMyMessage,
                            onNavigateToPost = onNavigateToPost,
                            onReply = { replyingToMessage = it },
                            onReplyClick = { replyId ->
                                val targetIndex = messages.indexOfFirst { it.id == replyId }
                                if (targetIndex != -1) {
                                    scope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            }
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (replyingToMessage != null) {
                        ReplyPanel(
                            message = replyingToMessage!!,
                            onCancel = { replyingToMessage = null }
                        )
                    }

                    Box {
                        ChatInputBar(
                            onSendText = { text ->
                                sendMessage(
                                    db = db,
                                    chatId = chatId,
                                    senderId = myUsername,
                                    text = text.trim(),
                                    replyToId = replyingToMessage?.id,
                                    replyToText = replyingToMessage?.let {
                                        if (it.text.isNotEmpty()) it.text else "Медиафайл"
                                    },
                                    senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                                    senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: ""
                                )
                                replyingToMessage = null
                            },
                            onTyping = { isTyping ->
                                rtdb.getReference("chats/$chatId/typing/$myUsername").setValue(isTyping)
                            },
                            onStartAudioRecord = { 
                                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    try {
                                        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                                        audioFile = file
                                        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            MediaRecorder(context)
                                        } else {
                                            @Suppress("DEPRECATION")
                                            MediaRecorder()
                                        }.apply {
                                            setAudioSource(MediaRecorder.AudioSource.MIC)
                                            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                            setOutputFile(file.absolutePath)
                                            prepare()
                                            start()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                                }
                            },
                            onStopAudioRecord = { shouldSend -> 
                                try {
                                    mediaRecorder?.apply {
                                        stop()
                                        release()
                                    }
                                    mediaRecorder = null
                                    
                                    if (shouldSend && audioFile != null) {
                                        ChatRepository.uploadMediaToCloudinary(
                                            context = context,
                                            fileUri = Uri.fromFile(audioFile!!),
                                            messageType = "voice",
                                            chatId = chatId,
                                            currentUserId = myUsername,
                                            senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                                            senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: ""
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onStartVideoRecord = { 
                                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    
                                    try {
                                        val file = File(context.cacheDir, "video_${System.currentTimeMillis()}.mp4")
                                        videoFile = file
                                        val outputOptions = FileOutputOptions.Builder(file).build()
                                        
                                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                                        cameraProvider.unbindAll()
                                        
                                        val preview = androidx.camera.core.Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }

                                        cameraProvider.bindToLifecycle(lifecycleOwner, currentCameraSelector, preview, videoCapture)

                                        activeRecording = videoCapture?.output
                                            ?.prepareRecording(context, outputOptions)
                                            ?.withAudioEnabled()
                                            ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                                if (event is VideoRecordEvent.Finalize) {
                                                    if (!event.hasError() && videoFile != null) {
                                                        // File is ready
                                                    }
                                                }
                                            }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO))
                                }
                            },
                            onStopVideoRecord = { shouldSend -> 
                                activeRecording?.stop()
                                activeRecording = null
                                isRecordingPaused = false
                                currentCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                
                                if (shouldSend && videoFile != null) {
                                    scope.launch {
                                        delay(500)
                                        ChatRepository.uploadMediaToCloudinary(
                                            context = context,
                                            fileUri = Uri.fromFile(videoFile!!),
                                            messageType = "video_square",
                                            chatId = chatId,
                                            currentUserId = myUsername,
                                            senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                                            senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: ""
                                        )
                                    }
                                }
                            },
                            onMoreClick = { isMenuExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Камера")
                                    }
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    onOpenCamera()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Фото или Видео")
                                    }
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    mediaLauncher.launch("*/*")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Gif, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("GIF-анимация")
                                    }
                                },
                                onClick = {
                                    isMenuExpanded = false
                                    gifLauncher.launch("image/gif")
                                }
                            )
                        }
                    }
                }
            }

            if (activeRecording != null && videoCapture != null) {
                VideoRecordOverlay(
                    previewView = previewView,
                    isPaused = isRecordingPaused,
                    onPauseToggle = {
                        if (isRecordingPaused) {
                            activeRecording?.resume()
                        } else {
                            activeRecording?.pause()
                        }
                        isRecordingPaused = !isRecordingPaused
                    },
                    onFlipCamera = {
                        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        
                        try {
                            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                            cameraProvider.unbindAll()
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            cameraProvider.bindToLifecycle(lifecycleOwner, currentCameraSelector, preview, videoCapture)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    chatId: String,
    message: Message,
    isMyMessage: Boolean,
    onNavigateToPost: (String) -> Unit,
    onReply: (Message) -> Unit,
    onReplyClick: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val fontSizeMultiplier = LocalFontSize.current
    var offsetX by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 50.dp.toPx() }
    var isHapticDone by remember { mutableStateOf(false) }

    var isMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(message.text) }

    val db = FirebaseFirestore.getInstance()

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать сообщение") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (message.id.isNotEmpty()) {
                        db.collection("chats").document(chatId).collection("messages").document(message.id)
                            .update("text", editedText)
                    }
                    showEditDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить сообщение?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.collection("chats").document(chatId).collection("messages").document(message.id).delete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
            }
        )
    }

    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX >= swipeThreshold) {
                            onReply(message)
                        }
                        offsetX = 0f
                        isHapticDone = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val newOffset = (offsetX + dragAmount).coerceIn(0f, swipeThreshold * 1.5f)
                        offsetX = newOffset
                        
                        if (offsetX >= swipeThreshold && !isHapticDone) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isHapticDone = true
                        } else if (offsetX < swipeThreshold) {
                            isHapticDone = false
                        }
                    }
                )
            }
    ) {
        if (offsetX > 0) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = (offsetX / swipeThreshold).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animatedOffsetX },
            horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMyMessage) 16.dp else 4.dp,
                    bottomEnd = if (isMyMessage) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { /* Клик */ },
                        onLongClick = { if (isMyMessage) isMenuExpanded = true }
                    )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (message.replyToId != null && message.replyToText != null) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .clickable { onReplyClick(message.replyToId) }
                                .height(IntrinsicSize.Min)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Ответ",
                                    fontSize = 11.sp * fontSizeMultiplier,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = message.replyToText,
                                    fontSize = 13.sp * fontSizeMultiplier,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isMyMessage) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (!message.forwardedPostId.isNullOrEmpty()) {
                        Surface(
                            color = if (isMyMessage) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .fillMaxWidth()
                                .clickable { onNavigateToPost(message.forwardedPostId) }
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "🔗 Пересланный пост",
                                    fontSize = 12.sp * fontSizeMultiplier,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyMessage) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (message.mediaUrl.isNotEmpty()) {
                        val mediaModifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(bottom = if (message.text.isNotEmpty()) 8.dp else 0.dp)

                        when (message.mediaType) {
                            MediaType.IMAGE -> {
                                AsyncImage(
                                    model = message.mediaUrl,
                                    contentDescription = "Изображение",
                                    modifier = mediaModifier.heightIn(max = 400.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            MediaType.GIF -> {
                                GifPlayer(gifUrl = message.mediaUrl, modifier = mediaModifier.height(240.dp))
                            }
                            MediaType.VIDEO -> {
                                if (message.replyToId == "voice" || message.mediaUrl.contains("audio_") || message.mediaUrl.contains(".m4a")) {
                                    VoiceMessageBubble(audioUrl = message.mediaUrl, isMyMessage = isMyMessage)
                                } else {
                                    VideoMessageBubble(rawVideoUrl = message.mediaUrl, isMyMessage = isMyMessage)
                                }
                            }
                            else -> {}
                        }
                    }

                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            fontSize = 15.sp * fontSizeMultiplier,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (isMyMessage) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            Text(
                                text = timeFormat.format(Date(message.timestamp)),
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        Text(
                            text = timeFormat.format(Date(message.timestamp)),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.End).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                isMenuExpanded = false
                                showEditDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMenuExpanded = false
                                showDeleteDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyPanel(message: Message, onCancel: () -> Unit) {
    val fontSizeMultiplier = LocalFontSize.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ответ на сообщение",
                fontSize = 12.sp * fontSizeMultiplier,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (message.text.isNotEmpty()) message.text else "Медиафайл",
                fontSize = 14.sp * fontSizeMultiplier,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Отмена", modifier = Modifier.size(16.dp))
        }
    }
}

fun sendMessage(
    db: FirebaseFirestore, 
    chatId: String, 
    senderId: String, 
    text: String, 
    mediaUrl: String = "", 
    mediaType: MediaType = MediaType.NONE,
    replyToId: String? = null,
    replyToText: String? = null,
    senderName: String = "",
    senderAvatar: String = ""
) {
    val chatRef = db.collection("chats").document(chatId)
    val messageRef = chatRef.collection("messages").document()

    val message = hashMapOf(
        "senderId" to senderId,
        "text" to text,
        "timestamp" to System.currentTimeMillis(),
        "mediaUrl" to mediaUrl,
        "mediaType" to mediaType.name,
        "forwardedPostId" to null,
        "replyToId" to replyToId,
        "replyToText" to replyToText,
        "isRead" to false
    )

    db.runTransaction { transaction ->
        transaction.set(messageRef, message)
        val lastMsgText = if (mediaUrl.isNotEmpty()) "📎 Медиафайл" else text
        transaction.update(chatRef, "lastMessage", lastMsgText)
        transaction.update(chatRef, "lastMessageTimestamp", System.currentTimeMillis())
    }

    // Уведомление собеседнику
    val peerId = chatId.split("_").firstOrNull { it != senderId } ?: ""
    if (peerId.isNotEmpty()) {
        sendNotification(
            db = db,
            senderId = senderId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            receiverId = peerId,
            type = "MESSAGE",
            text = text.ifEmpty { "📎 Медиафайл" }
        )
    }
}
