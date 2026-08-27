package com.RIKAPLAY.zhirpem_app

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore

object ChatRepository {

    fun uploadMediaToCloudinary(
        context: Context,
        fileUri: Uri,
        messageType: String, // "voice", "video_square" или "image"
        chatId: String,
        currentUserId: String,
        senderName: String = "",
        senderAvatar: String = ""
    ) {
        val resourceType = if (messageType == "image") "image" else "video"
        
        // Запуск асинхронной загрузки через Cloudinary MediaManager
        MediaManager.get().upload(fileUri)
            .unsigned("mediapres") // Твой Preset Name
            .option("resource_type", resourceType)
            .callback(object : UploadCallback {
                
                override fun onStart(requestId: String) {
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String ?: return

                    sendMessageToFirestore(
                        chatId = chatId,
                        senderId = currentUserId,
                        mediaUrl = secureUrl,
                        type = messageType,
                        senderName = senderName,
                        senderAvatar = senderAvatar
                    )
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                }
            })
            .dispatch(context)
    }

    private fun sendMessageToFirestore(
        chatId: String, 
        senderId: String, 
        mediaUrl: String, 
        type: String,
        senderName: String,
        senderAvatar: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val chatRef = db.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document()

        val mediaType = when(type) {
            "image" -> MediaType.IMAGE
            "video_square", "voice" -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }

        val messageMap = hashMapOf(
            "senderId" to senderId,
            "text" to "",
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType.name,
            "timestamp" to System.currentTimeMillis(),
            "replyToId" to if (type == "voice") "voice" else null
        )

        db.runTransaction { transaction ->
            transaction.set(messageRef, messageMap)
            transaction.update(chatRef, "lastMessage", "📎 Медиасообщение")
            transaction.update(chatRef, "lastMessageTimestamp", System.currentTimeMillis())
        }

        // Уведомление собеседнику
        val peerId = chatId.split("_").firstOrNull { it != senderId } ?: ""
        if (peerId.isNotEmpty()) {
            checkRecipientAndNotify(
                db = db,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                receiverId = peerId,
                chatId = chatId,
                text = "📎 Медиасообщение"
            )
        }
    }

    private fun checkRecipientAndNotify(
        db: FirebaseFirestore,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        receiverId: String,
        chatId: String,
        text: String
    ) {
        db.collection("users").document(receiverId).get().addOnSuccessListener { doc ->
            val isOnlyVerified = doc.getBoolean("isOnlyVerifiedMessages") ?: false
            
            fun proceedWithNotification() {
                val status = doc.getString("status") ?: "offline"
                val currentScreen = doc.getString("currentScreen") ?: ""
                val expectedScreen = "ChatScreen_$senderId" // Формат экрана чата

                // 1. Всегда отправляем Push (через существующую функцию)
                sendNotification(
                    db = db,
                    senderId = senderId,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    receiverId = receiverId,
                    type = "CHAT_MESSAGE",
                    text = text
                )

                // 2. Если не в чате или оффлайн -> Запись в ленту уведомлений
                if (status == "offline" || currentScreen != expectedScreen) {
                    val feedNotify = hashMapOf(
                        "senderId" to senderId,
                        "senderName" to senderName,
                        "senderAvatarUrl" to senderAvatar,
                        "receiverId" to receiverId,
                        "type" to "CHAT_MESSAGE",
                        "text" to text,
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "chatId" to chatId
                    )
                    db.collection("notifications").add(feedNotify)
                }
            }

            if (isOnlyVerified) {
                db.collection("users").document(senderId).get().addOnSuccessListener { senderDoc ->
                    val isVerified = senderDoc.getBoolean("blueBadge") == true || senderDoc.getBoolean("yellowBadge") == true
                    if (isVerified) {
                        proceedWithNotification()
                    }
                }
            } else {
                proceedWithNotification()
            }
        }
    }
}
