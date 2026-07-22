package com.RIKAPLAY.zhirpem_app

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
    fun showToast(message: String)
    fun setString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
}

expect fun getPlatform(): Platform

@Composable
expect fun LocalGlassEnabledProvider(content: @Composable () -> Unit)
