package com.RIKAPLAY.zhirpem_app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EnergySaverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EnergySaverRepository(application)
    private val batteryObserver = BatteryObserver(application)
    private val settingsManager = SettingsManager(application)
    private val themeManager = ThemeManager(application)
    private val sessionPrefs = application.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    private val actionMutex = Mutex()

    val autoEnergySaverEnabled = repository.autoEnergySaverEnabled
    val batteryThreshold = repository.batteryThreshold
    val isEnergySaverActive = repository.isEnergySaverActive

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isSystemPowerSave = MutableStateFlow(false)

    init {
        // Track battery and auto-trigger
        batteryObserver.batteryStatus
            .onEach { status ->
                _batteryLevel.value = status.level
                _isSystemPowerSave.value = status.isPowerSaveMode
                checkAutoTrigger(status.level, status.isPowerSaveMode)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun checkAutoTrigger(level: Int, systemPowerSave: Boolean) {
        val autoEnabled = autoEnergySaverEnabled.first()
        val threshold = batteryThreshold.first()
        val isActive = isEnergySaverActive.first()

        if (autoEnabled) {
            // Включаем, если заряд низкий ИЛИ если системный режим экономии включен
            if ((level <= threshold || systemPowerSave) && !isActive) {
                activateEnergySaver()
            } else if (level > threshold && !systemPowerSave && (isActive || settingsManager.isLowPerformanceMode)) {
                deactivateEnergySaver()
            }
        }
    }

    fun toggleAutoEnergySaver(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoEnergySaver(enabled)
            // Immediately check if we should activate/deactivate
            checkAutoTrigger(_batteryLevel.value, _isSystemPowerSave.value)
        }
    }

    fun setThreshold(threshold: Int) {
        viewModelScope.launch {
            repository.setBatteryThreshold(threshold)
            checkAutoTrigger(_batteryLevel.value, _isSystemPowerSave.value)
        }
    }

    fun manualToggleEnergySaver(active: Boolean) {
        viewModelScope.launch {
            if (active) activateEnergySaver() else deactivateEnergySaver()
        }
    }

    private suspend fun activateEnergySaver() {
        actionMutex.withLock {
            val isActive = isEnergySaverActive.first()
            if (isActive) return@withLock

            // 1. Take snapshot (только если мы сейчас НЕ в режиме экономии, чтобы не сохранить "плохие" значения)
            if (settingsManager.isGlassEnabled || !settingsManager.isLowPerformanceMode) {
                val currentAppTheme = sessionPrefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"
                val snapshot = EnergySnapshot(
                    glassEnabled = settingsManager.isGlassEnabled,
                    themeType = themeManager.themeType,
                    appThemeMode = currentAppTheme,
                    personalizationEnabled = themeManager.themeType != ThemeManager.TYPE_DEFAULT,
                    animationsEnabled = !settingsManager.isLowPerformanceMode
                )
                repository.saveSnapshot(snapshot)
            }

            // 2. Apply preset
            settingsManager.isGlassEnabled = false
            settingsManager.isLowPerformanceMode = true
            
            // Force Dark Theme
            sessionPrefs.edit().putString("app_theme", "DARK").apply()
            // If personalization was on, ensure it's dark too
            if (themeManager.themeType == ThemeManager.TYPE_MY_LIGHT) {
                themeManager.themeType = ThemeManager.TYPE_MY_DARK
            }

            repository.setEnergySaverActive(true)
        }
    }

    private suspend fun deactivateEnergySaver() {
        actionMutex.withLock {
            // Убрали строгую проверку isActive, чтобы можно было выйти из "зависшего" состояния
            
            // Restore from snapshot
            val snapshot = repository.snapshot.first()
            
            // Если в снимке есть данные, восстанавливаем их. 
            // Если снимка нет (первый запуск после бага), восстанавливаем значения по умолчанию (true)
            settingsManager.isGlassEnabled = snapshot.glassEnabled ?: true
            settingsManager.isLowPerformanceMode = !(snapshot.animationsEnabled ?: true)

            snapshot.themeType?.let { themeManager.themeType = it }
            snapshot.appThemeMode?.let { 
                sessionPrefs.edit().putString("app_theme", it).apply()
            }

            repository.setEnergySaverActive(false)
        }
    }
}
