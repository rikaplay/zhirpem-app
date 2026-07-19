package com.RIKAPLAY.zhirpem_app

import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VoiceMessageBubble(audioUrl: String, isMyMessage: Boolean) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioUrl))
            prepare()
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        duration = duration
                    }
                }
            })
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration
            delay(100)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .width(230.dp)
                .clip(RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = if (isMyMessage) 20.dp else 4.dp,
                    bottomEnd = if (isMyMessage) 4.dp else 20.dp
                ))
                .background(if (isMyMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Кнопка Play/Pause
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isMyMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Имитация аудиодорожки
                    AudioWaveform(
                        progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Таймер
                    Text(
                        text = formatTime(if (isPlaying) currentPosition else duration),
                        fontSize = 11.sp,
                        color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWaveform(progress: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
        val barCount = 35
        val barSpacing = 4.dp.toPx()
        val barWidth = (size.width - (barCount - 1) * barSpacing) / barCount
        
        // Фиксированные высоты для "волны"
        val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.6f, 0.8f, 0.4f, 0.5f, 0.7f, 0.3f, 0.9f, 0.6f, 0.4f, 0.8f, 0.5f, 0.7f, 0.4f, 0.6f, 0.9f, 0.3f, 0.5f, 0.8f, 0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.6f, 0.8f, 0.4f, 0.5f, 0.7f, 0.3f, 0.9f)

        for (i in 0 until barCount) {
            val barHeight = size.height * heights[i % heights.size]
            val x = i * (barWidth + barSpacing)
            val isPlayed = (i.toFloat() / barCount) < progress
            
            drawRoundRect(
                color = if (isPlayed) color else color.copy(alpha = 0.3f),
                topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - barHeight) / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
