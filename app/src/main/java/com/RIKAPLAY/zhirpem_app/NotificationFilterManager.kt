package com.RIKAPLAY.zhirpem_app

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class NotificationFilterManager(
    private val repository: NotificationSettingsRepository,
    private val currentUserId: String
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun shouldDeliverNotification(senderId: String, type: NotificationType): Boolean {
        if (currentUserId.isEmpty()) return false
        
        val settings = repository.settingsFlow.first()

        // 1. Проверка категорий
        if (!settings.enabledCategories.contains(type)) {
            return false
        }

        // 2. Проверка фильтра отправителей
        return when (settings.senderFilter) {
            NotificationSenderFilter.NONE -> false
            NotificationSenderFilter.ALL -> true
            NotificationSenderFilter.FOLLOWING -> isFollowing(senderId)
            NotificationSenderFilter.DIRECT_CHAT_ONLY -> hasExistingChat(senderId)
        }
    }

    private suspend fun isFollowing(senderId: String): Boolean {
        return try {
            val snapshot = db.collection("follows")
                .whereEqualTo("follower", currentUserId)
                .whereEqualTo("following", senderId)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun hasExistingChat(senderId: String): Boolean {
        return try {
            val snapshot = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
            
            snapshot.documents.any { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(senderId) == true
            }
        } catch (e: Exception) {
            false
        }
    }
}
