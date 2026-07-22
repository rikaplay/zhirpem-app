package com.RIKAPLAY.zhirpem_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

interface Platform {
    val name: String
    fun showToast(message: String)
    fun setString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
}

expect fun getPlatform(): Platform

// CompositionLocal for glass effect, common to all platforms
val LocalGlassEnabled = staticCompositionLocalOf { true }

@Composable
expect fun PlatformSpecificProvider(content: @Composable () -> Unit)
