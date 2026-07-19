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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
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
import com.google.firebase.storage.FirebaseStorage
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
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
import com.RIKAPLAY.zhirpem_app.ui.theme.Zhirpem_appTheme

// ==========================================
// 1. ГЛАВНАЯ АКТИВНОСТЬ
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
            val settingsManager = remember { SettingsManager(context) }
            
            // Состояние: включены ли стандартные анимации (инвертируем значение режима экономии)
            val animationsEnabled = remember { mutableStateOf(!settingsManager.isLowPerformanceMode) }
            val fontSizeMultiplier = remember { mutableStateOf(settingsManager.fontSizeMultiplier) }
            val isGlassEnabled = remember { mutableStateOf(settingsManager.isGlassEnabled) }
            val glassAlpha = remember { mutableStateOf(settingsManager.glassAlpha) }

            // Загружаем сохраненную тему (по умолчанию системная)
            var savedTheme by remember {
                mutableStateOf(AppThemeMode.valueOf(sharedPrefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"))
            }

            CompositionLocalProvider(
                LocalAnimationsEnabled provides animationsEnabled.value,
                LocalFontSize provides fontSizeMultiplier.value,
                LocalGlassEnabled provides isGlassEnabled.value,
                LocalGlassAlpha provides glassAlpha.value
            ) {
                Zhirpem_appTheme(themeMode = savedTheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val sharedPrefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(username).update("isOnline", isOnline)
        }
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
    var currentProfileUser by remember { mutableStateOf<String?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isBookmarksOpen by remember { mutableStateOf(false) }
    var isCommunitiesOpen by remember { mutableStateOf(false) }
    var isStatisticsOpen by remember { mutableStateOf(false) }
    var isOptimizationOpen by remember { mutableStateOf(false) }
    var isSecuritySettingsOpen by remember { mutableStateOf(false) }
    var showBackupWarning by remember { mutableStateOf(false) }
    var activeCommunityId by remember { mutableStateOf<String?>(null) }
    var globalChatId by remember { mutableStateOf<String?>(null) }
    var globalSearchQuery by remember { mutableStateOf<String?>(null) }
    var isCheckingSession by remember { mutableStateOf(true) }

    val myUsername = sharedPrefs.getString("username", "anonymous") ?: "anonymous"

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
            FirebaseFirestore.getInstance().collection("users").document(myUsername)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val hasBackupCode = snapshot.contains("backupCode")
                        showBackupWarning = !hasBackupCode && !sharedPrefs.getBoolean("backup_warning_dismissed", false)
                    }
                }
        }
    }

    // Имитация быстрой загрузки для плавности
    LaunchedEffect(Unit) {
        delay(300L) // Небольшая пауза, чтобы интерфейс не дергался
        isCheckingSession = false
    }

    if (isCheckingSession) {
        // Экран-заглушка при старте
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    isOptimizationOpen -> "optimization" to null
                    isBookmarksOpen -> "bookmarks" to null
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
                            BackHandler { isSecuritySettingsOpen = false }
                            SecuritySettingsScreen(onBack = { isSecuritySettingsOpen = false })
                        }
                        "optimization" -> {
                            BackHandler { isOptimizationOpen = false }
                            OptimizationScreen(onBack = { isOptimizationOpen = false })
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
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(onDismiss = { showForgotPasswordDialog = false })
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
                        sharedPrefs.edit()
                            .putBoolean("is_logged_in", true)
                            .putString("username", cleanUsername)
                            .putString("name", doc.getString("name"))
                            .apply()
                        onAuthSuccess()
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
                TextButton(onClick = { showForgotPasswordDialog = true }) {
                    Text("Забыли пароль?")
                }
            }
        }
    }
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
                                if (doc.exists() && doc.getString("backupCode") == backupCode) {
                                    step = 2
                                    errorMessage = ""
                                } else {
                                    errorMessage = "Неверные данные"
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
    externalChatId: String? = null,
    onExternalChatOpened: () -> Unit = {},
    initialSearchQuery: String? = null,
    onLogout: () -> Unit,
    showBackupWarning: Boolean,
    onDismissBackupWarning: () -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "anonymous") ?: "anonymous"

    // Загружаем данные профиля в реальном времени
    var currentName by remember { mutableStateOf(sharedPrefs.getString("name", "Пользователь") ?: "Пользователь") }
    var currentAvatarUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(myUsername) {
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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(320.dp).fillMaxHeight(),
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
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
                    val menuItems = listOf("👤  Мой Профиль", "⚙️  Настройки", "🔖  Закладки", "👥  Сообщества", "📈  Статистика")
                    menuItems.forEach { item ->
                        Text(
                            text = item,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    if (item.contains("Профиль")) onNavigateToProfile(myUsername)
                                    if (item.contains("Настройки")) onNavigateToSettings()
                                    if (item.contains("Закладки")) onNavigateToBookmarks()
                                    if (item.contains("Сообщества")) onNavigateToCommunities()
                                    if (item.contains("Статистика")) onNavigateToStatistics()
                                }
                                .padding(vertical = 14.dp, horizontal = 12.dp)
                        )
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
                            onOpenCamera = { isCameraOpen = true }
                        )
                        "notifications" -> NotificationsScreen()
                        "chats_list" -> ChatsListScreen(onChatClick = { activeChatId = it })
                        else -> {
                            FeedScreen(
                                onUserClick = onNavigateToProfile,
                                onHashtagClick = { hashtag ->
                                    hashtagSearchQuery = hashtag
                                    isSearchOpen = true
                                },
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onAdminAccess = { isAdminOpen = true },
                                currentAvatarUrl = currentAvatarUrl,
                                currentName = currentName,
                                showBackupWarning = showBackupWarning,
                                onNavigateToSecurity = onNavigateToSecurity,
                                onDismissBackupWarning = onDismissBackupWarning
                            )
                        }
                    }
                }
            }

            // Капсула навигации на верхнем слое
            if (!isCameraOpen) {
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
                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )

                                if (showPollForm && pollData.question.isNotBlank() && pollData.options.isNotEmpty()) {
                                    newPost["poll"] = pollData
                                }

                                if (mediaUri == null) {
                                    // Обычная логика отправки без фото
                                    db.collection("zhirpem_posts").add(newPost)
                                        .addOnSuccessListener { 
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
        // 1. Fetch all posts to derive analytics
        db.collection("zhirpem_posts").get().addOnSuccessListener { snapshot ->
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
            db.collection("zhirpem_posts").whereEqualTo("handle", "@$myUsername").get().addOnSuccessListener { mySnap ->
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
        }

        // 2. Fetch communities
        db.collection("communities").get().addOnSuccessListener { snapshot ->
            popularCommunities = snapshot.documents.mapNotNull { it.toObject(Community::class.java)?.copy(id = it.id) }
        }

        // 3. Fetch comments
        db.collection("comments").get().addOnSuccessListener { snapshot ->
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
        
        val setting = userDoc.getString("notificationSetting") ?: "all"
        
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
                                        performSendNotification(db, senderId, senderName, senderAvatar, receiverId, type, text, postId, targetText)
                                    }
                                }
                        }
                    }
            }
            else -> performSendNotification(db, senderId, senderName, senderAvatar, receiverId, type, text, postId, targetText)
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
