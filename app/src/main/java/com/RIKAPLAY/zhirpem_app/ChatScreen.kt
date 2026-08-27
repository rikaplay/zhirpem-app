@file:OptIn(ExperimentalMaterial3Api::class)

package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.ClipboardManager
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import android.os.Build
import androidx.lifecycle.viewmodel.compose.viewModel
import com.RIKAPLAY.zhirpem_app.webrtc.GlobalCallViewModel
import com.RIKAPLAY.zhirpem_app.webrtc.CallScreen
import com.RIKAPLAY.zhirpem_app.webrtc.IncomingCallOverlay
import com.RIKAPLAY.zhirpem_app.webrtc.CallService
import com.RIKAPLAY.zhirpem_app.webrtc.CallViewModel
import com.RIKAPLAY.zhirpem_app.webrtc.FirestoreSignalingClient
import kotlinx.coroutines.delay

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    chatId: String, 
    onBack: () -> Unit, 
    onNavigateToPost: (String) -> Unit, 
    onNavigateToProfile: (String) -> Unit,
    onOpenCamera: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "user_${System.currentTimeMillis()}") ?: "user_${System.currentTimeMillis()}"
    val peerId = chatId.split("_").firstOrNull { it != myUsername } ?: ""

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var chatData by remember { mutableStateOf<Chat?>(null) }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessageForMenu by remember { mutableStateOf<Message?>(null) }
    var messageToForward by remember { mutableStateOf<Message?>(null) }
    var isForwardingDialogOpen by remember { mutableStateOf(false) }
    var isProfileOverlayOpen by remember { mutableStateOf(false) }
    var isSettingsDialogOpen by remember { mutableStateOf(false) }
    var showCallTypeDialog by remember { mutableStateOf(false) }

    var myUser by remember { mutableStateOf<User?>(null) }
    var peerUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(myUsername) {
        db.collection("users").document(myUsername).get().addOnSuccessListener {
            myUser = it.toObject(User::class.java)
        }
    }
    
    LaunchedEffect(peerId) {
        if (peerId.isNotEmpty()) {
            db.collection("users").document(peerId).get().addOnSuccessListener {
                peerUser = it.toObject(User::class.java)
            }
        }
    }

    val globalCallViewModel: GlobalCallViewModel = viewModel()
    val isCallActive by globalCallViewModel.isCallActive.collectAsState()

    var peerName by remember { mutableStateOf("Чат") }
    var peerAvatarUrl by remember { mutableStateOf<String?>(null) }
    
    var isMenuExpanded by remember { mutableStateOf(value = false) }
    var isSendingMedia by remember { mutableStateOf(value = false) }
    var isPeerTyping by remember { mutableStateOf(value = false) }
    
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val rtdb = FirebaseDatabase.getInstance()

    var messageLimit by remember { mutableLongStateOf(10L) }

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).addSnapshotListener { snapshot, _ ->
            chatData = snapshot?.toObject(Chat::class.java)
        }
        
        if (peerId.isNotEmpty()) {
            val typingRef = rtdb.getReference("chats/$chatId/typing/$peerId")
            typingRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    isPeerTyping = snapshot.getValue(Boolean::class.java) ?: false
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                }
            })
        }
    }

    LaunchedEffect(messages) {
        val unreadMessages = messages.filter { it.senderId != myUsername && !it.isRead }
        if (unreadMessages.isNotEmpty()) {
            unreadMessages.forEach { msg ->
                db.collection("chats").document(chatId).collection("messages").document(msg.id)
                    .update("isRead", true)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var videoFile by remember { mutableStateOf<File?>(null) }
    var isRecordingPaused by remember { mutableStateOf(false) }
    var currentCameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    val previewView = remember { androidx.camera.view.PreviewView(context) }

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

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val isPeerVerifiedOnly = peerUser?.isOnlyVerifiedMessages ?: false
            val isMeVerified = (myUser?.blueBadge == true) || (myUser?.yellowBadge == true)
            if (isPeerVerifiedOnly && !isMeVerified) {
                Toast.makeText(context, "Пользователь ограничил получение сообщений", Toast.LENGTH_SHORT).show()
                return@let
            }

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
            val isPeerVerifiedOnly = peerUser?.isOnlyVerifiedMessages ?: false
            val isMeVerified = (myUser?.blueBadge == true) || (myUser?.yellowBadge == true)
            if (isPeerVerifiedOnly && !isMeVerified) {
                Toast.makeText(context, "Пользователь ограничил получение сообщений", Toast.LENGTH_SHORT).show()
                return@let
            }

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

    LaunchedEffect(chatId, messageLimit) {
        val peerId = chatId.split("_").firstOrNull { it != myUsername } ?: ""
        if (peerId.isNotEmpty()) {
            db.collection("users").document(peerId).get().addOnSuccessListener {
                peerName = it.getString("name") ?: peerId
                peerAvatarUrl = it.getString("avatarUrl")
            }
        }

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .limitToLast(messageLimit)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    val now = System.currentTimeMillis()
                    messages = it.documents.mapNotNull { doc ->
                        val msg = doc.toObject(Message::class.java)?.copy(id = doc.id)
                        if (msg?.expiresAt != null && msg.expiresAt < now) {
                            null
                        } else msg
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isProfileOverlayOpen = true }
                    ) {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Обои чата на самый нижний слой
            if (!chatData?.wallpaperUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = chatData?.wallpaperUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if ((selectedMessageForMenu != null || isProfileOverlayOpen) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(15.dp)
                        } else Modifier
                    )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        if (messages.size.toLong() >= messageLimit) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { messageLimit += 10 }) {
                                    Text("Загрузить предыдущие сообщения", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    itemsIndexed(messages) { index, message ->
                        val isMyMessage = message.senderId == myUsername
                        
                        if (index == 0) {
                            DateHeader(timestamp = message.timestamp)
                        } else {
                            val prevMessage = messages[index - 1]
                            val cal1 = Calendar.getInstance().apply { timeInMillis = prevMessage.timestamp }
                            val cal2 = Calendar.getInstance().apply { timeInMillis = message.timestamp }
                            val isDifferentDay = cal1[Calendar.YEAR] != cal2[Calendar.YEAR] || 
                                               cal1[Calendar.DAY_OF_YEAR] != cal2[Calendar.DAY_OF_YEAR]
                            if (isDifferentDay) {
                                DateHeader(timestamp = message.timestamp)
                            }
                        }
                        
                        MessageBubble(
                            chatId = chatId,
                            message = message,
                            isMyMessage = isMyMessage,
                            theme = chatData?.theme ?: "DEFAULT",
                            onNavigateToPost = onNavigateToPost,
                            onReply = { replyingToMessage = it },
                            onReplyClick = { replyId ->
                                val targetIndex = messages.indexOfFirst { it.id == replyId }
                                if (targetIndex != -1) {
                                    scope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            },
                            onLongClick = { selectedMessageForMenu = it }
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
                                val isPeerVerifiedOnly = peerUser?.isOnlyVerifiedMessages ?: false
                                val isMeVerified = (myUser?.blueBadge == true) || (myUser?.yellowBadge == true)
                                
                                if (isPeerVerifiedOnly && !isMeVerified) {
                                    Toast.makeText(context, "Пользователь ограничил получение сообщений", Toast.LENGTH_SHORT).show()
                                } else {
                                    sendMessage(
                                        db = db,
                                        chatId = chatId,
                                        senderId = myUsername,
                                        text = text.trim(),
                                        replyToId = replyingToMessage?.id,
                                        replyToText = replyingToMessage?.let {
                                            it.text.ifEmpty { "Медиафайл" }
                                        },
                                        senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                                        senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: "",
                                        expiresAt = chatData?.disappearingDuration?.let { if (it > 0) System.currentTimeMillis() + it else null }
                                    )
                                    replyingToMessage = null
                                }
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
                                        val isPeerVerifiedOnly = peerUser?.isOnlyVerifiedMessages ?: false
                                        val isMeVerified = (myUser?.blueBadge == true) || (myUser?.yellowBadge == true)
                                        if (isPeerVerifiedOnly && !isMeVerified) {
                                            Toast.makeText(context, "Пользователь ограничил получение сообщений", Toast.LENGTH_SHORT).show()
                                        } else {
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
                                                if (event is VideoRecordEvent.Finalize && !event.hasError() && videoFile != null) {
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
                                    val isPeerVerifiedOnly = peerUser?.isOnlyVerifiedMessages ?: false
                                    val isMeVerified = (myUser?.blueBadge == true) || (myUser?.yellowBadge == true)
                                    if (isPeerVerifiedOnly && !isMeVerified) {
                                        Toast.makeText(context, "Пользователь ограничил получение сообщений", Toast.LENGTH_SHORT).show()
                                    } else {
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

            if (selectedMessageForMenu != null) {
                MessageContextMenuOverlay(
                    message = selectedMessageForMenu!!,
                    isMyMessage = selectedMessageForMenu!!.senderId == myUsername,
                    onDismiss = { selectedMessageForMenu = null },
                    onReaction = { emoji ->
                        addReactionToMessage(db, chatId, selectedMessageForMenu!!.id, myUsername, emoji)
                        selectedMessageForMenu = null
                    },
                    onReply = {
                        replyingToMessage = selectedMessageForMenu
                        selectedMessageForMenu = null
                    },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("сообщение", selectedMessageForMenu!!.text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                        selectedMessageForMenu = null
                    },
                    onPin = {
                        Toast.makeText(context, "Закреплено", Toast.LENGTH_SHORT).show()
                        selectedMessageForMenu = null
                    },
                    onForward = {
                        messageToForward = selectedMessageForMenu
                        isForwardingDialogOpen = true
                        selectedMessageForMenu = null
                    },
                    onDelete = {
                        db.collection("chats").document(chatId).collection("messages").document(selectedMessageForMenu!!.id).delete()
                        selectedMessageForMenu = null
                    },
                    onSelect = {
                        Toast.makeText(context, "Выбрано", Toast.LENGTH_SHORT).show()
                        selectedMessageForMenu = null
                    }
                )
            }

            if (isForwardingDialogOpen && messageToForward != null) {
                ForwardMessageDialog(
                    message = messageToForward!!,
                    onDismiss = {
                        isForwardingDialogOpen = false
                        messageToForward = null
                    },
                    onForward = { targetChatId, hideSender ->
                        forwardMessage(
                            db = db,
                            targetChatId = targetChatId,
                            message = messageToForward!!,
                            senderId = myUsername,
                            senderName = sharedPrefs.getString("name", "Аноним") ?: "Аноним",
                            senderAvatar = sharedPrefs.getString("avatarUrl", "") ?: "",
                            hideSender = hideSender
                        )
                        isForwardingDialogOpen = false
                        messageToForward = null
                        Toast.makeText(context, "Сообщение переслано", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (isProfileOverlayOpen) {
                Dialog(
                    onDismissRequest = { isProfileOverlayOpen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { isProfileOverlayOpen = false },
                        contentAlignment = Alignment.Center
                    ) {
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }

                        AnimatedVisibility(
                            visible = visible,
                            enter = scaleIn(initialScale = 0.8f) + fadeIn(),
                            exit = scaleOut(targetScale = 0.8f) + fadeOut()
                        ) {
                            ChatProfileOverlay(
                                peerId = peerId,
                                peerName = peerName,
                                peerAvatarUrl = peerAvatarUrl,
                                onDismiss = { isProfileOverlayOpen = false },
                                onNavigateToProfile = { 
                                    onNavigateToProfile(peerId)
                                    isProfileOverlayOpen = false
                                },
                                onOpenSettings = {
                                    isSettingsDialogOpen = true
                                },
                                onStartCall = {
                                    isProfileOverlayOpen = false
                                    showCallTypeDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (showCallTypeDialog) {
                Dialog(
                    onDismissRequest = { showCallTypeDialog = false }
                ) {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Тип звонка", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Как вы хотите позвонить $peerName?", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                
                                Spacer(Modifier.height(24.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            showCallTypeDialog = false
                                            CallService.start(context)
                                            globalCallViewModel.startOutgoingCall(chatId, "AUDIO")
                                        },
                                        modifier = Modifier.weight(1f).height(100.dp).bounceClick(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Call, null, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Аудио")
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            showCallTypeDialog = false
                                            CallService.start(context)
                                            globalCallViewModel.startOutgoingCall(chatId, "VIDEO")
                                        },
                                        modifier = Modifier.weight(1f).height(100.dp).bounceClick(),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Videocam, null, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(8.dp))
                                            Text("Видео")
                                        }
                                    }
                                }
                                
                                TextButton(onClick = { showCallTypeDialog = false }, modifier = Modifier.padding(top = 12.dp).bounceClick()) {
                                    Text("Отмена")
                                }
                            }
                        }
                    }
                }
            }

            /* Global Call UI is now in MainActivity */

            if (isSettingsDialogOpen) {
                ChatSettingsDialog(
                    chatId = chatId,
                    onDismiss = { isSettingsDialogOpen = false }
                )
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
fun MessageContextMenuOverlay(
    message: Message,
    isMyMessage: Boolean,
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val emojis = listOf("❤️", "🥰", "👎", "👍", "🔥", "👏", "😁")
    
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(16.dp),
                horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        emojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable { onReaction(emoji) },
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    modifier = Modifier.width(220.dp)
                ) {
                    Column {
                        ContextMenuItem("Ответить", Icons.AutoMirrored.Filled.Reply, onReply)
                        ContextMenuItem("Скопировать", Icons.Default.ContentCopy, onCopy)
                        ContextMenuItem("Закрепить", Icons.Default.PushPin, onPin)
                        ContextMenuItem("Переслать", Icons.AutoMirrored.Filled.Forward, onForward)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ContextMenuItem("Удалить", Icons.Default.Delete, onDelete, isDestructive = true)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ContextMenuItem("Выбрать", Icons.Default.CheckCircleOutline, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
fun ContextMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
    }
}

@Composable
fun ChatProfileOverlay(
    peerId: String,
    peerName: String,
    peerAvatarUrl: String?,
    onDismiss: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .bounceClick()
                .clickable { onNavigateToProfile() },
            contentAlignment = Alignment.Center
        ) {
                    if (!peerAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = peerAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = peerName.take(1).uppercase(),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = peerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onNavigateToProfile() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CircleActionButton(
                        icon = Icons.Default.Person,
                        label = "Профиль",
                        onClick = onNavigateToProfile
                    )
                    CircleActionButton(
                        icon = Icons.Default.Call,
                        label = "Звонок",
                        onClick = onStartCall
                    )
                    CircleActionButton(
                        icon = Icons.Default.Settings,
                        label = "Настройки",
                        onClick = onOpenSettings
                    )
                }
            }
}

@Composable
fun CircleActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .bounceClick()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ChatSettingsDialog(chatId: String, onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var chatData by remember { mutableStateOf<Chat?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).get().addOnSuccessListener {
            chatData = it.toObject(Chat::class.java)
        }
    }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isUploading = true
            uploadImageToCloudinary(
                context = context,
                imageUri = it,
                mediaType = MediaType.IMAGE,
                cloudName = "dcwp4nm3e",
                uploadPreset = "ProfilePIC",
                onSuccess = { url ->
                    db.collection("chats").document(chatId).update("wallpaperUrl", url)
                    isUploading = false
                },
                onError = { isUploading = false }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки чата") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Цветовая тема", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = listOf("DEFAULT", "BLUE", "GREEN", "PURPLE", "ORANGE")
                    themes.forEach { theme ->
                        val color = when(theme) {
                            "BLUE" -> Color(0xFF2196F3)
                            "GREEN" -> Color(0xFF4CAF50)
                            "PURPLE" -> Color(0xFF9C27B0)
                            "ORANGE" -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (chatData?.theme == theme) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable {
                                    db.collection("chats").document(chatId).update("theme", theme)
                                    chatData = chatData?.copy(theme = theme)
                                }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsItem(
                    title = "Обои чата",
                    icon = Icons.Default.Image,
                    onClick = { wallpaperLauncher.launch("image/*") }
                )
                if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Исчезающие сообщения", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                val durations = listOf(
                    0L to "Выкл",
                    3600000L to "1 час",
                    86400000L to "1 день",
                    604800000L to "1 неделя"
                )
                
                durations.forEach { (duration, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                db.collection("chats").document(chatId).update("disappearingDuration", duration)
                                chatData = chatData?.copy(disappearingDuration = duration)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = chatData?.disappearingDuration == duration, onClick = {
                            db.collection("chats").document(chatId).update("disappearingDuration", duration)
                            chatData = chatData?.copy(disappearingDuration = duration)
                        })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Готово") }
        }
    )
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ForwardMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onForward: (String, Boolean) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val myUsername = context.getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("username", "") ?: ""
    
    var chats by remember { mutableStateOf(listOf<Chat>()) }
    var hideSenderName by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        db.collection("chats")
            .whereArrayContains("participants", myUsername)
            .get()
            .addOnSuccessListener { snap ->
                chats = snap.documents.mapNotNull { it.toObject(Chat::class.java) }
                    .sortedByDescending { it.lastMessageTimestamp }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переслать сообщение") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Скрыть имя отправителя", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hideSenderName, onCheckedChange = { hideSenderName = it })
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск чата...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    val filteredChats = chats.filter { chat ->
                        val peerId = chat.participants.firstOrNull { it != myUsername } ?: ""
                        peerId.contains(searchQuery, ignoreCase = true)
                    }

                    items(filteredChats) { chat ->
                        val peerId = chat.participants.firstOrNull { it != myUsername } ?: ""
                        var peerName by remember { mutableStateOf(peerId) }

                        LaunchedEffect(peerId) {
                            db.collection("users").document(peerId).get().addOnSuccessListener { doc ->
                                peerName = doc.getString("name") ?: peerId
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onForward(chat.id, hideSenderName) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(peerName.take(1).uppercase(), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(peerName, fontWeight = FontWeight.Medium)
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

fun forwardMessage(
    db: FirebaseFirestore,
    targetChatId: String,
    message: Message,
    senderId: String,
    senderName: String,
    senderAvatar: String,
    hideSender: Boolean
) {
    val chatRef = db.collection("chats").document(targetChatId)
    val messageRef = chatRef.collection("messages").document()

    val forwardedMessage = hashMapOf(
        "senderId" to senderId,
        "text" to message.text,
        "timestamp" to System.currentTimeMillis(),
        "mediaUrl" to message.mediaUrl,
        "mediaType" to message.mediaType.name,
        "forwardedPostId" to message.forwardedPostId,
        "forwardedFrom" to if (hideSender) null else message.senderId,
        "replyToId" to null,
        "replyToText" to null,
        "isRead" to false
    )

    db.runTransaction { transaction ->
        transaction[messageRef] = forwardedMessage
        val lastMsgText = if (message.mediaUrl.isNotEmpty()) "📎 Переслано: Медиафайл" else "📎 Переслано: ${message.text}"
        transaction.update(chatRef, "lastMessage", lastMsgText)
        transaction.update(chatRef, "lastMessageTimestamp", System.currentTimeMillis())
    }
}

fun addReactionToMessage(db: FirebaseFirestore, chatId: String, messageId: String, userId: String, emoji: String) {
    db.collection("chats").document(chatId).collection("messages").document(messageId)
        .update("reactions.$userId", emoji)
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
    senderAvatar: String = "",
    expiresAt: Long? = null
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
        "isRead" to false,
        "expiresAt" to expiresAt
    )

    db.runTransaction { transaction ->
        transaction[messageRef] = message
        val lastMsgText = if (mediaUrl.isNotEmpty()) "📎 Медиафайл" else text
        transaction.update(chatRef, "lastMessage", lastMsgText)
        transaction.update(chatRef, "lastMessageTimestamp", System.currentTimeMillis())
    }

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

@Composable
fun MessageBubble(
    chatId: String,
    message: Message,
    isMyMessage: Boolean,
    theme: String,
    onNavigateToPost: (String) -> Unit,
    onReply: (Message) -> Unit,
    onReplyClick: (String) -> Unit,
    onLongClick: (Message) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val isVibrationEnabled = remember(sharedPrefs) { sharedPrefs.getBoolean("vibration_enabled", true) }
    val fontSizeMultiplier = LocalFontSize.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 50.dp.toPx() }
    var isHapticDone by remember { mutableStateOf(value = false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedText by remember { mutableStateOf(message.text) }

    val db = FirebaseFirestore.getInstance()

    val bubbleColor = if (isMyMessage) {
        when (theme) {
            "BLUE" -> Color(0xFF2196F3)
            "GREEN" -> Color(0xFF4CAF50)
            "PURPLE" -> Color(0xFF9C27B0)
            "ORANGE" -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.primary
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

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
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = (offsetX + dragAmount).coerceIn(0f, swipeThreshold * 1.5f)
                        offsetX = newOffset
                        
                        if (offsetX >= swipeThreshold && !isHapticDone) {
                            if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    containerColor = bubbleColor,
                    contentColor = if (isMyMessage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { /* Клик */ },
                        onLongClick = { onLongClick(message) }
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

                    if (!message.forwardedFrom.isNullOrEmpty()) {
                        var forwardedName by remember { mutableStateOf(message.forwardedFrom) }
                        LaunchedEffect(message.forwardedFrom) {
                            db.collection("users").document(message.forwardedFrom).get().addOnSuccessListener { doc ->
                                doc.getString("name")?.let { forwardedName = it }
                            }
                        }
                        Text(
                            text = "Переслано от $forwardedName",
                            fontSize = 11.sp * fontSizeMultiplier,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = if (isMyMessage) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
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
                                    modifier = mediaModifier
                                        .heightIn(max = 400.dp)
                                        .adaptiveImageSize(isLandscape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            MediaType.GIF -> {
                                GifPlayer(gifUrl = message.mediaUrl, modifier = mediaModifier.height(240.dp).adaptiveImageSize(isLandscape))
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

                    if (message.reactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            message.reactions.values.distinct().take(3).forEach { emoji ->
                                Text(text = emoji, fontSize = 12.sp)
                            }
                            if (message.reactions.size > 1) {
                                Text(
                                    text = message.reactions.size.toString(),
                                    fontSize = 10.sp,
                                    color = if (isMyMessage) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
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
                text = message.text.ifEmpty { "Медиафайл" },
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
