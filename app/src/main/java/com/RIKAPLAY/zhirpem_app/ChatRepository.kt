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
            sendNotification(
                db = db,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                receiverId = peerId,
                type = "MESSAGE",
                text = "📎 Медиасообщение"
            )
        }
    }
}
