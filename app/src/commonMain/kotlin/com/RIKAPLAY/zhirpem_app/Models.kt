package com.RIKAPLAY.zhirpem_app

import kotlinx.serialization.Serializable
import dev.gitlive.firebase.firestore.Timestamp

@Serializable
enum class MediaType {
    NONE, IMAGE, VIDEO, GIF
}

@Serializable
data class PollData(
    val question: String = "",
    val options: List<String> = emptyList(),
    val anonymous: Boolean = true,
    val multipleChoice: Boolean = false,
    val votes: Map<String, List<String>> = emptyMap()
)

@Serializable
data class Post(
    val id: String = "",
    val author: String = "",
    val date: String = "",
    val handle: String = "",
    var isMedia: Boolean = false,
    val imageUrl: String? = null,
    val mediaUrl: String = "",
    val mediaType: MediaType = MediaType.NONE,
    val authorAvatarUrl: String? = null,
    val blueBadge: Boolean = false,
    val yellowBadge: Boolean = false,
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val text: String = "",
    val time: String = "",
    val views: Int = 0,
    val likedBy: List<String> = emptyList(),
    val bookmarkedBy: List<String> = emptyList(),
    val repostedBy: List<String> = emptyList(),
    // GitLive 2.x handles Timestamp with its own serializer
    // val timestamp: Timestamp? = null, 
    val isAuthorBanned: Boolean = false,
    val authorNameColor: String? = null,
    val communityId: String? = null,
    val poll: PollData? = null,
    val authorStatus: String? = null
)

@Serializable
data class Community(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val avatarUrl: String? = null,
    val membersCount: Int = 0,
    val members: List<String> = emptyList()
)

@Serializable
data class User(
    val username: String = "",
    val name: String = "",
    val password: String = "",
    val avatarUrl: String? = null,
    val nameColor: String? = null,
    val isBanned: Boolean = false,
    val bio: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val isOnline: Boolean = false
)
