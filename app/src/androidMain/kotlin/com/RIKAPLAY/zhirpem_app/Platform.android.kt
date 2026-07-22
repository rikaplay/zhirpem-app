package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android"
    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

actual fun getPlatform(): Platform = AndroidPlatform(TODO("Need context"))

val LocalGlassEnabled = staticCompositionLocalOf { true }

@Composable
actual fun LocalGlassEnabledProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassEnabled provides true, content = content)
}
