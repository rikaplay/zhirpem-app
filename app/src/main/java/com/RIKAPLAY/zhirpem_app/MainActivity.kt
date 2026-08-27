package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import com.RIKAPLAY.zhirpem_app.BuildConfig
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.RIKAPLAY.zhirpem_app.ui.theme.Zhirpem_appTheme
import com.RIKAPLAY.zhirpem_app.webrtc.GlobalCallViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.RIKAPLAY.zhirpem_app.webrtc.CallScreen
import com.RIKAPLAY.zhirpem_app.webrtc.IncomingCallOverlay
import com.RIKAPLAY.zhirpem_app.webrtc.CallPipOverlay
import com.RIKAPLAY.zhirpem_app.webrtc.CallService

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Удаление почты у пользователя @misuvava
        FirebaseFirestore.getInstance().collection("users").document("misuvava")
            .update("email", com.google.firebase.firestore.FieldValue.delete())

        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
            val settingsManager = remember { SettingsManager(context) }
            
            // Состояния, которые должны реагировать на изменения в SharedPreferences (например, от EnergySaver)
            val animationsEnabled = remember { mutableStateOf(!settingsManager.isLowPerformanceMode) }
            val fontSizeMultiplier = remember { mutableStateOf(settingsManager.fontSizeMultiplier) }
            val isGlassEnabled = remember { mutableStateOf(settingsManager.isGlassEnabled) }
            val glassAlpha = remember { mutableStateOf(settingsManager.glassAlpha) }

            // Слушатель изменений настроек
            DisposableEffect(context) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    when (key) {
                        "low_perf_mode" -> animationsEnabled.value = !prefs.getBoolean(key, false)
                        "font_size_multiplier" -> fontSizeMultiplier.value = prefs.getFloat(key, 1.0f)
                        "glass_enabled" -> isGlassEnabled.value = prefs.getBoolean(key, true)
                        "glass_alpha" -> glassAlpha.value = prefs.getFloat(key, 0.4f)
                    }
                }
                val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            // Загружаем сохраненную тему (по умолчанию системная)
            var savedTheme by remember {
                mutableStateOf(AppThemeMode.valueOf(sharedPrefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"))
            }

            // Слушатель изменений сессии (для темы)
            DisposableEffect(context) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "app_theme") {
                        savedTheme = AppThemeMode.valueOf(prefs.getString(key, "SYSTEM") ?: "SYSTEM")
                    }
                }
                sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            CompositionLocalProvider(
                LocalAnimationsEnabled provides animationsEnabled.value,
                LocalFontSize provides fontSizeMultiplier.value,
                LocalGlassEnabled provides isGlassEnabled.value,
                LocalGlassAlpha provides glassAlpha.value
            ) {
                Zhirpem_appTheme(themeMode = savedTheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        NetworkStabilityWrapper {
                            AppNavigation(
                                currentTheme = savedTheme,
                                onThemeChange = { newTheme ->
                                    savedTheme = newTheme
                                    sharedPrefs.edit().putString("app_theme", newTheme.name).apply()
                                },
                                onPerformanceModeChanged = { isLowPerf ->
                                    settingsManager.isLowPerformanceMode = isLowPerf
                                    animationsEnabled.value = !isLowPerf
                                },
                                onFontSizeChanged = { newSize ->
                                    settingsManager.fontSizeMultiplier = newSize
                                    fontSizeMultiplier.value = newSize
                                },
                                onGlassModeChanged = { enabled ->
                                    settingsManager.isGlassEnabled = enabled
                                    isGlassEnabled.value = enabled
                                },
                                onGlassAlphaChanged = { alpha ->
                                    settingsManager.glassAlpha = alpha
                                    glassAlpha.value = alpha
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        updateOnlineStatus(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val sharedPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(username).update("isOnline", isOnline)

            // Realtime Database Presence
            val rtdb = FirebaseDatabase.getInstance()
            val statusRef = rtdb.getReference("status/$username")
            if (isOnline) {
                statusRef.setValue(mapOf(
                    "state" to "online",
                    "last_changed" to ServerValue.TIMESTAMP
                ))
                statusRef.onDisconnect().setValue(mapOf(
                    "state" to "offline",
                    "last_changed" to ServerValue.TIMESTAMP
                ))
            } else {
                statusRef.setValue(mapOf(
                    "state" to "offline",
                    "last_changed" to ServerValue.TIMESTAMP
                ))
            }
        }
    }

    fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Не удалось распознать биометрию")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Вход в Zhirpem")
            .setSubtitle("Используйте биометрию для входа")
            .setNegativeButtonText("Использовать пароль")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

// ==========================================
// 3. НАВИГАЦИЯ (КОНТРОЛЛЕР ЭКРАНОВ)
// ==========================================
@Composable
fun AppNavigation(
    currentTheme: AppThemeMode, 
    onThemeChange: (AppThemeMode) -> Unit,
    onPerformanceModeChanged: (Boolean) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onGlassModeChanged: (Boolean) -> Unit,
    onGlassAlphaChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }

    // Состояния авторизации и профиля
    var isLoggedIn by remember { mutableStateOf(sharedPrefs.getBoolean("is_logged_in", false)) }
    var isAppLocked by remember {
        mutableStateOf(sharedPrefs.getBoolean("use_biometric", false) && sharedPrefs.getBoolean("is_logged_in", false)) 
    }
    var showPasswordUnlockDialog by remember { mutableStateOf(false) }

    var currentProfileUser by remember { mutableStateOf<String?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isBookmarksOpen by remember { mutableStateOf(false) }
    var isCommunitiesOpen by remember { mutableStateOf(false) }
    var isStatisticsOpen by remember { mutableStateOf(false) }
    var isOptimizationOpen by remember { mutableStateOf(false) }
    var isEnergySaverOpen by remember { mutableStateOf(false) }
    var isSecuritySettingsOpen by remember { mutableStateOf(false) }
    var isNewsOpen by remember { mutableStateOf(false) }
    var showBackupWarning by remember { mutableStateOf(false) }
    var activeCommunityId by remember { mutableStateOf<String?>(null) }
    var globalChatId by remember { mutableStateOf<String?>(null) }
    var globalSearchQuery by remember { mutableStateOf<String?>(null) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var showSplash by remember { mutableStateOf(true) }
    var is2faEnabled by remember { mutableStateOf(true) } // По умолчанию считаем что включена, чтобы не мигало

    val settingsManager = remember { SettingsManager(context) }
    val myUsername = sharedPrefs.getString("username", "anonymous") ?: "anonymous"

    val globalCallViewModel: GlobalCallViewModel = viewModel()
    
    LaunchedEffect(myUsername) {
        if (myUsername != "anonymous") {
            globalCallViewModel.init(myUsername)
        }
    }

    val isCallActive by globalCallViewModel.isCallActive.collectAsState()
    val showIncomingOverlay by globalCallViewModel.showIncomingOverlay.collectAsState()
    val peerName by globalCallViewModel.peerName.collectAsState()
    val peerAvatarUrl by globalCallViewModel.peerAvatarUrl.collectAsState()
    val currentChatId by globalCallViewModel.currentChatId.collectAsState()
    val callDuration by globalCallViewModel.callDuration.collectAsState()
    val isMinimized by globalCallViewModel.isMinimized.collectAsState()
    val ping by globalCallViewModel.ping.collectAsState()
    val callEndReason by globalCallViewModel.callEndReason.collectAsState()
    val isRemoteSpeaking by globalCallViewModel.isRemoteSpeaking.collectAsState()
    val isLocalSpeaking by globalCallViewModel.isLocalSpeaking.collectAsState()

    // Запрос разрешения на уведомления для Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Можно логировать статус
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(isLoggedIn, myUsername) {
        if (isLoggedIn && myUsername != "anonymous") {
            // Идентифицируем пользователя в OneSignal при входе
            OneSignal.User.addAlias("external_id", myUsername)
            OneSignal.Notifications.requestPermission(true)

            FirebaseFirestore.getInstance().collection("users").document(myUsername)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val hasBackupCode = snapshot.contains("backupCode")
                        showBackupWarning = !hasBackupCode && !sharedPrefs.getBoolean("backup_warning_dismissed", false)
                        is2faEnabled = snapshot.getBoolean("is2faEnabled") ?: false

                        // Сохраняем статус галочки
                        sharedPrefs.edit()
                            .putBoolean("blueBadge", snapshot.getBoolean("blueBadge") ?: false)
                            .apply()
                    }
                }
        }
    }

    // Имитация быстрой загрузки для плавности
    LaunchedEffect(Unit) {
        delay(300L) // Небольшая пауза, чтобы интерфейс не дергался
        isCheckingSession = false
    }

    // Биометрическая проверка при входе
    LaunchedEffect(isAppLocked) {
        if (isAppLocked && context is MainActivity) {
            context.showBiometricPrompt(
                onSuccess = { isAppLocked = false },
                onError = { error ->
                    if (error.contains("negative button", ignoreCase = true) || 
                        error.contains("отмена", ignoreCase = true) || 
                        error.contains("cancel", ignoreCase = true)) {
                        showPasswordUnlockDialog = true
                    }
                }
            )
        }
    }

    if (showSplash) {
        val settingsManager = remember { SettingsManager(context) }
        SplashScreen(
            isEnabled = settingsManager.isSplashScreenEnabled,
            isPremium = sharedPrefs.getBoolean("blueBadge", false),
            onNavigateToMain = { showSplash = false }
        )
    } else if (isCheckingSession) {
        // Экран-заглушка при старте
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (isAppLocked) {
        // Экран блокировки
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Shield, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Приложение заблокировано", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { 
                    if (context is MainActivity) {
                        context.showBiometricPrompt(
                            onSuccess = { isAppLocked = false },
                            onError = { showPasswordUnlockDialog = true }
                        )
                    }
                }) {
                    Text("Разблокировать")
                }
            }
        }

        if (showPasswordUnlockDialog) {
            PasswordUnlockDialog(
                onSuccess = { isAppLocked = false },
                onDismiss = { showPasswordUnlockDialog = false }
            )
        }
    } else {
        val animationsEnabledGlobal = LocalAnimationsEnabled.current
        AnimatedContent(
            targetState = isLoggedIn,
            transitionSpec = { 
                if (animationsEnabledGlobal) {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "AuthTransition"
        ) { loggedIn ->
            if (!loggedIn) {
                AuthScreen(onAuthSuccess = { isLoggedIn = true })
            } else {
                // Определяем текущее состояние экрана для анимации переходов
                    val navigationState = when {
                        isSettingsOpen -> "settings" to null
                        isEnergySaverOpen -> "energy_saver" to null
                        isOptimizationOpen -> "optimization" to null
                        isBookmarksOpen -> "bookmarks" to null
                        isNewsOpen -> "news" to null
                        activeCommunityId != null -> "community_details" to activeCommunityId
                        isCommunitiesOpen -> "communities" to null
                        isStatisticsOpen -> "statistics" to null
                        isSecuritySettingsOpen -> "security_settings" to null
                        currentProfileUser != null -> "profile" to currentProfileUser
                        else -> "main" to null
                    }

                val animationsEnabled = LocalAnimationsEnabled.current

                AnimatedContent(
                    targetState = navigationState,
                    transitionSpec = {
                        if (animationsEnabled) {
                            (scaleIn(initialScale = 0.95f) + fadeIn(animationSpec = premiumSpring)) togetherWith 
                            (scaleOut(targetScale = 1.05f) + fadeOut(animationSpec = premiumSpring))
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "ScreenTransition"
                ) { stateData ->
                    val (state, id) = stateData
                    when (state) {
                        "settings" -> {
                            BackHandler { isSettingsOpen = false }
                            SettingsScreen(
                                onBack = { isSettingsOpen = false }, 
                                onLogout = {
                                    sharedPrefs.edit().clear().apply()
                                    isLoggedIn = false
                                    isSettingsOpen = false
                                },
                                onNavigateToSecuritySettings = {
                                    isSecuritySettingsOpen = true
                                    isSettingsOpen = false
                                },
                                onNavigateToOptimization = {
                                    isOptimizationOpen = true
                                    isSettingsOpen = false
                                },
                                onNavigateToEnergySaver = {
                                    isEnergySaverOpen = true
                                    isSettingsOpen = false
                                },
                                currentTheme = currentTheme,
                                onThemeChange = onThemeChange,
                                onPerformanceModeChanged = onPerformanceModeChanged,
                                onFontSizeChanged = onFontSizeChanged,
                                onGlassModeChanged = onGlassModeChanged,
                                onGlassAlphaChanged = onGlassAlphaChanged
                            )
                        }
                        "bookmarks" -> {
                            BackHandler { isBookmarksOpen = false }
                            BookmarksScreen(
                                onBack = { isBookmarksOpen = false }, 
                                onUserClick = { currentProfileUser = it },
                                onHashtagClick = { 
                                    globalSearchQuery = it
                                    isBookmarksOpen = false
                                }
                            )
                        }
                        "community_details" -> {
                            BackHandler { activeCommunityId = null }
                            CommunityDetailsScreen(
                                communityId = id!!, 
                                onBack = { activeCommunityId = null }, 
                                onUserClick = { currentProfileUser = it },
                                onHashtagClick = {
                                    globalSearchQuery = it
                                    activeCommunityId = null
                                }
                            )
                        }
                        "communities" -> {
                            BackHandler { isCommunitiesOpen = false }
                            CommunitiesScreen(onBack = { isCommunitiesOpen = false }, onCommunityClick = { activeCommunityId = it.id })
                        }
                        "statistics" -> {
                            BackHandler { isStatisticsOpen = false }
                            StatisticsScreenContainer(onBack = { isStatisticsOpen = false })
                        }
                        "security_settings" -> {
                            BackHandler { 
                                isSecuritySettingsOpen = false
                                isSettingsOpen = true
                            }
                            SecuritySettingsScreen(onBack = { 
                                isSecuritySettingsOpen = false
                                isSettingsOpen = true
                            })
                        }
                        "optimization" -> {
                            BackHandler { 
                                isOptimizationOpen = false
                                isSettingsOpen = true
                            }
                            OptimizationScreen(onBack = { 
                                isOptimizationOpen = false
                                isSettingsOpen = true
                            })
                        }
                        "energy_saver" -> {
                            BackHandler { 
                                isEnergySaverOpen = false
                                isSettingsOpen = true
                            }
                            EnergySaverScreen(onBack = { 
                                isEnergySaverOpen = false
                                isSettingsOpen = true
                            })
                        }
                        "news" -> {
                            BackHandler { isNewsOpen = false }
                            UpdateNewsScreen(onBack = { isNewsOpen = false })
                        }
                        "profile" -> {
                            BackHandler { currentProfileUser = null }
                            UserProfileScreen(
                                username = id!!, 
                                onBack = { currentProfileUser = null }, 
                                onNavigateToChat = { chatId ->
                                    globalChatId = chatId
                                    currentProfileUser = null
                                },
                                onHashtagClick = {
                                    globalSearchQuery = it
                                    currentProfileUser = null
                                },
                                onNavigateToProfile = { newUser ->
                                    currentProfileUser = newUser
                                }
                            )
                        }
                        else -> {
                            MainScreen(
                                onNavigateToProfile = { currentProfileUser = it.replace("@", "").trim() },
                                onNavigateToSettings = { isSettingsOpen = true },
                                onNavigateToBookmarks = { isBookmarksOpen = true },
                                onNavigateToCommunities = { isCommunitiesOpen = true },
                                onNavigateToStatistics = { isStatisticsOpen = true },
                                onShowWhatsNew = { isNewsOpen = true },
                                externalChatId = globalChatId,
                                onExternalChatOpened = { globalChatId = null },
                                initialSearchQuery = globalSearchQuery,
                                onLogout = {
                                    sharedPrefs.edit().clear().apply()
                                    isLoggedIn = false
                                },
                                showBackupWarning = showBackupWarning,
                                onDismissBackupWarning = {
                                    showBackupWarning = false
                                    sharedPrefs.edit().putBoolean("backup_warning_dismissed", true).apply()
                                },
                                onNavigateToSecurity = {
                                    isSecuritySettingsOpen = true
                                    showBackupWarning = false
                                },
                                is2faEnabled = is2faEnabled,
                                isCallActive = isCallActive,
                                callDuration = callDuration,
                                onCallClick = {
                                    currentChatId?.let { 
                                        globalChatId = it 
                                    }
                                }
                            )
                            // Сбрасываем запрос после того как MainScreen его подхватил
                            LaunchedEffect(globalSearchQuery) {
                                if (globalSearchQuery != null) {
                                    delay(100)
                                    globalSearchQuery = null
                                }
                            }
                        }
                    }
                }

                if (isCallActive || callEndReason != null) {
                    val callUIState = when {
                        showIncomingOverlay -> "incoming"
                        isMinimized && callEndReason == null -> "pip"
                        else -> "full"
                    }

                    AnimatedContent(
                        targetState = callUIState,
                        transitionSpec = {
                            if (animationsEnabled) {
                                (scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(400))) togetherWith 
                                (scaleOut(targetScale = 1.1f) + fadeOut(animationSpec = tween(400)))
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        label = "CallUITransition",
                        modifier = Modifier.fillMaxSize().zIndex(100f)
                    ) { state ->
                        when (state) {
                            "incoming" -> {
                                IncomingCallOverlay(
                                    peerName = peerName,
                                    peerAvatarUrl = peerAvatarUrl,
                                    onAccept = {
                                        CallService.start(context)
                                        globalCallViewModel.acceptCall()
                                    },
                                    onReject = {
                                        globalCallViewModel.rejectCall()
                                    }
                                )
                            }
                            "pip" -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                                    CallPipOverlay(
                                        remoteVideoTrack = globalCallViewModel.remoteVideoTrack.collectAsState().value,
                                        onInitRemote = { globalCallViewModel.initRemoteSurface(it) },
                                        onClick = { globalCallViewModel.toggleMinimize() }
                                    )
                                }
                            }
                            "full" -> {
                                val connectionState by globalCallViewModel.connectionState.collectAsState()
                                val isSpeakerphoneEnabled by globalCallViewModel.isSpeakerphoneEnabled.collectAsState()
                                val isFrontCamera by globalCallViewModel.isFrontCamera.collectAsState()
                                CallScreen(
                                    localVideoTrack = globalCallViewModel.localVideoTrack.collectAsState().value,
                                    remoteVideoTrack = globalCallViewModel.remoteVideoTrack.collectAsState().value,
                                    peerName = peerName,
                                    peerAvatarUrl = peerAvatarUrl,
                                    isAudioEnabled = globalCallViewModel.isAudioEnabled.collectAsState().value,
                                    isVideoEnabled = globalCallViewModel.isVideoEnabled.collectAsState().value,
                                    isRemoteSpeaking = isRemoteSpeaking,
                                    isLocalSpeaking = isLocalSpeaking,
                                    isFrontCamera = isFrontCamera,
                                    isSpeakerphoneEnabled = isSpeakerphoneEnabled,
                                    connectionState = connectionState.name,
                                    ping = ping,
                                    endReason = callEndReason,
                                    onInitLocal = { globalCallViewModel.initLocalSurface(it) },
                                    onInitRemote = { globalCallViewModel.initRemoteSurface(it) },
                                    onToggleAudio = { globalCallViewModel.toggleAudio() },
                                    onToggleVideo = { globalCallViewModel.toggleVideo() },
                                    onFlipCamera = { globalCallViewModel.flipCamera() },
                                    onToggleSpeaker = { globalCallViewModel.toggleSpeakerphone() },
                                    onHangup = {
                                        globalCallViewModel.hangup()
                                        CallService.stop(context)
                                    },
                                    onMinimize = { globalCallViewModel.toggleMinimize() }
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
// 4. ЭКРАН АВТОРИЗАЦИИ / РЕГИСТРАЦИИ
// ==========================================
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val focusManager = LocalFocusManager.current

    var isLoginTab by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var show2faDialog by remember { mutableStateOf(false) }
    var userTotpSecret by remember { mutableStateOf("") }
    var pendingUsername by remember { mutableStateOf("") }
    var pendingName by remember { mutableStateOf("") }
    var pendingBlueBadge by remember { mutableStateOf(false) }

    if (showForgotPasswordTotpDialog) {
        ForgotPasswordTotpDialog(onDismiss = { showForgotPasswordTotpDialog = false })
    }

    if (showForgotPasswordDialogGlobal) {
        ForgotPasswordDialog(onDismiss = { showForgotPasswordDialogGlobal = false })
    }

    if (show2faDialog) {
        TwoFactorAuthDialog(
            username = pendingUsername,
            onSuccess = {
                OneSignal.User.addAlias("external_id", pendingUsername)
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    OneSignal.Notifications.requestPermission(true)
                }
                sharedPrefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("username", pendingUsername)
                    .putString("name", pendingName)
                    .putBoolean("blueBadge", pendingBlueBadge)
                    .apply()
                onAuthSuccess()
            },
            onDismiss = { show2faDialog = false },
            secret = userTotpSecret
        )
    }

    fun processAuth() {
        focusManager.clearFocus()
        if (username.isEmpty() || password.isEmpty() || (!isLoginTab && name.isEmpty())) {
            errorMessage = "Пожалуйста, заполните все поля!"
            return
        }

        val cleanUsername = username.lowercase().trim().replace("@", "")
        isLoading = true
        errorMessage = ""

        if (isLoginTab) {
            // ЛОГИН
            db.collection("users").document(cleanUsername).get()
                .addOnSuccessListener { doc ->
                    isLoading = false
                    if (doc.exists() && doc.getString("password") == password) {
                        val is2faEnabled = doc.getBoolean("is2faEnabled") ?: false
                        val totpSecret = doc.getString("totpSecret") ?: ""

                        if (is2faEnabled && totpSecret.isNotEmpty()) {
                            userTotpSecret = totpSecret
                            pendingUsername = cleanUsername
                            pendingName = doc.getString("name") ?: ""
                            pendingBlueBadge = doc.getBoolean("blueBadge") ?: false
                            show2faDialog = true
                        } else {
                            val loggedUsername = cleanUsername
                            OneSignal.User.addAlias("external_id", loggedUsername)
                            // Запрос разрешения в фоне
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                OneSignal.Notifications.requestPermission(true)
                            }

                            sharedPrefs.edit()
                                .putBoolean("is_logged_in", true)
                                .putString("username", cleanUsername)
                                .putString("name", doc.getString("name"))
                                .putBoolean("blueBadge", doc.getBoolean("blueBadge") ?: false)
                                .apply()
                            onAuthSuccess()
                        }
                    } else {
                        errorMessage = "Неверный юзернейм или пароль!"
                    }
                }
                .addOnFailureListener {
                    isLoading = false
                    errorMessage = "Ошибка сети. Проверьте интернет."
                }
        } else {
            // РЕГИСТРАЦИЯ
            db.collection("users").document(cleanUsername).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        isLoading = false
                        errorMessage = "Этот юзернейм уже занят!"
                    } else {
                        val backupCode = (100000..999999).random().toString()
                        val newUser = hashMapOf(
                            "name" to name.trim(),
                            "username" to cleanUsername,
                            "password" to password,
                            "backupCode" to backupCode
                        )
                        db.collection("users").document(cleanUsername).set(newUser)
                            .addOnSuccessListener {
                                isLoading = false
                                sharedPrefs.edit()
                                    .putBoolean("is_logged_in", true)
                                    .putString("username", cleanUsername)
                                    .putString("name", name.trim())
                                    .apply()
                                onAuthSuccess()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = "Не удалось создать аккаунт."
                            }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Логотип (если нет картинки, не упадет, но лучше чтобы была)
        Image(
            painter = painterResource(id = R.drawable.jirpem_logo),
            contentDescription = "Логотип",
            modifier = Modifier.height(60.dp).padding(bottom = 16.dp)
        )

        Text(
            text = if (isLoginTab) "С возвращением!" else "Создать аккаунт",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(24.dp))

        val animationsEnabledAuth = LocalAnimationsEnabled.current
        AnimatedVisibility(
            visible = !isLoginTab,
            enter = if (animationsEnabledAuth) fadeIn() + expandVertically() else EnterTransition.None,
            exit = if (animationsEnabledAuth) fadeOut() + shrinkVertically() else ExitTransition.None
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Как вас зовут?") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true
            )
        }

        TextField(
            value = username,
            onValueChange = { username = it.replace(" ", "") },
            label = { Text("Юзернейм (без @)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { processAuth() }),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp), fontSize = 14.sp)
        }

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = { processAuth() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = if (isLoginTab) "Войти" else "Присоединиться", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                isLoginTab = !isLoginTab
                errorMessage = ""
            }) {
                Text(text = if (isLoginTab) "Создать новый аккаунт" else "Уже есть профиль? Войти")
            }
            if (isLoginTab) {
                var showResetOptions by remember { mutableStateOf(false) }
                
                Box {
                    TextButton(onClick = { showResetOptions = true }) {
                        Text("Забыли пароль?")
                    }
                    
                    DropdownMenu(
                        expanded = showResetOptions,
                        onDismissRequest = { showResetOptions = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сброс через Backup Code") },
                            onClick = {
                                showResetOptions = false
                                showForgotPasswordDialogGlobal = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Сброс через TOTP (2FA)") },
                            onClick = {
                                showResetOptions = false
                                // Открываем диалог сброса через TOTP
                                showForgotPasswordTotpDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

var showForgotPasswordTotpDialog by mutableStateOf(false)
var showForgotPasswordDialogGlobal by mutableStateOf(false)

@Composable
fun ForgotPasswordTotpDialog(onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var step by remember { mutableIntStateOf(1) }
    var username by remember { mutableStateOf("") }
    var totpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var userSecret by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Сброс через TOTP" else "Новый пароль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step == 1) {
                    TextField(
                        value = username,
                        onValueChange = { username = it.trim().lowercase().replace("@", "") },
                        label = { Text("Юзернейм") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = totpCode,
                        onValueChange = { if (it.length <= 6) totpCode = it },
                        label = { Text("Код из аутентификатора") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                } else {
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Новый пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Подтвердите пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (username.isEmpty() || totpCode.isEmpty()) {
                            errorMessage = "Заполните все поля"
                            return@Button
                        }
                        isLoading = true
                        db.collection("users").document(username).get()
                            .addOnSuccessListener { doc ->
                                isLoading = false
                                if (doc.exists()) {
                                    val secret = doc.getString("totpSecret") ?: ""
                                    if (secret.isNotEmpty() && TotpUtils.verifyTotp(secret, totpCode)) {
                                        userSecret = secret
                                        step = 2
                                        errorMessage = ""
                                    } else {
                                        errorMessage = "Неверный код или 2FA не настроена"
                                    }
                                } else {
                                    errorMessage = "Пользователь не найден"
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = "Ошибка сети"
                            }
                    } else {
                        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            errorMessage = "Заполните поля"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = "Пароли не совпадают"
                            return@Button
                        }
                        isLoading = true
                        db.collection("users").document(username).update("password", newPassword)
                            .addOnSuccessListener {
                                isLoading = false
                                onDismiss()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = "Ошибка при обновлении"
                            }
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (step == 1) "Проверить" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var step by remember { mutableIntStateOf(1) }
    var username by remember { mutableStateOf("") }
    var backupCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Восстановление пароля" else "Новый пароль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step == 1) {
                    TextField(
                        value = username,
                        onValueChange = { username = it.trim().lowercase().replace("@", "") },
                        label = { Text("Юзернейм") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = backupCode,
                        onValueChange = { backupCode = it.trim() },
                        label = { Text("Код восстановления") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Новый пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Подтвердите пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (username.isEmpty() || backupCode.isEmpty()) {
                            errorMessage = "Заполните все поля"
                            return@Button
                        }
                        isLoading = true
                        db.collection("users").document(username).get()
                            .addOnSuccessListener { doc ->
                                isLoading = false
                                if (doc.exists()) {
                                    val dbCode = doc.getString("backupCode")
                                    if (dbCode == null) {
                                        errorMessage = "Backup Code не настроен для этого аккаунта"
                                    } else if (dbCode == backupCode) {
                                        step = 2
                                        errorMessage = ""
                                    } else {
                                        errorMessage = "Неверный Backup Code"
                                    }
                                } else {
                                    errorMessage = "Пользователь не найден"
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = "Ошибка сети"
                            }
                    } else {
                        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            errorMessage = "Заполните поля"
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = "Пароли не совпадают"
                            return@Button
                        }
                        isLoading = true
                        db.collection("users").document(username).update("password", newPassword)
                            .addOnSuccessListener {
                                isLoading = false
                                onDismiss()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                errorMessage = "Ошибка при обновлении"
                            }
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (step == 1) "Проверить" else "Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun TwoFactorAuthDialog(
    username: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    secret: String
) {
    var code by remember { mutableStateOf("") }
    var backupCodeInput by remember { mutableStateOf("") }
    var isBackupMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoadingBackup by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBackupMode) "Вход по коду восстановления" else "Двухфакторная проверка") },
        text = {
            Column {
                if (!isBackupMode) {
                    Text(
                        "Введите 6-значный код из вашего приложения для аутентификации (например, Google Authenticator)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        label = { Text("Код подтверждения") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                } else {
                    Text(
                        "Введите ваш 6-значный Backup Code, полученный при регистрации или в настройках безопасности.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = backupCodeInput,
                        onValueChange = { if (it.length <= 6) backupCodeInput = it },
                        label = { Text("Backup Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
                
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { 
                        isBackupMode = !isBackupMode
                        errorMessage = ""
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isBackupMode) "Использовать TOTP код" else "Нет доступа к аутентификатору?")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isBackupMode) {
                        if (TotpUtils.verifyTotp(secret, code)) {
                            onSuccess()
                        } else {
                            errorMessage = "Неверный код подтверждения"
                        }
                    } else {
                        if (backupCodeInput.isEmpty()) return@Button
                        isLoadingBackup = true
                        db.collection("users").document(username).get()
                            .addOnSuccessListener { doc ->
                                isLoadingBackup = false
                                if (doc.exists() && doc.getString("backupCode") == backupCodeInput) {
                                    onSuccess()
                                } else {
                                    errorMessage = "Неверный Backup Code"
                                }
                            }
                            .addOnFailureListener {
                                isLoadingBackup = false
                                errorMessage = "Ошибка сети"
                            }
                    }
                },
                enabled = if (isBackupMode) backupCodeInput.length == 6 && !isLoadingBackup else code.length == 6
            ) {
                if (isLoadingBackup) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun PasswordUnlockDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""
    val db = FirebaseFirestore.getInstance()
    
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Введите пароль от аккаунта") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isEmpty()) return@Button
                    isLoading = true
                    db.collection("users").document(myUsername).get()
                        .addOnSuccessListener { doc ->
                            isLoading = false
                            if (doc.exists() && doc.getString("password") == password) {
                                onSuccess()
                            } else {
                                errorMessage = "Неверный пароль"
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            errorMessage = "Ошибка сети"
                        }
                },
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Войти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

// ==========================================
// 5. ГЛАВНЫЙ ЭКРАН (ЛЕНТА + МЕНЮ)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToCommunities: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onShowWhatsNew: () -> Unit,
    externalChatId: String? = null,
    onExternalChatOpened: () -> Unit = {},
    initialSearchQuery: String? = null,
    onLogout: () -> Unit,
    showBackupWarning: Boolean,
    onDismissBackupWarning: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    is2faEnabled: Boolean = true,
    isCallActive: Boolean = false,
    callDuration: Long = 0,
    onCallClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "anonymous") ?: "anonymous"

    // Загружаем данные профиля в реальном времени
    var currentName by remember { mutableStateOf(sharedPrefs.getString("name", "Пользователь") ?: "Пользователь") }
    var currentAvatarUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(myUsername) {
        FirebaseMessaging.getInstance().subscribeToTopic("new_posts")
        if (myUsername != "anonymous") {
            FirebaseFirestore.getInstance().collection("users").document(myUsername)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        currentName = snapshot.getString("name") ?: currentName
                        currentAvatarUrl = snapshot.getString("avatarUrl")
                    }
                }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Внутренние состояния вкладок для сохранения NavigationBar
    var isSearchOpen by remember { mutableStateOf(false) }
    var hashtagSearchQuery by remember { mutableStateOf("") }
    var isChatsListOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var isCameraOpen by remember { mutableStateOf(false) }

    // Обработка перехода в чат из внешних экранов (например, из профиля)
    LaunchedEffect(externalChatId) {
        if (externalChatId != null) {
            activeChatId = externalChatId
            isChatsListOpen = true
            isSearchOpen = false
            isNotificationsOpen = false
            onExternalChatOpened()
        }
    }

    // Обработка перехода в поиск по хэштегу
    LaunchedEffect(initialSearchQuery) {
        if (!initialSearchQuery.isNullOrEmpty()) {
            hashtagSearchQuery = initialSearchQuery
            isSearchOpen = true
            isChatsListOpen = false
            isNotificationsOpen = false
            activeChatId = null
        }
    }

    // Обработка кнопки "Назад" для вкладок
    BackHandler(enabled = isSearchOpen || isChatsListOpen || isNotificationsOpen || activeChatId != null || isCameraOpen) {
        when {
            isCameraOpen -> isCameraOpen = false
            activeChatId != null -> activeChatId = null
            isSearchOpen -> isSearchOpen = false
            isChatsListOpen -> isChatsListOpen = false
            isNotificationsOpen -> isNotificationsOpen = false
        }
    }

    var isComposePostOpen by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val isGlassEnabled = LocalGlassEnabled.current
            val glassAlpha = LocalGlassAlpha.current
            val isDark = isSystemInDarkTheme()

            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
                drawerShape = RoundedCornerShape(32.dp),
                drawerTonalElevation = 0.dp,
                windowInsets = WindowInsets(0)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Glassy Background Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .then(
                                if (isGlassEnabled) {
                                    Modifier
                                        .let {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                it.graphicsLayer {
                                                    renderEffect = android.graphics.RenderEffect
                                                        .createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.DECAL)
                                                        .asComposeRenderEffect()
                                                }
                                            } else {
                                                it.blur(20.dp)
                                            }
                                        }
                                        .background(
                                            if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = glassAlpha)
                                            else Color.White.copy(alpha = 0.7f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(32.dp)
                                        )
                                } else {
                                    Modifier.background(MaterialTheme.colorScheme.surface)
                                }
                            )
                    )

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // Шапка меню (Профиль)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    onNavigateToProfile(myUsername)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = currentAvatarUrl,
                                    contentDescription = "Аватар",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(currentName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(currentName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("@$myUsername", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Кнопки меню
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        val menuItems = listOf(
                            Triple("👤", "Мой Профиль", Icons.Default.Person),
                            Triple("⚙️", "Настройки", Icons.Default.Add), // Placeholder, I'll use logic
                            Triple("🔖", "Закладки", Icons.Default.Add),
                            Triple("👥", "Сообщества", Icons.Default.Add),
                            Triple("📈", "Статистика", Icons.Default.Add)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "👤  Мой Профиль" to { onNavigateToProfile(myUsername) },
                                "⚙️  Настройки" to { onNavigateToSettings() },
                                "🔖  Закладки" to { onNavigateToBookmarks() },
                                "👥  Сообщества" to { onNavigateToCommunities() },
                                "📈  Статистика" to { onNavigateToStatistics() }
                            ).forEach { (label, onClick) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            onClick()
                                        }
                                        .padding(vertical = 14.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        // Кнопка выхода
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Выйти из аккаунта", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val glassEnabled = LocalGlassEnabled.current
            val glassAlpha = LocalGlassAlpha.current

            var isAdminOpen by remember { mutableStateOf(false) }

            // Контент на первом слое
            Box(modifier = Modifier.fillMaxSize()) {
                val mainScreenState = when {
                    isAdminOpen -> "admin" to null
                    isSearchOpen -> "search" to null
                    activeChatId != null -> "chat" to activeChatId
                    isNotificationsOpen -> "notifications" to null
                    isChatsListOpen -> "chats_list" to null
                    else -> "media_feed" to null
                }

                val animationsEnabled = LocalAnimationsEnabled.current
                AnimatedContent(
                    targetState = mainScreenState,
                    transitionSpec = {
                        if (animationsEnabled) {
                            (scaleIn(initialScale = 0.98f) + fadeIn(animationSpec = premiumSpring)) togetherWith
                            (scaleOut(targetScale = 1.02f) + fadeOut(animationSpec = premiumSpring))
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "MainScreenContentTransition"
                ) { stateData ->
                    val (state, chatId) = stateData
                    when (state) {
                        "admin" -> AdminPanelScreen(onBack = { isAdminOpen = false })
                        "search" -> SearchScreen(
                            initialQuery = hashtagSearchQuery,
                            onNavigateToProfile = { uid ->
                                onNavigateToProfile(uid)
                                isSearchOpen = false
                            }
                        )
                        "chat" -> ChatScreen(
                            chatId = chatId!!, 
                            onBack = { activeChatId = null }, 
                            onNavigateToPost = { /* scrollToPost */ },
                            onNavigateToProfile = { onNavigateToProfile(it) },
                            onOpenCamera = { isCameraOpen = true }
                        )
                        "notifications" -> NotificationsScreen()
                        "chats_list" -> ChatsListScreen(onChatClick = { activeChatId = it })
                        else -> {
                            MainFeedScreen(
                                onUserClick = onNavigateToProfile,
                                onHashtagClick = { hashtag ->
                                    hashtagSearchQuery = hashtag
                                    isSearchOpen = true
                                },
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onAdminAccess = { isAdminOpen = true },
                                onShowWhatsNew = onShowWhatsNew,
                                currentAvatarUrl = currentAvatarUrl,
                                currentName = currentName,
                                showBackupWarning = showBackupWarning,
                                onNavigateToSecurity = onNavigateToSecurity,
                                onDismissBackupWarning = onDismissBackupWarning,
                                is2faEnabled = is2faEnabled,
                                isCallActive = isCallActive,
                                callDuration = callDuration,
                                onCallClick = onCallClick
                            )
                        }
                    }
                }
            }

            // Капсула навигации на верхнем слое
            if (!isCameraOpen && activeChatId == null) {
                val navItems = listOf("🏠" to "Главная", "🔍" to "Поиск", "🔔" to "Уведомления", "✉️" to "Сообщения")
                val selectedLabel = when {
                    isChatsListOpen -> "Сообщения"
                    isNotificationsOpen -> "Уведомления"
                    isSearchOpen -> "Поиск"
                    activeChatId == null -> "Главная"
                    else -> ""
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    FluidSwipeBottomBar(
                        isGlassEnabled = glassEnabled,
                        glassAlpha = glassAlpha,
                        items = navItems,
                        selectedLabel = selectedLabel,
                        onTabSelected = { label ->
                            when (label) {
                                "Главная" -> {
                                    isSearchOpen = false
                                    isChatsListOpen = false
                                    isNotificationsOpen = false
                                    activeChatId = null
                                }
                                "Поиск" -> {
                                    isSearchOpen = true
                                    hashtagSearchQuery = ""
                                    isChatsListOpen = false
                                    isNotificationsOpen = false
                                    activeChatId = null
                                }
                                "Уведомления" -> {
                                    isNotificationsOpen = true
                                    isSearchOpen = false
                                    isChatsListOpen = false
                                    activeChatId = null
                                }
                                "Сообщения" -> {
                                    isChatsListOpen = true
                                    isSearchOpen = false
                                    isNotificationsOpen = false
                                    activeChatId = null
                                }
                            }
                        }
                    )
                }
            }

            // FAB на верхнем слое
            if (!isSearchOpen && !isChatsListOpen && !isNotificationsOpen && activeChatId == null && !isAdminOpen) {
                GlassFloatingActionButton(
                    onClick = { isComposePostOpen = true },
                    isGlassEnabled = glassEnabled,
                    glassAlpha = glassAlpha,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 96.dp, end = 20.dp)
                )
            }

            // ЭКРАН КАМЕРЫ ДЛЯ ЧАТА
            if (isCameraOpen && activeChatId != null) {
                val db = FirebaseFirestore.getInstance()
                CameraPermissionWrapper(
                    onPermissionGranted = {
                        CameraScreen(
                            onMediaSelected = { uri, isVideo ->
                                uploadImageToCloudinary(
                                    context = context,
                                    imageUri = uri,
                                    mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                                    cloudName = "dcwp4nm3e",
                                    uploadPreset = "ProfilePIC",
                                    onSuccess = { url ->
                                        // sendMessage вызываем через прямое обращение
                                        sendMessage(
                                            db = db,
                                            chatId = activeChatId!!,
                                            senderId = myUsername,
                                            text = "",
                                            mediaUrl = url,
                                            mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                                        )
                                        isCameraOpen = false
                                    },
                                    onError = { isCameraOpen = false }
                                )
                            },
                            onClose = { isCameraOpen = false }
                        )
                    },
                    onClose = { isCameraOpen = false }
                )
            }
        }
    }

    // Диалог создания поста
    if (isComposePostOpen) {
        ComposePostDialog(
            name = currentName,
            username = myUsername,
            isMediaTabActive = false,
            onDismiss = { isComposePostOpen = false }
        )
    }
}

fun extractKeywords(text: String): List<String> {
    val hashtagRegex = Regex("#([a-zA-Z0-9_а-яА-Я]+)")
    val hashtags = hashtagRegex.findAll(text).map { it.groupValues[1].lowercase() }.toList()
    
    // Упрощенный список стоп-слов (можно расширить)
    val stopWords = setOf("и", "в", "на", "что", "как", "это", "по", "для", "но", "а", "ты", "мы", "вы", "они", "с", "у", "к", "из")
    
    val words = text.lowercase()
        .replace(Regex("[^a-zA-Z0-9а-яА-Я\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.length > 3 && it !in stopWords }
        .distinct()

    return (hashtags + words).distinct().take(15) // Берем топ-15 ключевых слов
}

// ==========================================
// 6. ОКНО СОЗДАНИЯ ПОСТА
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePostDialog(name: String, username: String, isMediaTabActive: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    var postText by remember { mutableStateOf("") }
    val db = remember { FirebaseFirestore.getInstance() }
    var isSending by remember { mutableStateOf(false) }
    val maxChars = 500

    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf(MediaType.NONE) }
    var showCamera by remember { mutableStateOf(false) }
    var myAvatarUrl by remember { mutableStateOf<String?>(null) }
    var myNameColor by remember { mutableStateOf<String?>(null) }
    var myBannedStatus by remember { mutableStateOf(false) }

    var showPollForm by remember { mutableStateOf(false) }
    var pollData by remember { mutableStateOf(PollData()) }
    
    // Получаем текущие данные пользователя
    LaunchedEffect(username) {
        db.collection("users").document(username).get().addOnSuccessListener { doc ->
            myAvatarUrl = doc.getString("avatarUrl")
            myNameColor = doc.getString("nameColor")
            myBannedStatus = doc.getBoolean("isBanned") ?: false
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            mediaUri = it
            selectedMediaType = MediaType.IMAGE
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            mediaUri = it
            selectedMediaType = MediaType.VIDEO
        }
    }

    val gifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            mediaUri = it
            selectedMediaType = MediaType.GIF
        }
    }

    if (showCamera) {
        CameraPermissionWrapper(
            onPermissionGranted = {
                CameraScreen(
                    onMediaSelected = { uri, isVideo ->
                        mediaUri = uri
                        selectedMediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                        showCamera = false
                    },
                    onClose = { showCamera = false }
                )
            },
            onClose = { showCamera = false }
        )
        return // Не рисуем диалог пока открыта камера
    }

    Dialog(onDismissRequest = { if(!isSending) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSending) {
                        Icon(Icons.Filled.Close, contentDescription = "Отмена", tint = MaterialTheme.colorScheme.onBackground)
                    }

                    Button(
                        onClick = {
                            if (postText.trim().isNotEmpty() || mediaUri != null) {
                                isSending = true
                                val tags = extractKeywords(postText.trim())
                                val newPost = hashMapOf(
                                    "author" to name,
                                    "handle" to "@$username",
                                    "text" to postText.trim(),
                                    "date" to SimpleDateFormat("d MMM", Locale("ru")).format(Date()),
                                    "time" to SimpleDateFormat("HH:mm", Locale("ru")).format(Date()),
                                    "likes" to 0,
                                    "views" to 0,
                                    "likedBy" to emptyList<String>(),
                                    "isMedia" to (mediaUri != null),
                                    "mediaUrl" to "",
                                    "mediaType" to selectedMediaType.name,
                                    "authorAvatarUrl" to myAvatarUrl, // Сохраняем аву автора
                                    "authorNameColor" to myNameColor, // Сохраняем цвет ника
                                    "isAuthorBanned" to myBannedStatus, // Сохраняем статус бана
                                    "authorStatus" to sharedPrefs.getString("status", ""), // Сохраняем статус
                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                    "tags" to tags
                                )

                                if (showPollForm && pollData.question.isNotBlank() && pollData.options.isNotEmpty()) {
                                    newPost["poll"] = pollData
                                }

                                if (mediaUri == null) {
                                    // Обычная логика отправки без фото
                                    db.collection("zhirpem_posts").add(newPost)
                                        .addOnSuccessListener { 
                                            val postId = it.id
                                            
                                            // 1. Отправляем пуш через OneSignal REST API прямо из приложения
                                            sendOneSignalNotification(
                                                appId = BuildConfig.ONESIGNAL_APP_ID,
                                                restKey = BuildConfig.ONESIGNAL_REST_KEY,
                                                authorName = name,
                                                text = postText.trim(),
                                                postId = postId
                                            )

                                            // 2. Устанавливаем алиас для идентификации пользователя
                                            OneSignal.User.addAlias("external_id", username)

                                            // 3. Устанавливаем тег, чтобы сработал Journey в OneSignal
                                            OneSignal.User.addTag("has_posted", "true")
                                            android.util.Log.d("OneSignalDebug", "Тег has_posted успешно установлен для $username")

                                            // 4. Сбрасываем тег через 2 секунды
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                OneSignal.User.removeTag("has_posted")
                                                android.util.Log.d("OneSignalDebug", "Тег has_posted удален")
                                            }, 2000)

                                            if (sharedPrefs.getBoolean("vibration_enabled", true)) {
                                                triggerPublishVibration(context)
                                            }
                                            isSending = false; onDismiss() 
                                        }
                                        .addOnFailureListener { isSending = false }
                                } else {
                                    // Загрузка в Cloudinary через REST API
                                    uploadImageToCloudinary(
                                        context = context,
                                        imageUri = mediaUri!!,
                                        mediaType = selectedMediaType,
                                        cloudName = "dcwp4nm3e",
                                        uploadPreset = "ProfilePIC", // Используем ваш пресет
                                        onSuccess = { url: String ->
                                            newPost["mediaUrl"] = url
                                            // Для обратной совместимости, если где-то еще используется imageUrl
                                            if (selectedMediaType == MediaType.IMAGE) {
                                                newPost["imageUrl"] = url
                                            }
                                            db.collection("zhirpem_posts").add(newPost)
                                                .addOnSuccessListener { 
                                                    val postId = it.id

                                                    // 1. Отправляем пуш через OneSignal REST API прямо из приложения
                                                    sendOneSignalNotification(
                                                        appId = BuildConfig.ONESIGNAL_APP_ID,
                                                        restKey = BuildConfig.ONESIGNAL_REST_KEY,
                                                        authorName = name,
                                                        text = postText.trim(),
                                                        postId = postId
                                                    )

                                                    // 2. Устанавливаем алиас для идентификации пользователя
                                                    OneSignal.User.addAlias("external_id", username)

                                                    // 3. Устанавливаем тег, чтобы сработал Journey в OneSignal
                                                    OneSignal.User.addTag("has_posted", "true")
                                                    android.util.Log.d("OneSignalDebug", "Тег has_posted успешно установлен для $username")

                                                    // 4. Сбрасываем тег через 2 секунды
                                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                        OneSignal.User.removeTag("has_posted")
                                                        android.util.Log.d("OneSignalDebug", "Тег has_posted удален")
                                                    }, 2000)

                                                    if (sharedPrefs.getBoolean("vibration_enabled", true)) {
                                                        triggerPublishVibration(context)
                                                    }
                                                    isSending = false; onDismiss() 
                                                }
                                                .addOnFailureListener { isSending = false }
                                        },
                                        onError = { isSending = false }
                                    )
                                }
                            }
                        },
                        enabled = if (isMediaTabActive) {
                            (mediaUri != null) && !isSending // На вкладке "Медиа" фото ОБЯЗАТЕЛЬНО
                        } else {
                            (postText.trim().isNotEmpty() || mediaUri != null) && !isSending // Обычный пост
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.bounceClick()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Опубликовать", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
                TextField(
                    value = postText,
                    onValueChange = { if (it.length <= maxChars) postText = it },
                    placeholder = { Text("Что у вас нового, $name?", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
                )

                if (showPollForm) {
                    CreatePollView(
                        pollData = pollData,
                        onPollDataChange = { pollData = it },
                        onClosePoll = {
                            showPollForm = false
                            pollData = PollData()
                        }
                    )
                }

                // Превью выбранного медиа и кнопки добавления
                val animationsEnabled = LocalAnimationsEnabled.current
                AnimatedVisibility(
                    visible = true,
                    enter = if (animationsEnabled) expandVertically() + fadeIn() else EnterTransition.None,
                    exit = if (animationsEnabled) shrinkVertically() + fadeOut() else ExitTransition.None
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Скрепка (Фото)
                        IconButton(onClick = { imageLauncher.launch("image/*") }, enabled = !isSending, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Выбрать фото", tint = MaterialTheme.colorScheme.primary)
                        }

                        // 2. Видеокамера (Видео)
                        IconButton(onClick = { videoLauncher.launch("video/*") }, enabled = !isSending, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Default.VideoCall, contentDescription = "Выбрать видео", tint = MaterialTheme.colorScheme.primary)
                        }

                        // 3. Значок GIF
                        IconButton(onClick = { gifLauncher.launch("image/gif") }, enabled = !isSending, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Default.Gif, contentDescription = "Выбрать GIF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }

                        // 4. Камера
                        IconButton(onClick = { showCamera = true }, enabled = !isSending, modifier = Modifier.bounceClick()) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Камера", tint = MaterialTheme.colorScheme.primary)
                        }

                        // 5. Опрос
                        IconButton(onClick = { showPollForm = !showPollForm }, enabled = !isSending, modifier = Modifier.bounceClick()) {
                            Icon(
                                Icons.Default.BarChart, 
                                contentDescription = "Опрос", 
                                tint = if (showPollForm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        mediaUri?.let { uri ->
                            Box {
                                if (selectedMediaType == MediaType.IMAGE || selectedMediaType == MediaType.GIF) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = "Превью",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (selectedMediaType == MediaType.VIDEO) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.VideoCall, contentDescription = null, tint = Color.White)
                                    }
                                }
                                // Кнопка удаления превью
                                IconButton(
                                    onClick = { 
                                        mediaUri = null
                                        selectedMediaType = MediaType.NONE
                                    },
                                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape).bounceClick()
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Удалить", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Счетчик символов
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "${postText.length} / $maxChars",
                        color = if (postText.length >= maxChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
// ==========================================
fun uploadImageToCloudinary(
    context: android.content.Context,
    imageUri: Uri,
    mediaType: MediaType = MediaType.IMAGE,
    cloudName: String,
    uploadPreset: String,
    onSuccess: (String) -> Unit,
    onError: () -> Unit
) {
    try {
        // 1. Создаем временный файл из URI
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val extension = when(mediaType) {
            MediaType.VIDEO -> "mp4"
            MediaType.GIF -> "gif"
            else -> "jpg"
        }
        val file = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.$extension")
        inputStream.use { input ->
            FileOutputStream(file).use { output -> input?.copyTo(output) }
        }

        // 2. Отправляем запрос на Cloudinary
        val client = OkHttpClient()

        val mimeType = when(mediaType) {
            MediaType.VIDEO -> "video/mp4"
            MediaType.GIF -> "image/gif"
            else -> "image/jpeg"
        }

        val resourceType = if (mediaType == MediaType.VIDEO) "video" else "image"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaType()))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload")
            .post(requestBody)
            .build()

        // 3. Обрабатываем ответ
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                e.printStackTrace()
                onError()
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val json = JSONObject(responseData ?: "{}")
                    val url = json.getString("secure_url")
                    onSuccess(url)
                } else {
                    onError()
                }
            }
        })
    } catch (e: Exception) {
        e.printStackTrace()
        onError()
    }
}

@Composable
fun ZhirpemLogo(onAdminAccess: () -> Unit) {
    // Объявляем переменные ОДИН РАЗ
    var clickCount by remember { mutableIntStateOf(0) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    // 1. Всплывающее окно
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Админ-доступ 🛡️") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль разработчика") },
                    // Скрываем символы пароля
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (password == "RIK@_PLAY51") {
                        showPasswordDialog = false
                        password = "" // Очищаем поле после успеха
                        onAdminAccess() // Вызываем функцию перехода
                    } else {
                        password = "" // Очищаем при неверном пароле
                    }
                }) { Text("Войти") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    password = ""
                }) { Text("Отмена") }
            }
        )
    }

    // 2. Логотип
    Image(
        painter = painterResource(id = R.drawable.jirpem_logo),
        contentDescription = "Лого",
        modifier = Modifier
            .height(30.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                clickCount++
                if (clickCount >= 15) {
                    showPasswordDialog = true
                    clickCount = 0 // Сброс счетчика
                }
            },
        contentScale = ContentScale.Fit
    )
}

fun triggerPublishVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(70)
    }
}

@Composable
fun StatisticsScreenContainer(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""
    val db = FirebaseFirestore.getInstance()

    var myPostsAnalytics by remember { mutableStateOf(listOf<PostAnalytics>()) }
    var allPostsAnalytics by remember { mutableStateOf(listOf<PostAnalytics>()) }
    var popularCommunities by remember { mutableStateOf(listOf<Community>()) }
    var bestComments by remember { mutableStateOf(listOf<CommentAnalytics>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 1. Fetch posts to derive analytics (Limited for performance)
        db.collection("zhirpem_posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().addOnSuccessListener { snapshot ->
            val allPosts = snapshot.documents.mapNotNull { doc ->
                val p = doc.toObject(Post::class.java)
                p?.let {
                    PostAnalytics(
                        postId = doc.id,
                        titleOrText = it.text,
                        views = it.views,
                        likes = it.likes,
                        reposts = it.repostedBy.size,
                        commentsCount = 0,
                        timestamp = it.timestamp?.seconds?.times(1000) ?: 0L
                    )
                }
            }
            allPostsAnalytics = allPosts

            // Refine myPosts
            db.collection("zhirpem_posts")
                .whereEqualTo("handle", "@$myUsername")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(30)
                .get().addOnSuccessListener { mySnap ->
                myPostsAnalytics = mySnap.documents.mapNotNull { doc ->
                    val p = doc.toObject(Post::class.java)
                    p?.let {
                        PostAnalytics(
                            postId = doc.id,
                            titleOrText = it.text,
                            views = it.views,
                            likes = it.likes,
                            reposts = it.repostedBy.size,
                            commentsCount = 0,
                            timestamp = it.timestamp?.seconds?.times(1000) ?: 0L
                        )
                    }
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
        }
        .addOnFailureListener { isLoading = false }

        // 2. Fetch communities (Limited)
        db.collection("communities").limit(20).get().addOnSuccessListener { snapshot ->
            popularCommunities = snapshot.documents.mapNotNull { it.toObject(Community::class.java)?.copy(id = it.id) }
        }

        // 3. Fetch comments (Limited)
        db.collection("comments").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().addOnSuccessListener { snapshot ->
            bestComments = snapshot.documents.mapNotNull { doc ->
                val c = doc.toObject(Comment::class.java)
                c?.let {
                    CommentAnalytics(
                        commentId = doc.id,
                        postId = it.postId,
                        authorName = it.author,
                        commentText = it.text,
                        likes = it.likesCount
                    )
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        StatisticsScreen(
            myPostsAnalytics = myPostsAnalytics,
            allJirpemPosts = allPostsAnalytics,
            popularCommunities = popularCommunities,
            bestComments = bestComments,
            onBack = onBack
        )
    }
}

fun sendNotification(
    db: FirebaseFirestore,
    senderId: String,
    senderName: String,
    senderAvatar: String,
    receiverId: String,
    type: String,
    text: String = "",
    postId: String? = null,
    targetText: String = ""
) {
    if (senderId == receiverId || receiverId.isEmpty()) return

    db.collection("users").document(receiverId).get().addOnSuccessListener { userDoc ->
        if (!userDoc.exists()) return@addOnSuccessListener

        val isOnlyVerified = userDoc.getBoolean("isOnlyVerifiedMessages") ?: false
        val setting = userDoc.getString("notificationSetting") ?: "all"

        fun checkBadgesAndSend() {
            if (isOnlyVerified) {
                db.collection("users").document(senderId).get().addOnSuccessListener { senderDoc ->
                    val isVerified = senderDoc.getBoolean("blueBadge") == true || senderDoc.getBoolean("yellowBadge") == true
                    if (isVerified) {
                        performSendNotification(db, senderId, senderName, senderAvatar, receiverId, type, text, postId, targetText)
                    }
                }
            } else {
                performSendNotification(db, senderId, senderName, senderAvatar, receiverId, type, text, postId, targetText)
            }
        }

        when (setting) {
            "none" -> return@addOnSuccessListener
            "following" -> {
                // Вложенная проверка взаимной подписки
                db.collection("follows")
                    .whereEqualTo("follower", receiverId)
                    .whereEqualTo("following", senderId)
                    .get()
                    .addOnSuccessListener { snapshot1 ->
                        if (!snapshot1.isEmpty) {
                            db.collection("follows")
                                .whereEqualTo("follower", senderId)
                                .whereEqualTo("following", receiverId)
                                .get()
                                .addOnSuccessListener { snapshot2 ->
                                    if (!snapshot2.isEmpty) {
                                        checkBadgesAndSend()
                                    }
                                }
                        }
                    }
            }
            else -> checkBadgesAndSend()
        }
    }
}

private fun performSendNotification(
    db: FirebaseFirestore,
    senderId: String,
    senderName: String,
    senderAvatar: String,
    receiverId: String,
    type: String,
    text: String,
    postId: String?,
    targetText: String
) {
    val notification = hashMapOf(
        "senderId" to senderId,
        "senderName" to senderName,
        "senderAvatarUrl" to senderAvatar,
        "receiverId" to receiverId,
        "type" to type,
        "text" to text,
        "targetText" to targetText,
        "postId" to postId,
        "timestamp" to FieldValue.serverTimestamp()
    )
    db.collection("notifications").add(notification)
}

fun sendOneSignalNotification(
    appId: String,
    restKey: String,
    authorName: String,
    text: String,
    postId: String
) {
    val client = OkHttpClient()

    val json = JSONObject()
    json.put("app_id", appId)

    val segments = JSONArray()
    segments.put("Subscribed Users")
    json.put("included_segments", segments)

    val headings = JSONObject()
    headings.put("en", "Новый пост от $authorName")
    headings.put("ru", "Новый пост от $authorName")
    json.put("headings", headings)

    val contents = JSONObject()
    contents.put("en", text)
    contents.put("ru", text)
    json.put("contents", contents)

    val data = JSONObject()
    data.put("postId", postId)
    data.put("type", "NEW_POST")
    json.put("data", data)

    json.put("android_channel_id", "zhirpem_notifications")

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = json.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url("https://onesignal.com/api/v1/notifications")
        .post(body)
        .addHeader("Authorization", "Basic $restKey")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            android.util.Log.e("OneSignalREST", "Ошибка отправки пуша: ${e.message}")
        }
        override fun onResponse(call: Call, response: Response) {
            val responseData = response.body?.string()
            if (response.isSuccessful) {
                android.util.Log.d("OneSignalREST", "Пуш успешно отправлен: $responseData")
            } else {
                android.util.Log.e("OneSignalREST", "Ошибка API OneSignal ($response): $responseData")
            }
        }
    })
}

fun sendOneSignalEmail(
    appId: String,
    restKey: String,
    email: String,
    code: String
) {
    val client = OkHttpClient()
    
    val json = JSONObject()
    json.put("app_id", appId)
    
    val emailTo = JSONArray()
    emailTo.put(email)
    json.put("email_to", emailTo)
    
    // Помечаем как транзакционное, чтобы обходить проверку на подписку
    json.put("include_unsubscribed", true)
    
    json.put("email_subject", "Ваш код подтверждения Zhirpem")
    
    // HTML body for better appearance
    val emailBody = """
        <div style="font-family: sans-serif; padding: 20px; text-align: center; background-color: #f4f4f4;">
            <div style="background-color: #ffffff; padding: 30px; border-radius: 16px; display: inline-block; box-shadow: 0 4px 10px rgba(0,0,0,0.1);">
                <h2 style="color: #6200EE;">Подтверждение входа</h2>
                <p style="font-size: 16px; color: #333;">Ваш одноразовый код для входа в Zhirpem:</p>
                <div style="font-size: 32px; font-weight: bold; color: #6200EE; margin: 20px 0; letter-spacing: 5px;">$code</div>
                <p style="font-size: 12px; color: #888;">Если вы не запрашивали этот код, просто проигнорируйте это письмо.</p>
            </div>
        </div>
    """.trimIndent()
    
    json.put("email_body", emailBody)

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = json.toString().toRequestBody(mediaType)
    
    val request = Request.Builder()
        .url("https://onesignal.com/api/v1/notifications")
        .post(body)
        .addHeader("Authorization", "Basic $restKey")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            android.util.Log.e("OneSignalEmail", "Ошибка отправки email: ${e.message}")
        }
        override fun onResponse(call: Call, response: Response) {
            val responseData = response.body?.string()
            if (response.isSuccessful) {
                android.util.Log.d("OneSignalEmail", "Email успешно отправлен: $responseData")
            } else {
                android.util.Log.e("OneSignalEmail", "Ошибка API OneSignal Email ($response): $responseData")
            }
        }
    })
}
