package com.RIKAPLAY.zhirpem_app

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import kotlinx.browser.localStorage

class WasmPlatform : Platform {
    override val name: String = "Web (Wasm)"
    
    override fun showToast(message: String) {
        window.alert(message)
    }

    override fun setString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return localStorage.getItem(key) ?: defaultValue
    }

    override fun setBoolean(key: String, value: Boolean) {
        localStorage.setItem(key, value.toString())
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return localStorage.getItem(key)?.toBoolean() ?: defaultValue
    }
}

actual fun getPlatform(): Platform = WasmPlatform()

@Composable
actual fun PlatformSpecificProvider(content: @Composable () -> Unit) {
    content()
}
