package com.RIKAPLAY.zhirpem_app.webrtc

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.RIKAPLAY.zhirpem_app.bounceClick
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun CallScreen(
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    peerName: String,
    peerAvatarUrl: String?,
    isAudioEnabled: Boolean,
    isVideoEnabled: Boolean,
    isRemoteSpeaking: Boolean = false,
    isLocalSpeaking: Boolean = false,
    isFrontCamera: Boolean = true,
    isSpeakerphoneEnabled: Boolean = true,
    connectionState: String,
    ping: Int = 0,
    endReason: String? = null,
    onInitLocal: (SurfaceViewRenderer) -> Unit,
    onInitRemote: (SurfaceViewRenderer) -> Unit,
    onToggleAudio: () -> Unit,
    onToggleVideo: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHangup: () -> Unit,
    onMinimize: () -> Unit
) {
    var isMoreInfoVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val hasEarpiece = remember {
        val packageManager = context.packageManager
        packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- REMOTE VIDEO / AVATAR LAYER ---
        if (isVideoEnabled && remoteVideoTrack != null) {
            AndroidView(
                factory = { context -> SurfaceViewRenderer(context).also { onInitRemote(it) } },
                modifier = Modifier.fillMaxSize(),
                update = { view -> remoteVideoTrack.addSink(view) }
            )
        } else {
            // Background Gradient for Avatar
            CallAvatarBackground(avatarUrl = peerAvatarUrl, isSpeaking = isRemoteSpeaking)
            
            // Central Avatar
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect if speaking
                if (isRemoteSpeaking) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    )
                }

                AsyncImage(
                    model = peerAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isRemoteSpeaking) 4.dp else 2.dp,
                            color = if (isRemoteSpeaking) MaterialTheme.colorScheme.primary else Color.White.copy(0.3f),
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // --- LOCAL PREVIEW ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .size(width = 120.dp, height = 180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.DarkGray)
                .border(2.dp, Color.White.copy(0.2f), RoundedCornerShape(20.dp))
        ) {
            if (isVideoEnabled) {
                AndroidView(
                    factory = { context -> SurfaceViewRenderer(context).also { onInitLocal(it) } },
                    modifier = Modifier.fillMaxSize(),
                    update = { view -> 
                        localVideoTrack?.addSink(view)
                        view.setMirror(true)
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Small local avatar if video off
                    val myAvatar = LocalContext.current.getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("avatarUrl", "")
                    AsyncImage(
                        model = myAvatar,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(CircleShape).border(1.dp, Color.White.copy(0.5f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Minimize Button
        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 40.dp, start = 16.dp)
                .background(Color.Black.copy(0.4f), CircleShape)
                .bounceClick()
        ) {
            Icon(Icons.Default.FullscreenExit, null, tint = Color.White)
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusColor = when (connectionState.uppercase()) {
                "CONNECTED", "COMPLETED" -> Color.Green
                "CHECKING" -> Color.Yellow
                "FAILED", "DISCONNECTED" -> Color.Red
                else -> Color.White
            }
            Surface(color = Color.Black.copy(0.5f), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = connectionState, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isMoreInfoVisible) {
                Surface(color = Color.Black.copy(0.7f), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(0.8f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Status: $connectionState", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        if (ping > 0) {
                            Text("Ping: ${ping}ms", color = if (ping < 100) Color.Green else if (ping < 200) Color.Yellow else Color.Red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text("720p HD | Opus Audio", color = Color.White.copy(0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            TextButton(onClick = { isMoreInfoVisible = !isMoreInfoVisible }) {
                Text(if (isMoreInfoVisible) "Меньше" else "Больше", color = Color.White.copy(0.6f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                CallControlButton(if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff, if (isAudioEnabled) Color.White.copy(0.2f) else Color.Red, onToggleAudio)
                
                CallControlButton(Icons.Default.FlipCameraAndroid, Color.White.copy(0.2f), onFlipCamera)

                CallControlButton(Icons.Default.CallEnd, Color.Red, onHangup, 64.dp, 32.dp)

                if (hasEarpiece) {
                    CallControlButton(
                        if (isSpeakerphoneEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Smartphone,
                        Color.White.copy(0.2f),
                        onToggleSpeaker
                    )
                }

                CallControlButton(if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, if (isVideoEnabled) Color.White.copy(0.2f) else Color.Red, onToggleVideo)
            }
        }

        if (endReason != null) {
            CallEndOverlay(reason = endReason)
        }
    }
}

@Composable
fun CallPipOverlay(
    remoteVideoTrack: VideoTrack?,
    onInitRemote: (SurfaceViewRenderer) -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .size(width = 110.dp, height = 160.dp)
            .bounceClick()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clickable { onClick() }
            .border(2.dp, Color.White.copy(0.3f), RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { context -> SurfaceViewRenderer(context).also { onInitRemote(it) } },
            modifier = Modifier.fillMaxSize(),
            update = { view -> remoteVideoTrack?.addSink(view) }
        )
    }
}

@Composable
fun CallEndOverlay(reason: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (reason == "Disconnected") Icons.Default.SignalWifiOff else Icons.Default.CallEnd,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (reason == "Call ended") "Звонок окончен" else if (reason == "Disconnected") "Disconnected" else reason,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CallAvatarBackground(avatarUrl: String?, isSpeaking: Boolean) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!isSpeaking) {
            val infiniteTransition = rememberInfiniteTransition()
            val phase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(15000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            // Using the avatar itself very blurred to get "accent colors"
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = phase; scaleX = 2f; scaleY = 2f }
                        .blur(100.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
            } else {
                // Fallback gradient if no avatar
                val colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.sweepGradient(
                                colors = colors + colors.first()
                            )
                        )
                        .graphicsLayer { rotationZ = phase }
                        .blur(80.dp)
                )
            }
        }
    }
}

@Composable
fun IncomingCallOverlay(peerName: String, peerAvatarUrl: String?, onAccept: () -> Unit, onReject: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.5f) + fadeIn(animationSpec = tween(600)),
                exit = scaleOut(targetScale = 1.5f) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
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
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically { it } + fadeIn(animationSpec = tween(800))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(peerName, fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    Text("Входящий вызов...", color = Color.White.copy(0.6f), fontSize = 18.sp, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(100.dp))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically { it / 2 } + fadeIn(animationSpec = tween(1000))
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallControlButton(
                        icon = Icons.Default.CallEnd,
                        backgroundColor = Color.Red,
                        onClick = onReject,
                        buttonSize = 80.dp,
                        iconSize = 36.dp
                    )
                    CallControlButton(
                        icon = Icons.Default.Call,
                        backgroundColor = Color.Green,
                        onClick = onAccept,
                        buttonSize = 80.dp,
                        iconSize = 36.dp
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(icon: ImageVector, backgroundColor: Color, onClick: () -> Unit, buttonSize: androidx.compose.ui.unit.Dp = 56.dp, iconSize: androidx.compose.ui.unit.Dp = 24.dp) {
    IconButton(
        onClick = onClick, 
        modifier = Modifier
            .size(buttonSize)
            .bounceClick()
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(icon, null, modifier = Modifier.size(iconSize), tint = Color.White)
    }
}
