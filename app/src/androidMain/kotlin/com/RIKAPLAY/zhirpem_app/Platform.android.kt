package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android"
    
    private val prefs = context.getSharedPreferences("zhirpem_prefs", Context.MODE_PRIVATE)

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
}

lateinit var androidPlatform: AndroidPlatform

actual fun getPlatform(): Platform = androidPlatform

@Composable
actual fun PlatformSpecificProvider(content: @Composable () -> Unit) {
    // Android specific providers if any
    content()
}
