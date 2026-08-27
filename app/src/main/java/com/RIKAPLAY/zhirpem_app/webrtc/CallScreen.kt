package com.RIKAPLAY.zhirpem_app.webrtc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun CallScreen(
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    onInitLocal: (SurfaceViewRenderer) -> Unit,
    onInitRemote: (SurfaceViewRenderer) -> Unit,
    onHangup: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Remote Video (Full Screen)
        AndroidView(
            factory = { context ->
                SurfaceViewRenderer(context).also { onInitRemote(it) }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                remoteVideoTrack?.addSink(view)
            }
        )

        // Local Video (Small Overlay)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(width = 120.dp, height = 180.dp)
        ) {
            AndroidView(
                factory = { context ->
                    SurfaceViewRenderer(context).also { onInitLocal(it) }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    localVideoTrack?.addSink(view)
                }
            )
        }

        // Call Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onHangup,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Hang Up", color = Color.White)
            }
        }
    }
}
