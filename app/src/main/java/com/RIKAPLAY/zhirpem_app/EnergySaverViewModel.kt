package com.RIKAPLAY.zhirpem_app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EnergySaverViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EnergySaverRepository(application)
    private val batteryObserver = BatteryObserver(application)
    private val settingsManager = SettingsManager(application)
    private val themeManager = ThemeManager(application)
    private val sessionPrefs = application.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)

    val autoEnergySaverEnabled = repository.autoEnergySaverEnabled
    val batteryThreshold = repository.batteryThreshold
    val isEnergySaverActive = repository.isEnergySaverActive

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    init {
        // Track battery and auto-trigger
        batteryObserver.batteryStatus
            .onEach { status ->
                _batteryLevel.value = status.level
                checkAutoTrigger(status.level)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun checkAutoTrigger(level: Int) {
        val autoEnabled = autoEnergySaverEnabled.first()
        val threshold = batteryThreshold.first()
        val isActive = isEnergySaverActive.first()

        if (autoEnabled) {
            if (level <= threshold && !isActive) {
                activateEnergySaver()
            } else if (level > threshold && isActive) {
                deactivateEnergySaver()
            }
        }
    }

    fun toggleAutoEnergySaver(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoEnergySaver(enabled)
            // Immediately check if we should activate/deactivate
            checkAutoTrigger(_batteryLevel.value)
        }
    }

    fun setThreshold(threshold: Int) {
        viewModelScope.launch {
            repository.setBatteryThreshold(threshold)
            checkAutoTrigger(_batteryLevel.value)
        }
    }

    fun manualToggleEnergySaver(active: Boolean) {
        viewModelScope.launch {
            if (active) activateEnergySaver() else deactivateEnergySaver()
        }
    }

    private suspend fun activateEnergySaver() {
        if (isEnergySaverActive.first()) return

        // 1. Take snapshot
        val currentAppTheme = sessionPrefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"
        val snapshot = EnergySnapshot(
            glassEnabled = settingsManager.isGlassEnabled,
            themeType = themeManager.themeType,
            appThemeMode = currentAppTheme,
            personalizationEnabled = themeManager.themeType != ThemeManager.TYPE_DEFAULT,
            animationsEnabled = !settingsManager.isLowPerformanceMode
        )
        repository.saveSnapshot(snapshot)

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

    private suspend fun deactivateEnergySaver() {
        if (!isEnergySaverActive.first()) return

        // Restore from snapshot
        val snapshot = repository.snapshot.first()
        
        snapshot.glassEnabled?.let { settingsManager.isGlassEnabled = it }
        snapshot.animationsEnabled?.let { settingsManager.isLowPerformanceMode = !it }
        snapshot.themeType?.let { themeManager.themeType = it }
        snapshot.appThemeMode?.let { 
            sessionPrefs.edit().putString("app_theme", it).apply()
        }

        repository.setEnergySaverActive(false)
    }
}
