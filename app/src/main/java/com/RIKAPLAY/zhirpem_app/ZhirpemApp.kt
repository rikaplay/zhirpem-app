package com.RIKAPLAY.zhirpem_app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.cloudinary.android.MediaManager
import com.onesignal.OneSignal
import com.RIKAPLAY.zhirpem_app.BuildConfig

class ZhirpemApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 0. Инициализация OneSignal
        OneSignal.initWithContext(this, BuildConfig.ONESIGNAL_APP_ID)

        // 1. Инициализация Cloudinary
        val config = HashMap<String, String>()
        config["cloud_name"] = BuildConfig.CLOUDINARY_CLOUD_NAME
        config["api_key"] = BuildConfig.CLOUDINARY_API_KEY
        config["api_secret"] = BuildConfig.CLOUDINARY_API_SECRET
        
        try {
            MediaManager.init(this, config)
        } catch (_: Exception) {
            // Уже инициализировано
        }

        // 2. Создание канала уведомлений (для Android 8.0+)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Уведомления Жирпем"
            val descriptionText = "Получайте уведомления о лайках, комментариях и сообщениях"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("zhirpem_notifications", name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
