package com.RIKAPLAY.zhirpem_app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun sendSocialNotification(
        senderId: String,
        senderName: String,
        senderAvatar: String,
        receiverId: String,
        type: NotificationType,
        targetText: String = "",
        postId: String? = null
    ) {
        if (senderId == receiverId || receiverId.isEmpty()) return

        // 1. Проверка фильтров получателя
        val shouldDeliver = checkFilters(receiverId, senderId, type)
        if (!shouldDeliver) return

        // 2. Запись в ленту уведомлений
        val notification = hashMapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "senderAvatarUrl" to senderAvatar,
            "receiverId" to receiverId,
            "type" to type.name,
            "targetText" to targetText,
            "postId" to postId,
            "timestamp" to Timestamp.now()
        )
        db.collection("notifications").add(notification)
    }

    private suspend fun checkFilters(receiverId: String, senderId: String, type: NotificationType): Boolean {
        return try {
            val userDoc = db.collection("users").document(receiverId).get().await()
            val settingsMap = userDoc.get("notificationSettings") as? Map<*, *>
            
            val senderFilterStr = settingsMap?.get("senderFilter") as? String
            val senderFilter = if (senderFilterStr != null) {
                try { NotificationSenderFilter.valueOf(senderFilterStr) } catch(e: Exception) { NotificationSenderFilter.ALL }
            } else NotificationSenderFilter.ALL

            val enabledCategoriesList = settingsMap?.get("enabledCategories") as? List<*>
            val enabledCategories = enabledCategoriesList
                ?.mapNotNull { try { NotificationType.valueOf(it as String) } catch (e: Exception) { null } }
                ?.toSet() ?: NotificationType.entries.toSet()

            // Проверка категории
            if (!enabledCategories.contains(type)) return false

            // Проверка отправителя
            when (senderFilter) {
                NotificationSenderFilter.NONE -> false
                NotificationSenderFilter.ALL -> true
                NotificationSenderFilter.FOLLOWING -> {
                    val follow = db.collection("follows")
                        .whereEqualTo("follower", receiverId)
                        .whereEqualTo("following", senderId)
                        .get().await()
                    !follow.isEmpty
                }
                NotificationSenderFilter.DIRECT_CHAT_ONLY -> {
                    val chat = db.collection("chats")
                        .whereArrayContains("participants", receiverId)
                        .get().await()
                    chat.documents.any { (it.get("participants") as? List<*>)?.contains(senderId) == true }
                }
            }
        } catch (e: Exception) {
            true // По умолчанию доставляем при ошибках
        }
    }
}
