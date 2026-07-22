package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatInputBar(
    onSendText: (String) -> Unit,
    onStartAudioRecord: () -> Unit,
    onStopAudioRecord: (shouldSend: Boolean) -> Unit,
    onStartVideoRecord: () -> Unit,
    onStopVideoRecord: (shouldSend: Boolean) -> Unit,
    onMoreClick: () -> Unit,
    onTyping: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    var inputMode by remember { mutableStateOf(ChatInputMode.AUDIO) }
    var isRecording by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    // Логика печатания
    LaunchedEffect(textState) {
        if (textState.isNotEmpty()) {
            onTyping(true)
            delay(3000)
            onTyping(false)
        } else {
            onTyping(false)
        }
    }

    val buttonSizeAnimation by animateDpAsState(
        targetValue = if (isRecording || isLocked) 68.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ButtonSizeAnimation"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMoreClick) {
            Icon(Icons.Default.MoreVert, contentDescription = "Дополнительно")
        }

        OutlinedTextField(
            value = textState,
            onValueChange = { textState = it },
            placeholder = { Text("Сообщение") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            shape = RoundedCornerShape(24.dp),
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(buttonSizeAnimation)
                    .clip(CircleShape)
                    .background(
                        if (isRecording || isLocked) MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (textState.trim().isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSendText(textState)
                            textState = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить текст",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(inputMode, isLocked) {
                                if (isLocked) {
                                    detectTapGestures {
                                        if (inputMode == ChatInputMode.AUDIO) {
                                            onStopAudioRecord(true)
                                        } else {
                                            onStopVideoRecord(true)
                                        }
                                        isLocked = false
                                    }
                                } else {
                                    coroutineScope {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitFirstDown()
                                                var isLongClick = false
                                                
                                                val recordingJob = launch {
                                                    delay(300)
                                                    isLongClick = true
                                                    isRecording = true
                                                    if (inputMode == ChatInputMode.AUDIO) {
                                                        onStartAudioRecord()
                                                    } else {
                                                        onStartVideoRecord()
                                                    }
                                                }
                                                
                                                val up = waitForUpOrCancellation()
                                                recordingJob.cancel()
                                                
                                                if (isLongClick) {
                                                    if (!isLocked) {
                                                        isRecording = false
                                                        if (inputMode == ChatInputMode.AUDIO) {
                                                            onStopAudioRecord(true)
                                                        } else {
                                                            onStopVideoRecord(true)
                                                        }
                                                    }
                                                } else if (up != null) {
                                                    inputMode = if (inputMode == ChatInputMode.AUDIO) {
                                                        ChatInputMode.VIDEO
                                                    } else {
                                                        ChatInputMode.AUDIO
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .pointerInput(inputMode, isRecording) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { _, dragAmount ->
                                        if (isRecording && dragAmount.y < -15f) {
                                            isLocked = true
                                            isRecording = false
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = if (isLocked) "locked" else inputMode.name,
                            label = "MediaIconAnimation"
                        ) { state ->
                            Icon(
                                imageVector = when (state) {
                                    "locked" -> Icons.Default.ArrowUpward
                                    ChatInputMode.AUDIO.name -> Icons.Default.Mic
                                    else -> Icons.Default.Videocam
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
