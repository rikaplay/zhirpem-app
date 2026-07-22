package com.RIKAPLAY.zhirpem_app

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class AndroidDataRepository : DataRepository {
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()

    override fun getPosts(): Flow<List<Post>> = flow {
        // Simple flow implementation for example
        val snapshot = db.collection("zhirpem_posts").get().await()
        val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
        emit(posts)
    }

    override suspend fun createPost(post: Post): Result<String> = try {
        val ref = db.collection("zhirpem_posts").add(post).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUser(username: String): Result<User?> = try {
        val doc = db.collection("users").document(username).get().await()
        Result.success(doc.toObject(User::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun saveUser(user: User): Result<Unit> = try {
        db.collection("users").document(user.username).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getOnlineStatus(username: String): Flow<Boolean> = flow {
        // Stub for now
        emit(true)
    }

    override suspend fun updateOnlineStatus(username: String, isOnline: Boolean) {
        db.collection("users").document(username).update("isOnline", isOnline)
    }
}

actual fun getDataRepository(): DataRepository = AndroidDataRepository()
