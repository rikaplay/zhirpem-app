package com.RIKAPLAY.zhirpem_app

import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun getPosts(): Flow<List<Post>>
    suspend fun createPost(post: Post): Result<String>
    suspend fun getUser(username: String): Result<User?>
    suspend fun saveUser(user: User): Result<Unit>
    fun getOnlineStatus(username: String): Flow<Boolean>
    suspend fun updateOnlineStatus(username: String, isOnline: Boolean)
}

data class Post(
    val id: String = "",
    val author: String = "",
    val handle: String = "",
    val text: String = "",
    val date: String = "",
    val time: String = "",
    val likes: Int = 0,
    val views: Int = 0,
    val isMedia: Boolean = false,
    val mediaUrl: String = "",
    val mediaType: String = "NONE",
    val authorAvatarUrl: String? = null,
    val authorNameColor: String? = null,
    val timestamp: Long? = null
)

data class User(
    val username: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val nameColor: String? = null,
    val backupCode: String? = null
)

expect fun getDataRepository(): DataRepository
