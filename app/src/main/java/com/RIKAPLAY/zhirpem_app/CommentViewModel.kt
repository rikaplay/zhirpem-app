package com.RIKAPLAY.zhirpem_app

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var commentsListener: ListenerRegistration? = null
    
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    // 1. СЛУШАТЕЛЬ В РЕАЛЬНОМ ВРЕМЕНИ
    fun listenToComments(postId: String) {
        if (postId.isEmpty()) return
        
        // Удаляем старый слушатель перед созданием нового
        commentsListener?.remove()
        
        commentsListener = db.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Ошибка при получении комментариев: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val commentList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }
                    _comments.value = commentList
                    Log.d("FirestoreSuccess", "Загружено ${commentList.size} комментариев")
                }
            }
    }

    // 2. ОГРАНИЧЕНИЕ: Ровно 1 лайк от пользователя (Переключатель)
    fun toggleLikeComment(commentId: String, currentUserId: String) {
        if (commentId.isEmpty() || currentUserId.isEmpty()) return
        
        val commentRef = db.collection("comments").document(commentId)

        // Получаем текущее состояние из нашего локального списка
        val currentComment = _comments.value.find { it.id == commentId } ?: return
        val isAlreadyLiked = currentComment.likedBy.contains(currentUserId)

        if (isAlreadyLiked) {
            commentRef.update(
                "likesCount", FieldValue.increment(-1),
                "likedBy", FieldValue.arrayRemove(currentUserId)
            ).addOnFailureListener { e ->
                Log.e("FirestoreError", "Ошибка при удалении лайка: ${e.message}")
            }
        } else {
            commentRef.update(
                "likesCount", FieldValue.increment(1),
                "likedBy", FieldValue.arrayUnion(currentUserId)
            ).addOnFailureListener { e ->
                Log.e("FirestoreError", "Ошибка при добавлении лайка: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        commentsListener?.remove()
    }
}
