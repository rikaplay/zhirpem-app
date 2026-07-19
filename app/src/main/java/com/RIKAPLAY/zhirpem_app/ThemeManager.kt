package com.RIKAPLAY.zhirpem_app

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeManager(context: Context) {
    companion object {
        const val TYPE_DEFAULT = "DEFAULT"
        const val TYPE_MY_LIGHT = "material_you_light"
        const val TYPE_MY_DARK = "material_you_dark"
    }

    private val prefs = context.getSharedPreferences("app_theme_settings", Context.MODE_PRIVATE)

    var themeType: String
        get() = prefs.getString("theme_type", TYPE_DEFAULT) ?: TYPE_DEFAULT
        set(value) = prefs.edit().putString("theme_type", value).apply()

    var customColor: String
        get() = prefs.getString("custom_color", "#E5DBFF") ?: "#E5DBFF"
        set(value) = prefs.edit().putString("custom_color", value).apply()

    fun getCustomColorObj(): Color {
        return try {
            Color(android.graphics.Color.parseColor(customColor))
        } catch (e: Exception) {
            Color(0xFFE5DBFF)
        }
    }

    fun generateColorScheme(baseColor: Color, isDark: Boolean) = if (isDark) {
        darkColorScheme(
            primary = baseColor,
            primaryContainer = baseColor.copy(alpha = 0.3f),
            onPrimaryContainer = baseColor
        )
    } else {
        lightColorScheme(
            primary = baseColor,
            primaryContainer = baseColor.copy(alpha = 0.3f),
            onPrimaryContainer = baseColor,
            background = Color(0xFFF8F9FA),
            surface = Color(0xFFF8F9FA)
        )
    }
}
