package com.RIKAPLAY.zhirpem_app

import androidx.compose.runtime.Composable

interface Platform {
    val name: String
    fun showToast(message: String)
}

expect fun getPlatform(): Platform

@Composable
expect fun LocalGlassEnabledProvider(content: @Composable () -> Unit)
