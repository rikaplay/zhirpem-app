package com.RIKAPLAY.zhirpem_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.browser.window

class WasmPlatform : Platform {
    override val name: String = "Web (Wasm)"
    override fun showToast(message: String) {
        window.alert(message)
    }
}

actual fun getPlatform(): Platform = WasmPlatform()

val LocalGlassEnabled = staticCompositionLocalOf { false }

@Composable
actual fun LocalGlassEnabledProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassEnabled provides false, content = content)
}
