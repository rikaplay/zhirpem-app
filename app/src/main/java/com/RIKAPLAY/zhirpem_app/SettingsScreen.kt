package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSecuritySettings: () -> Unit,
    onNavigateToOptimization: () -> Unit,
    onNavigateToEnergySaver: () -> Unit,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onPerformanceModeChanged: (Boolean) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onGlassModeChanged: (Boolean) -> Unit,
    onGlassAlphaChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val db = FirebaseFirestore.getInstance()
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val settingsManager = remember { SettingsManager(context) }
    val themeManager = remember { ThemeManager(context) }
    val updater = remember { GitHubUpdater(context) }
    val coroutineScope = rememberCoroutineScope()
    var myUsername by remember { mutableStateOf(sharedPrefs.getString("username", "") ?: "") }

    // Состояния для обновления
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf("") }

    // Состояния данных пользователя
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Локальные настройки
    var isVibrationEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("vibration_enabled", true))
    }
    var isLowPerf by remember { mutableStateOf(settingsManager.isLowPerformanceMode) }
    var isSplashEnabled by remember { mutableStateOf(settingsManager.isSplashScreenEnabled) }
    var isSplashSoundEnabled by remember { mutableStateOf(settingsManager.isSplashSoundEnabled) }
    var isGlassEnabled by remember { mutableStateOf(settingsManager.isGlassEnabled) }
    var glassAlpha by remember { mutableStateOf(settingsManager.glassAlpha) }
    var fontSizeMultiplier by remember { mutableStateOf(settingsManager.fontSizeMultiplier) }
    
    // Новые настройки темы
    var customThemeType by remember { mutableStateOf(themeManager.themeType) }
    var selectedColorHex by remember { mutableStateOf(themeManager.customColor) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val pastelPresets = listOf(
        "#B2F2BB" to "Мята",
        "#E5DBFF" to "Лаванда",
        "#FFD8A8" to "Персик",
        "#A5D8FF" to "Небесный",
        "#FFD1DC" to "Сакура"
    )
    
    val notificationOptions = listOf("Все", "Читаемые", "Никто")
    val notificationValues = listOf("all", "following", "none")
    var selectedNotificationIndex by remember {
        mutableIntStateOf(0)
    }
    var notificationSetting by remember { mutableStateOf("all") }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf(myUsername) }
    var newDisplayName by remember { mutableStateOf(name) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var showIconDialog by remember { mutableStateOf(false) }
    
    // --- НОВЫЕ СОСТОЯНИЯ ---
    var showChatCallSettings by remember { mutableStateOf(false) }
    var isOnlyVerifiedMessages by remember { mutableStateOf(false) }
    var selectedRingtone by remember {
        mutableIntStateOf(sharedPrefs.getInt("outgoing_call_ringtone", 0))
    }
    var showRingtoneDialog by remember { mutableStateOf(false) }
    var showGlobalChatConfigDialog by remember { mutableStateOf(false) }
    
    // --- ПРИВАТНОСТЬ ---
    var privacyLastSeen by remember { mutableStateOf("all") }
    var privacyPhoto by remember { mutableStateOf("all") }
    var hideFollows by remember { mutableStateOf(false) }
    var readReceipts by remember { mutableStateOf(sharedPrefs.getBoolean("read_receipts", true)) }
    var useBiometric by remember { mutableStateOf(sharedPrefs.getBoolean("use_biometric", false)) }
    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showPhotoPrivacyDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showFAQDialog by remember { mutableStateOf(false) }

    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            db.collection("users").document(myUsername).get().addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                notificationSetting = doc.getString("notificationSetting") ?: "all"
                selectedNotificationIndex = notificationValues.indexOf(notificationSetting).coerceAtLeast(0)
                isOnlyVerifiedMessages = doc.getBoolean("isOnlyVerifiedMessages") ?: false
                
                // Загрузка настроек приватности
                privacyLastSeen = doc.getString("privacyLastSeen") ?: "all"
                privacyPhoto = doc.getString("privacyPhoto") ?: "all"
                hideFollows = doc.getBoolean("hideFollows") ?: false
                
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val glassEnabledGlobal = LocalGlassEnabled.current
            GlassTopBar(
                isGlassEnabled = glassEnabledGlobal,
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- АККАУНТ И ПРОФИЛЬ ---
                Text("Аккаунт", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                SettingsGroup {
                    SettingsItem(
                        title = "Имя",
                        trailingText = name,
                        icon = Icons.Default.Person,
                        iconBackgroundColor = Color(0xFF007AFF), // Blue
                        onClick = { 
                            newDisplayName = name
                            showNameDialog = true 
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Юзернейм",
                        trailingText = "@$myUsername",
                        icon = Icons.Default.AlternateEmail,
                        iconBackgroundColor = Color(0xFF32ADE6), // Cyan
                        onClick = { 
                            newUsername = myUsername
                            usernameError = null
                            showUsernameDialog = true 
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Безопасность",
                        subtitle = "Код восстановления доступа",
                        icon = Icons.Default.Shield,
                        iconBackgroundColor = Color(0xFF8E8E93), // Grey
                        onClick = onNavigateToSecuritySettings
                    )
                }

                // --- КОНФИДЕНЦИАЛЬНОСТЬ ---
                Text("Конфиденциальность", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
                SettingsGroup {
                    SettingsItem(
                        title = "Последняя активность",
                        subtitle = "Кто видит, когда вы были в сети",
                        trailingText = when(privacyLastSeen) {
                            "all" -> "Все"
                            "friends" -> "Друзья"
                            else -> "Никто"
                        },
                        icon = Icons.Default.Visibility,
                        iconBackgroundColor = Color(0xFF34C759),
                        onClick = { showLastSeenDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Фото профиля",
                        trailingText = when(privacyPhoto) {
                            "all" -> "Все"
                            "friends" -> "Друзья"
                            else -> "Никто"
                        },
                        icon = Icons.Default.AccountCircle,
                        iconBackgroundColor = Color(0xFF007AFF),
                        onClick = { showPhotoPrivacyDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsSwitchItem(
                        title = "Скрыть подписки",
                        subtitle = "Не показывать список ваших читателей",
                        icon = Icons.Default.People,
                        iconBackgroundColor = Color(0xFF5856D6),
                        checked = hideFollows,
                        onCheckedChange = { 
                            hideFollows = it
                            db.collection("users").document(myUsername).update("hideFollows", it)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsSwitchItem(
                        title = "Отчеты о прочтении",
                        subtitle = "Видно ли, что вы прочитали сообщение",
                        icon = Icons.Default.DoneAll,
                        iconBackgroundColor = Color(0xFF5856D6),
                        checked = readReceipts,
                        onCheckedChange = { 
                            readReceipts = it
                            sharedPrefs.edit().putBoolean("read_receipts", it).apply()
                            db.collection("users").document(myUsername).update("readReceipts", it)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsSwitchItem(
                        title = "Защита приложения",
                        subtitle = "Вход по отпечатку/FaceID",
                        icon = Icons.Default.Fingerprint,
                        iconBackgroundColor = Color.Black,
                        checked = useBiometric,
                        onCheckedChange = { 
                            useBiometric = it
                            sharedPrefs.edit().putBoolean("use_biometric", it).apply()
                        }
                    )
                }

                // --- ВНЕШНИЙ ВИД ---
                Text("Внешний вид", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                SettingsGroup {
                    SettingsItem(
                        title = "Тема оформления",
                        trailingText = when(currentTheme) {
                            AppThemeMode.LIGHT -> "Светлая"
                            AppThemeMode.DARK -> "Темная"
                            AppThemeMode.AMOLED -> "AMOLED"
                            AppThemeMode.SYSTEM -> "Системная"
                            AppThemeMode.MATERIAL_YOU_LIGHT -> "M. You Light"
                            AppThemeMode.MATERIAL_YOU_DARK -> "M. You Dark"
                        },
                        icon = Icons.Default.Palette,
                        iconBackgroundColor = Color(0xFFAF52DE), // Purple
                        onClick = { showThemeDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Иконка приложения",
                        icon = Icons.Default.Apps,
                        iconBackgroundColor = Color(0xFF34C759), // Green
                        onClick = { showIconDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    
                    // Персонализация
                    SettingsSwitchItem(
                        title = "Своя палитра",
                        icon = Icons.Default.ColorLens,
                        iconBackgroundColor = Color(0xFFFF9500), // Orange
                        checked = customThemeType != ThemeManager.TYPE_DEFAULT,
                        onCheckedChange = { 
                            if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (it) {
                                customThemeType = ThemeManager.TYPE_MY_DARK
                                themeManager.themeType = ThemeManager.TYPE_MY_DARK
                            } else {
                                customThemeType = ThemeManager.TYPE_DEFAULT
                                themeManager.themeType = ThemeManager.TYPE_DEFAULT
                            }
                        }
                    )
                    
                    if (customThemeType != ThemeManager.TYPE_DEFAULT) {
                        Column(modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 12.dp)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    onClick = { 
                                        customThemeType = ThemeManager.TYPE_MY_LIGHT
                                        themeManager.themeType = ThemeManager.TYPE_MY_LIGHT
                                    },
                                    selected = customThemeType == ThemeManager.TYPE_MY_LIGHT
                                ) {
                                    Text("Светлая", fontSize = 12.sp)
                                }
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    onClick = { 
                                        customThemeType = ThemeManager.TYPE_MY_DARK
                                        themeManager.themeType = ThemeManager.TYPE_MY_DARK
                                    },
                                    selected = customThemeType == ThemeManager.TYPE_MY_DARK
                                ) {
                                    Text("Темная", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                pastelPresets.forEach { (hex, name) ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(16.dp))
                                            .border(
                                                width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedColorHex = hex
                                                themeManager.customColor = hex
                                            }
                                    )
                                }
                                
                                IconButton(
                                    onClick = { showColorPickerDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                ) {
                                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    
                    SettingsSwitchItem(
                        title = "Эффект стекла",
                        icon = Icons.Default.BlurOn,
                        iconBackgroundColor = Color(0xFF5AC8FA), // Light Blue
                        checked = isGlassEnabled,
                        onCheckedChange = { newValue ->
                            if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isGlassEnabled = newValue
                            onGlassModeChanged(newValue)
                        }
                    )
                    
                    if (isGlassEnabled) {
                        Column(modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 12.dp)) {
                            Slider(
                                value = glassAlpha,
                                onValueChange = { 
                                    if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    glassAlpha = it
                                    onGlassAlphaChanged(it)
                                },
                                valueRange = 0.1f..0.9f
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    
                    SettingsSwitchItem(
                        title = "Низкая производительность",
                        subtitle = "Упрощенные анимации",
                        icon = Icons.Default.Speed,
                        iconBackgroundColor = Color(0xFF5856D6), // Indigo
                        checked = isLowPerf,
                        onCheckedChange = { newValue ->
                            if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLowPerf = newValue
                            onPerformanceModeChanged(newValue)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8E8E93)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TextFormat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Размер шрифта", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text("${(fontSizeMultiplier * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = fontSizeMultiplier,
                            onValueChange = { 
                                if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                fontSizeMultiplier = it
                                onFontSizeChanged(it)
                            },
                            valueRange = 0.8f..1.5f,
                            steps = 6
                        )
                    }
                }

                // --- ЗАПУСК ---
                Text("Запуск", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                SettingsGroup {
                    SettingsSwitchItem(
                        title = "Splash Screen",
                        subtitle = "Логотип при старте",
                        icon = Icons.Default.RocketLaunch,
                        iconBackgroundColor = Color(0xFFFF2D55), // Pink
                        checked = isSplashEnabled,
                        onCheckedChange = { newValue ->
                            if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSplashEnabled = newValue
                            settingsManager.isSplashScreenEnabled = newValue
                        }
                    )
                    
                    if (isSplashEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsSwitchItem(
                            title = "Звук запуска",
                            icon = Icons.Default.VolumeUp,
                            iconBackgroundColor = Color(0xFFFFCC00), // Yellow
                            checked = isSplashSoundEnabled,
                            onCheckedChange = { newValue ->
                                if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSplashSoundEnabled = newValue
                                settingsManager.isSplashSoundEnabled = newValue
                            }
                        )
                    }
                }

                // --- ЧАТЫ И ЗВОНКИ ---
                Text("Чаты и Звонки", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                SettingsGroup {
                    SettingsItem(
                        title = "Оформление чатов",
                        subtitle = "Темы и обои для всех",
                        icon = Icons.Default.ChatBubble,
                        iconBackgroundColor = Color(0xFF5856D6), // Indigo
                        onClick = { showGlobalChatConfigDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsSwitchItem(
                        title = "Только проверенные",
                        subtitle = "Сообщения только от верифицированных",
                        icon = Icons.Default.VerifiedUser,
                        iconBackgroundColor = Color(0xFF34C759), // Green
                        checked = isOnlyVerifiedMessages,
                        onCheckedChange = { newValue ->
                            if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isOnlyVerifiedMessages = newValue
                            db.collection("users").document(myUsername).update("isOnlyVerifiedMessages", newValue)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Мелодия вызова",
                        trailingText = if (selectedRingtone == 0) "Tune 1" else "Tune 2",
                        icon = Icons.Default.MusicNote,
                        iconBackgroundColor = Color(0xFFFF9500), // Orange
                        onClick = { showRingtoneDialog = true }
                    )
                }

                // --- СИСТЕМА ---
                Text("Система", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                SettingsGroup {
                    SettingsItem(
                        title = "Уведомления",
                        trailingText = when(selectedNotificationIndex) {
                            0 -> "Все"
                            1 -> "Читаемые"
                            else -> "Никто"
                        },
                        icon = Icons.Default.Notifications,
                        iconBackgroundColor = Color(0xFFFF3B30), // Red
                        onClick = { 
                            // Можно открыть диалог или оставить логику ниже
                        }
                    )
                    
                    // Раскрывающийся список уведомлений
                    Column(modifier = Modifier.padding(start = 64.dp, end = 16.dp, bottom = 8.dp)) {
                        notificationOptions.forEachIndexed { index, option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        if (isVibrationEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedNotificationIndex = index
                                        val newVal = notificationValues[index]
                                        db.collection("users").document(myUsername).update("notificationSetting", newVal)
                                        sharedPrefs.edit().putInt("notification_mode", index).apply()
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedNotificationIndex == index,
                                    onClick = { 
                                        selectedNotificationIndex = index 
                                        val newVal = notificationValues[index]
                                        db.collection("users").document(myUsername).update("notificationSetting", newVal)
                                        sharedPrefs.edit().putInt("notification_mode", index).apply()
                                    }
                                )
                                Text(text = option, fontSize = 14.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    
                    SettingsSwitchItem(
                        title = "Вибрация",
                        icon = Icons.Default.Vibration,
                        iconBackgroundColor = Color(0xFF34C759), // Green
                        checked = isVibrationEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isVibrationEnabled = it
                            sharedPrefs.edit().putBoolean("vibration_enabled", it).apply()
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    SettingsItem(
                        title = "Оптимизация",
                        subtitle = "Очистка кэша",
                        icon = Icons.Default.DeleteSweep,
                        iconBackgroundColor = Color(0xFF5AC8FA), // Blue
                        onClick = onNavigateToOptimization
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    SettingsItem(
                        title = "Энергосбережение",
                        icon = Icons.Default.BatteryChargingFull,
                        iconBackgroundColor = Color(0xFF4CD964), // Bright Green
                        onClick = onNavigateToEnergySaver
                    )
                }

                // --- ПОДДЕРЖКА И О ПРИЛОЖЕНИИ ---
                Text("Поддержка и О приложении", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
                SettingsGroup {
                    SettingsItem(
                        title = "Написать в поддержку",
                        subtitle = "Связь с разработчиками",
                        icon = Icons.Default.SupportAgent,
                        iconBackgroundColor = Color(0xFF007AFF),
                        onClick = { showSupportDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "FAQ / База знаний",
                        subtitle = "Ответы на вопросы",
                        icon = Icons.Default.Info,
                        iconBackgroundColor = Color(0xFF5856D6),
                        onClick = { showFAQDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Обновления",
                        subtitle = if (isCheckingUpdate) "Поиск..." else "Проверить версию",
                        icon = Icons.Default.SystemUpdate,
                        iconBackgroundColor = Color(0xFF007AFF),
                        onClick = {
                            if (!isCheckingUpdate) {
                                coroutineScope.launch {
                                    isCheckingUpdate = true
                                    val url = updater.checkForUpdates()
                                    isCheckingUpdate = false
                                    if (url != null) {
                                        updateUrl = url
                                        showUpdateDialog = true
                                    } else {
                                        android.widget.Toast.makeText(context, "У вас последняя версия", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    SettingsItem(
                        title = "Пригласить друзей",
                        subtitle = "Поделиться ссылкой на приложение",
                        icon = Icons.Default.Share,
                        iconBackgroundColor = Color(0xFF34C759),
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Приложение Zhirpem")
                                putExtra(Intent.EXTRA_TEXT, "Привет! Скачай классное приложение Zhirpem по ссылке: https://github.com/rikaplay/zhirpem-app/releases/latest")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Поделиться"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Выйти из аккаунта", fontWeight = FontWeight.Bold)
                }

                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    "1.0.0"
                }

                Text(
                    text = "Версия: $versionName",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
                )
            }
        }
    }

    // Диалог выбора темы
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Выберите тему") },
            text = {
                Column {
                    val themes = listOf(
                        AppThemeMode.LIGHT to "☀️ Светлая (Зеленая)",
                        AppThemeMode.DARK to "🌙 Тёмная (Зеленая)",
                        AppThemeMode.AMOLED to "⬛ AMOLED (Черная)",
                        AppThemeMode.SYSTEM to "🤖 Как в системе",
                        AppThemeMode.MATERIAL_YOU_LIGHT to "🎨 Material You (Светлая)",
                        AppThemeMode.MATERIAL_YOU_DARK to "🎨 Material You (Тёмная)"
                    )
                    themes.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onThemeChange(mode)
                                    showThemeDialog = false 
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentTheme == mode, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Закрыть") }
            }
        )
    }

    // Диалог выбора иконки
    if (showIconDialog) {
        AlertDialog(
            onDismissRequest = { showIconDialog = false },
            title = { Text("Иконка приложения") },
            text = {
                Column {
                    Text("Выберите стиль значка для рабочего стола:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { 
                                    changeAppIcon(context, "MainActivity")
                                    sharedPrefs.edit().putString("app_icon", "MainActivity").apply()
                                    showIconDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Оригинал")
                            }
                            OutlinedButton(
                                onClick = { 
                                    changeAppIcon(context, "MainActivityAlias1")
                                    sharedPrefs.edit().putString("app_icon", "MainActivityAlias1").apply()
                                    showIconDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Жирпем 1")
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { 
                                    changeAppIcon(context, "MainActivityAlias2")
                                    sharedPrefs.edit().putString("app_icon", "MainActivityAlias2").apply()
                                    showIconDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Жирпем 2")
                            }
                            OutlinedButton(
                                onClick = { 
                                    changeAppIcon(context, "MainActivityAlias3")
                                    sharedPrefs.edit().putString("app_icon", "MainActivityAlias3").apply()
                                    showIconDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Жирпем 3")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог изменения юзернейма
    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Изменить юзернейм") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { 
                            newUsername = it
                            usernameError = null
                        },
                        label = { Text("Новый юзернейм") },
                        prefix = { Text("@") },
                        isError = usernameError != null,
                        supportingText = { if (usernameError != null) Text(usernameError!!) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername == myUsername) {
                            showUsernameDialog = false
                            return@Button
                        }
                        if (newUsername.isBlank()) {
                            usernameError = "Введите юзернейм"
                            return@Button
                        }

                        db.collection("users")
                            .whereEqualTo("username", newUsername)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                if (!snapshot.isEmpty) {
                                    usernameError = "Этот юзернейм уже занят!"
                                } else {
                                    db.collection("users").document(myUsername).update("username", newUsername)
                                        .addOnSuccessListener {
                                            sharedPrefs.edit().putString("username", newUsername).apply()
                                            myUsername = newUsername
                                            showUsernameDialog = false
                                            android.widget.Toast.makeText(context, "Юзернейм изменен!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            usernameError = "Ошибка при обновлении"
                                        }
                                }
                            }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог изменения имени
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Отображаемое имя") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Ваше имя") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDisplayName.isNotBlank()) {
                            db.collection("users").document(myUsername).update("name", newDisplayName.trim())
                                .addOnSuccessListener {
                                    sharedPrefs.edit().putString("name", newDisplayName.trim()).apply()
                                    name = newDisplayName.trim()
                                    showNameDialog = false
                                    android.widget.Toast.makeText(context, "Имя обновлено!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог кастомного цвета
    if (showColorPickerDialog) {
        var customHex by remember { mutableStateOf(selectedColorHex) }
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Кастомный цвет (HEX)") },
            text = {
                OutlinedTextField(
                    value = customHex,
                    onValueChange = { customHex = it.uppercase() },
                    placeholder = { Text("#FFFFFF") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    try {
                        android.graphics.Color.parseColor(customHex)
                        selectedColorHex = customHex
                        themeManager.customColor = customHex
                        showColorPickerDialog = false
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Неверный формат HEX", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Применить") }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог обновления
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Доступно обновление") },
            text = { Text("Найдена новая версия приложения. Хотите обновить?") },
            confirmButton = {
                Button(onClick = {
                    showUpdateDialog = false
                    updateUrl?.let { updater.downloadAndInstall(it) }
                }) {
                    Text("Обновить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // --- ДИАЛОГ ВЫБОРА РИНГТОНА ---
    if (showRingtoneDialog) {
        var ringtonePlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
        
        fun stopAndReleasePlayer() {
            try {
                ringtonePlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ringtonePlayer = null
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                stopAndReleasePlayer()
            }
        }

        AlertDialog(
            onDismissRequest = { 
                stopAndReleasePlayer()
                showRingtoneDialog = false 
            },
            title = { Text("Мелодия исходящего вызова") },
            text = {
                Column {
                    val ringtones = listOf(
                        "Zhirpem tune 1" to R.raw.zhirpem_tune_1,
                        "Zhirpem tune 2" to R.raw.zhirpem_tune_2
                    )
                    ringtones.forEachIndexed { index, (name, resId) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedRingtone = index
                                    sharedPrefs.edit().putInt("outgoing_call_ringtone", index).apply()
                                    
                                    // Проигрываем превью
                                    stopAndReleasePlayer()
                                    try {
                                        val player = android.media.MediaPlayer.create(context, resId)
                                        ringtonePlayer = player
                                        player?.start()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedRingtone == index, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    stopAndReleasePlayer()
                    showRingtoneDialog = false 
                }) { Text("Готово") }
            }
        )
    }

    // --- ДИАЛОГ ОФОРМЛЕНИЯ ЧАТОВ ---
    if (showGlobalChatConfigDialog) {
        var isUpdatingChats by remember { mutableStateOf(false) }
        val wallpaperLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                isUpdatingChats = true
                uploadImageToCloudinary(
                    context = context,
                    imageUri = it,
                    cloudName = "dcwp4nm3e",
                    uploadPreset = "ProfilePIC",
                    onSuccess = { url ->
                        // Обновляем все чаты пользователя
                        db.collection("chats")
                            .whereArrayContains("participants", myUsername)
                            .get()
                            .addOnSuccessListener { snap ->
                                val batch = db.batch()
                                for (doc in snap) {
                                    batch.update(doc.reference, "wallpaperUrl", url)
                                }
                                batch.commit().addOnSuccessListener {
                                    isUpdatingChats = false
                                    android.widget.Toast.makeText(context, "Обои применены ко всем чатам!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                    },
                    onError = { isUpdatingChats = false }
                )
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isUpdatingChats) showGlobalChatConfigDialog = false },
            title = { Text("Оформление всех чатов") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Выберите действие, которое применится ко всем вашим перепискам:")
                    
                    Button(
                        onClick = { wallpaperLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingChats
                    ) {
                        Text("Установить общие обои")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text("Выберите общую тему:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val themes = listOf("DEFAULT", "BLUE", "GREEN", "PURPLE", "ORANGE")
                        themes.forEach { themeName ->
                            val color = when(themeName) {
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
                                    .clickable {
                                        isUpdatingChats = true
                                        db.collection("chats")
                                            .whereArrayContains("participants", myUsername)
                                            .get()
                                            .addOnSuccessListener { snap ->
                                                val batch = db.batch()
                                                for (doc in snap) {
                                                    batch.update(doc.reference, "theme", themeName)
                                                }
                                                batch.commit().addOnSuccessListener {
                                                    isUpdatingChats = false
                                                    android.widget.Toast.makeText(context, "Тема обновлена для всех чатов!", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                            )
                        }
                    }
                    
                    if (isUpdatingChats) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGlobalChatConfigDialog = false }, enabled = !isUpdatingChats) { Text("Закрыть") }
            }
        )
    }

    // --- ДИАЛОГИ ПРИВАТНОСТИ ---
    if (showLastSeenDialog) {
        PrivacyOptionDialog(
            title = "Кто видит время захода?",
            currentValue = privacyLastSeen,
            onDismiss = { showLastSeenDialog = false },
            onSelect = { newValue ->
                privacyLastSeen = newValue
                db.collection("users").document(myUsername).update("privacyLastSeen", newValue)
                showLastSeenDialog = false
            }
        )
    }

    if (showPhotoPrivacyDialog) {
        PrivacyOptionDialog(
            title = "Кто видит моё фото?",
            currentValue = privacyPhoto,
            onDismiss = { showPhotoPrivacyDialog = false },
            onSelect = { newValue ->
                privacyPhoto = newValue
                db.collection("users").document(myUsername).update("privacyPhoto", newValue)
                showPhotoPrivacyDialog = false
            }
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Связаться с нами") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Выберите удобный способ связи:")
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:zhirpem1@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Поддержка Zhirpem")
                            }
                            context.startActivity(intent)
                            showSupportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Основная почта (zhirpem1@...)")
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:richikcat51@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Вопрос разработчику Zhirpem")
                            }
                            context.startActivity(intent)
                            showSupportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Почта разработчика (richikcat51@...)")
                    }

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/zhirpem1"))
                            context.startActivity(intent)
                            showSupportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24A1DE))
                    ) {
                        Text("Telegram Канал")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showFAQDialog) {
        AlertDialog(
            onDismissRequest = { showFAQDialog = false },
            title = { Text("FAQ — Вопросы и Ответы") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FAQItem(
                        question = "Как изменить аватар?",
                        answer = "Перейдите в свой профиль и нажмите на область аватара. Выберите фото из галереи или сделайте новое."
                    )
                    FAQItem(
                        question = "Забыл пароль, что делать?",
                        answer = "На экране входа нажмите 'Забыли пароль?'. Вы сможете сбросить его с помощью 6-значного Backup Code, который был выдан при регистрации."
                    )
                    FAQItem(
                        question = "Как получить галочку верификации?",
                        answer = "Верификация выдается активным пользователям, авторам контента или по усмотрению администрации. Напишите в поддержку для деталей."
                    )
                    FAQItem(
                        question = "Не приходят уведомления",
                        answer = "Проверьте, включены ли уведомления в настройках приложения (раздел Система) и в настройках телефона для Zhirpem."
                    )
                    FAQItem(
                        question = "Что такое Эффект стекла?",
                        answer = "Это визуальный эффект размытия (Blur), который делает интерфейс более современным. Можно настроить прозрачность или выключить для экономии заряда."
                    )
                    FAQItem(
                        question = "Как скрыть время последнего захода?",
                        answer = "В настройках зайдите в раздел 'Конфиденциальность' -> 'Последняя активность' и выберите 'Никто'."
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        "Остались вопросы? Загляните в наш Telegram канал:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/zhirpem1"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24A1DE))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Перейти в Telegram")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFAQDialog = false }) { Text("Закрыть") }
            }
        )
    }
}

@Composable
fun FAQItem(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Q: $question", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Text(text = "A: $answer", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    }
}

@Composable
fun PrivacyOptionDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                val options = listOf("all" to "Все", "friends" to "Мои друзья (взаимно)", "none" to "Никто")
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentValue == value, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color = Color.White,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            scale = 0.8f
        )
    }
}

// Вспомогательное расширение для уменьшения свитча
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    enabled: Boolean = true
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        enabled = enabled
    )
}

fun uploadImageToCloudinary(
    context: android.content.Context,
    imageUri: Uri,
    cloudName: String,
    uploadPreset: String,
    onSuccess: (String) -> Unit,
    onError: () -> Unit
) {
    try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val file = java.io.File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.jpg")
        inputStream.use { input ->
            java.io.FileOutputStream(file).use { output -> input?.copyTo(output) }
        }

        val client = okhttp3.OkHttpClient()
        val mediaType = "image/jpeg".toMediaType()
        val requestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = okhttp3.Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) { onError() }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val json = org.json.JSONObject(response.body?.string() ?: "{}")
                    onSuccess(json.getString("secure_url"))
                } else { onError() }
            }
        })
    } catch (e: Exception) { onError() }
}
