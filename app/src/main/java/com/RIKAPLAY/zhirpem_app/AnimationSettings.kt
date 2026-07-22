package com.RIKAPLAY.zhirpem_app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.compositionLocalOf

enum class AppThemeMode { LIGHT, DARK, AMOLED, SYSTEM, MATERIAL_YOU_LIGHT, MATERIAL_YOU_DARK }

// Глобальный указатель: включены ли полные анимации. По умолчанию — true (анимации работают)
val LocalAnimationsEnabled = compositionLocalOf { true }

// Глобальный коэффициент размера шрифта для доступности
val LocalFontSize = compositionLocalOf { 1.0f }

val LocalGlassEnabled = compositionLocalOf { true }
val LocalGlassAlpha = compositionLocalOf { 0.4f }
val LocalBackgroundBlurEnabled = compositionLocalOf { true }

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Проверяем, включен ли режим облегчения анимаций. По умолчанию FALSE (анимации НЕ облегчены, то есть работают на полную)
    var isLowPerformanceMode: Boolean
        get() = prefs.getBoolean("low_perf_mode", false)
        set(value) = prefs.edit().putBoolean("low_perf_mode", value).apply()

    var fontSizeMultiplier: Float
        get() = prefs.getFloat("font_size_multiplier", 1.0f)
        set(value) = prefs.edit().putFloat("font_size_multiplier", value).apply()

    var isGlassEnabled: Boolean
        get() = prefs.getBoolean("glass_enabled", true)
        set(value) = prefs.edit().putBoolean("glass_enabled", value).apply()

    var glassAlpha: Float
        get() = prefs.getFloat("glass_alpha", 0.4f)
        set(value) = prefs.edit().putFloat("glass_alpha", value).apply()

    var isSplashScreenEnabled: Boolean
        get() = prefs.getBoolean("splash_screen_enabled", true)
        set(value) = prefs.edit().putBoolean("splash_screen_enabled", value).apply()

    var isSplashSoundEnabled: Boolean
        get() = prefs.getBoolean("splash_sound_enabled", true)
        set(value) = prefs.edit().putBoolean("splash_sound_enabled", value).apply()
}

fun changeAppIcon(context: Context, newAliasName: String) {
    val packageManager = context.packageManager
    val packageName = context.packageName
    
    // Список всех возможных точек входа (основная активность + алиасы)
    val components = listOf("MainActivity", "MainActivityAlias1", "MainActivityAlias2", "MainActivityAlias3")
    
    components.forEach { component ->
        val state = if (component == newAliasName) 
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED 
        else 
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            
        packageManager.setComponentEnabledSetting(
            ComponentName(packageName, "$packageName.$component"),
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}
