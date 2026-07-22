package com.RIKAPLAY.zhirpem_app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.cloudinary.android.MediaManager
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class ZhirpemApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 0. Инициализация OneSignal
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, "e52144a6-d4ea-46a4-870f-4089ec7a6af9")

        // 1. Инициализация Cloudinary
        val config = HashMap<String, String>()
        config["cloud_name"] = "dcwp4nm3e"
        config["api_key"] = "163977746791319"
        config["api_secret"] = "uIOnNEK9v2_duKWMBSSgZX7lLOM"
        
        try {
            MediaManager.init(this, config)
        } catch (e: Exception) {
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
