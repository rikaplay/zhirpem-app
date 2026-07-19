package com.RIKAPLAY.zhirpem_app

import android.annotation.SuppressLint
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// 1. Перечисление для режимов камеры
enum class CameraMode { PHOTO, VIDEO }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionWrapper(
    onPermissionGranted: @Composable () -> Unit,
    onClose: () -> Unit
) {
    // Создаем состояние для отслеживания разрешений на камеру и микрофон
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    )

    if (permissionsState.allPermissionsGranted) {
        // ЕСЛИ ВСЕ РАЗРЕШЕНИЯ ДАНЫ -> Показываем твой экран камеры с CameraX
        onPermissionGranted()
    } else {
        // ЕСЛИ РАЗРЕШЕНИЯ НЕ ДАНЫ -> Показываем интерфейс с запросом
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val textToShow = if (permissionsState.shouldShowRationale) {
                    "Приложению необходим доступ к камере и микрофону, чтобы вы могли делать снимки и записывать видео."
                } else {
                    "Для съемки фото и видео приложению требуется доступ к камере и микрофону."
                }
                
                Text(
                    text = textToShow, 
                    textAlign = TextAlign.Center, 
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = { 
                        permissionsState.launchMultiplePermissionRequest() 
                    }
                ) {
                    Text("Предоставить разрешения")
                }
            }
        }
        
        // Автоматически запрашиваем разрешения сразу при входе на экран
        LaunchedEffect(Unit) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }
}

@Composable
fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Ограничиваем минимальный зум на 1x, максимальный — на 5x
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    
                    // Смещение работает только если картинка приближена
                    if (scale > 1f) {
                        offsetX += pan.x * scale
                        offsetY += pan.y * scale
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Масштабируемое изображение",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("MissingPermission", "RestrictedApi")
@Composable
fun CameraScreen(
    onMediaSelected: (Uri, Boolean) -> Unit, // Возвращает Uri и флаг isVideo (true если видео)
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Состояния камеры
    var cameraMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    // Состояния таймера записи видео
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // CameraX компоненты
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var currentRecording: Recording? by remember { mutableStateOf(null) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    // Эффект счетчика времени (Таймер)
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    // Вспомогательная функция для красивого форматирования времени ЧЧ:ММ:СС
    fun formatRecordingTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
    }

    // Инициализация режимов Камеры (Перезапуск при смене режима)
    LaunchedEffect(cameraMode) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        if (cameraMode == CameraMode.PHOTO) {
            imageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (capturedUri == null) {
            // ==========================================
            // РЕЖИМ СЪЕМКИ (Видоискатель)
            // ==========================================
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // ВЕРХНЯЯ ПАНЕЛЬ И ПЛАШКА ТАЙМЕРА
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                // Если запись НЕ идет, показываем кнопку закрыть и вспышку
                if (!isRecording) {
                    IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                    }

                    if (cameraMode == CameraMode.PHOTO) {
                        IconButton(
                            onClick = {
                                flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Вспышка",
                                tint = if (flashMode == ImageCapture.FLASH_MODE_ON) Color.Yellow else Color.White
                            )
                        }
                    }
                } else {
                    // ПЛАШКА ТАЙМЕРА (Показывается сверху строго во время записи)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Red.copy(alpha = 0.85f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Маленькая мигающая точка
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Текст времени
                        Text(
                            text = formatRecordingTime(recordingSeconds),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // НИЖНЯЯ ПАНЕЛЬ С УПРАВЛЕНИЕМ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(bottom = 32.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // КНОПКА ЗАТВОРА С АНИМАЦИЕЙ ТРАНСФОРМАЦИИ ФОРМЫ
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Анимируем скругление углов: круг (от 32.dp до полностью круглого) или квадрат (8.dp)
                    val cornerRadius by animateDpAsState(
                        targetValue = if (isRecording) 8.dp else 40.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "buttonShape"
                    )
                    
                    // Анимируем размер внутренней иконки при переходе в режим "Стоп"
                    val buttonPadding by animateDpAsState(
                        targetValue = if (isRecording) 14.dp else 0.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "buttonPadding"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(buttonPadding)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(if (cameraMode == CameraMode.VIDEO) Color.Red else Color.White)
                            .clickable {
                                if (cameraMode == CameraMode.PHOTO) {
                                    // Логика фотографии
                                    val photoFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    imageCapture?.takePicture(
                                        outputOptions,
                                        cameraExecutor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                capturedUri = Uri.fromFile(photoFile)
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                exception.printStackTrace()
                                            }
                                        }
                                    )
                                } else {
                                    // Логика Видео
                                    if (isRecording) {
                                        currentRecording?.stop()
                                        isRecording = false
                                    } else {
                                        val videoFile = File(context.cacheDir, "${System.currentTimeMillis()}.mp4")
                                        val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                        isRecording = true
                                        val pendingRecording = videoCapture?.output
                                            ?.prepareRecording(context, outputOptions)

                                        // Проверяем разрешение перед включением аудио
                                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                            pendingRecording?.withAudioEnabled()
                                        }

                                        currentRecording = pendingRecording
                                            ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                                if (recordEvent is VideoRecordEvent.Finalize) {
                                                    if (!recordEvent.hasError()) {
                                                        capturedUri = Uri.fromFile(videoFile)
                                                    }
                                                    isRecording = false
                                                }
                                            }
                                    }
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Анимированное скрытие переключателя режимов во время записи
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (cameraMode == CameraMode.PHOTO) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { cameraMode = CameraMode.PHOTO }
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text("ФОТО", color = if (cameraMode == CameraMode.PHOTO) Color.White else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (cameraMode == CameraMode.VIDEO) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable { cameraMode = CameraMode.VIDEO }
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text("ВИДЕО", color = if (cameraMode == CameraMode.VIDEO) Color.White else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // РЕЖИМ ПРЕДПРОСМОТРА СНЯТОГО МЕДИА
            // ==========================================
            val isVideo = cameraMode == CameraMode.VIDEO

            if (isVideo) {
                VideoPlayer(videoUrl = capturedUri.toString(), modifier = Modifier.fillMaxSize())
            } else {
                ZoomableImage(imageUrl = capturedUri.toString(), modifier = Modifier.fillMaxSize())
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 32.dp, end = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { capturedUri = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Переснять")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Переснять")
                }

                Button(
                    onClick = { onMediaSelected(capturedUri!!, isVideo) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.8f))
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Готово")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать")
                }
            }
        }
    }
}
