package com.RIKAPLAY.zhirpem_app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WasmDataRepository : DataRepository {
    override fun getPosts(): Flow<List<Post>> = flow {
        // Return some mock data for now
        emit(listOf(
            Post(author = "Rikaplay", text = "Привет из Wasm! 🚀"),
            Post(author = "Admin", text = "Zhirpem теперь работает в вебе.")
        ))
    }

    override suspend fun createPost(post: Post): Result<String> {
        return Result.success("mock_id")
    }

    override suspend fun getUser(username: String): Result<User?> {
        return Result.success(User(username = username, name = "Web User"))
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getOnlineStatus(username: String): Flow<Boolean> = flow {
        emit(true)
    }

    override suspend fun updateOnlineStatus(username: String, isOnline: Boolean) {
    }
}

actual fun getDataRepository(): DataRepository = WasmDataRepository()
