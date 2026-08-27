package com.RIKAPLAY.zhirpem_app

import com.google.firebase.Timestamp

enum class NotificationType {
    CHAT_MESSAGE, LIKE, COMMENT, FOLLOW, ADMIN
}

enum class NotificationSenderFilter {
    ALL, FOLLOWING, NONE, DIRECT_CHAT_ONLY
}

data class NotificationSettings(
    val isVibrationEnabled: Boolean = true,
    val enabledCategories: Set<NotificationType> = NotificationType.entries.toSet(),
    val senderFilter: NotificationSenderFilter = NotificationSenderFilter.ALL
)

data class NotificationModel(
    val id: String = "",
    val senderId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val targetText: String = "", 
    val userComment: String = "", 
    val timestamp: Timestamp? = null,
    val receiverId: String = "",
    val postId: String? = null,
    val bigPictureUrl: String = ""
)
