package com.RIKAPLAY.zhirpem_app

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeedViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _postsList = MutableStateFlow<List<Post>>(emptyList())
    val postsList: StateFlow<List<Post>> = _postsList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastVisiblePost: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isLastPage = false
    private val PAGE_SIZE = 25L

    private var lastVisibleForYouPost: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isLastForYouPage = false
    private var isUsingFallback = false

    private val _recommendedPosts = MutableStateFlow<List<Post>>(emptyList())
    val recommendedPosts: StateFlow<List<Post>> = _recommendedPosts.asStateFlow()

    init {
        fetchPosts(isRefresh = true)
    }

    fun fetchForYouPosts(username: String, isRefresh: Boolean = false) {
        if (username.isBlank()) {
            fetchPosts(isRefresh)
            return
        }

        if (_isLoading.value || (isLastForYouPage && !isRefresh)) return

        if (isRefresh) {
            _isRefreshing.value = true
            lastVisibleForYouPost = null
            isLastForYouPage = false
            isUsingFallback = false
        } else {
            _isLoading.value = true
        }

        if (isUsingFallback) {
            fetchTrendingFallback(emptyList(), isRefresh)
            return
        }

        db.collection("user_interests").document(username).get()
            .addOnSuccessListener { interestDoc ->
                val data = interestDoc.data ?: emptyMap<String, Any>()
                val scores = data["scores"] as? Map<String, Long> ?: emptyMap()
                val topTags = scores.toList().sortedByDescending { it.second }.take(10).map { it.first }

                if (topTags.isNotEmpty()) {
                    var query = db.collection("zhirpem_posts")
                        .whereArrayContainsAny("tags", topTags)
                        .limit(PAGE_SIZE)

                    lastVisibleForYouPost?.let {
                        query = query.startAfter(it)
                    }

                    query.get()
                        .addOnSuccessListener { snapshot ->
                            val recommended = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(Post::class.java)?.copy(id = doc.id)
                            }
                            
                            if (snapshot.documents.isNotEmpty()) {
                                lastVisibleForYouPost = snapshot.documents[snapshot.documents.size - 1]
                            }

                            if (snapshot.size() < PAGE_SIZE) {
                                // Если по тегам посты кончились, переходим на общий поток (fallback)
                                isUsingFallback = true
                                lastVisibleForYouPost = null // Сбрасываем курсор для нового запроса в fallback
                                fetchTrendingFallback(recommended, isRefresh)
                            } else {
                                updateForYouList(recommended, isRefresh)
                            }
                        }
                        .addOnFailureListener {
                            isUsingFallback = true
                            fetchTrendingFallback(emptyList(), isRefresh)
                        }
                } else {
                    isUsingFallback = true
                    fetchTrendingFallback(emptyList(), isRefresh)
                }
            }
            .addOnFailureListener {
                isUsingFallback = true
                fetchTrendingFallback(emptyList(), isRefresh)
            }
    }

    private fun updateForYouList(newPosts: List<Post>, isRefresh: Boolean) {
        if (isRefresh) {
            _recommendedPosts.value = newPosts
        } else {
            val currentList = _recommendedPosts.value
            _recommendedPosts.value = (currentList + newPosts).distinctBy { it.id }
        }
        _isLoading.value = false
        _isRefreshing.value = false
    }

    private fun fetchTrendingFallback(recommended: List<Post>, isRefresh: Boolean) {
        // Используем обычный поток постов по времени как бесконечный fallback
        var query = db.collection("zhirpem_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)

        lastVisibleForYouPost?.let {
            query = query.startAfter(it)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val fallbackPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }

                if (snapshot.documents.isNotEmpty()) {
                    lastVisibleForYouPost = snapshot.documents[snapshot.documents.size - 1]
                }

                val combined = (recommended + fallbackPosts).distinctBy { it.id }
                updateForYouList(combined, isRefresh)

                if (snapshot.size() < PAGE_SIZE) {
                    isLastForYouPage = true
                }
            }
            .addOnFailureListener {
                updateForYouList(recommended, isRefresh)
            }
    }

    fun toggleLike(post: Post, userId: String) {
        if (post.id.isEmpty() || userId.isEmpty()) return
        
        val postRef = db.collection("zhirpem_posts").document(post.id)
        val isLiked = post.likedBy.contains(userId)
        
        // Оптимистичное обновление локального списка
        val updatedList = _postsList.value.map {
            if (it.id == post.id) {
                val newLikedBy = if (isLiked) it.likedBy - userId else it.likedBy + userId
                it.copy(
                    likes = if (isLiked) (it.likes - 1).coerceAtLeast(0) else it.likes + 1,
                    likedBy = newLikedBy
                )
            } else it
        }
        _postsList.value = updatedList

        // Обновление в Firestore
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.getLong("likes") ?: 0L
            if (isLiked) {
                transaction.update(postRef, "likedBy", FieldValue.arrayRemove(userId))
                transaction.update(postRef, "likes", (currentLikes - 1).coerceAtLeast(0))
            } else {
                transaction.update(postRef, "likedBy", FieldValue.arrayUnion(userId))
                transaction.update(postRef, "likes", currentLikes + 1)
            }
        }.addOnFailureListener {
            // В случае ошибки возвращаем как было (опционально, для простоты пока оставим так)
        }
    }

    fun toggleBookmark(post: Post, userId: String) {
        if (post.id.isEmpty() || userId.isEmpty()) return
        val postRef = db.collection("zhirpem_posts").document(post.id)
        val isBookmarked = post.bookmarkedBy.contains(userId)

        val updatedList = _postsList.value.map {
            if (it.id == post.id) {
                val newBookmarkedBy = if (isBookmarked) it.bookmarkedBy - userId else it.bookmarkedBy + userId
                it.copy(bookmarkedBy = newBookmarkedBy)
            } else it
        }
        _postsList.value = updatedList

        if (isBookmarked) {
            postRef.update("bookmarkedBy", FieldValue.arrayRemove(userId))
        } else {
            postRef.update("bookmarkedBy", FieldValue.arrayUnion(userId))
        }
    }

    fun toggleRepost(post: Post, userId: String) {
        if (post.id.isEmpty() || userId.isEmpty()) return
        val postRef = db.collection("zhirpem_posts").document(post.id)
        val isReposted = post.repostedBy.contains(userId)

        val updatedList = _postsList.value.map {
            if (it.id == post.id) {
                val newRepostedBy = if (isReposted) it.repostedBy - userId else it.repostedBy + userId
                it.copy(repostedBy = newRepostedBy)
            } else it
        }
        _postsList.value = updatedList

        if (isReposted) {
            postRef.update("repostedBy", FieldValue.arrayRemove(userId))
        } else {
            postRef.update("repostedBy", FieldValue.arrayUnion(userId))
        }
    }

    fun fetchPosts(isRefresh: Boolean = false) {
        if (_isLoading.value || (isLastPage && !isRefresh)) return

        if (isRefresh) {
            _isRefreshing.value = true
            lastVisiblePost = null
            isLastPage = false
        } else {
            _isLoading.value = true
        }

        var query = db.collection("zhirpem_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)

        lastVisiblePost?.let {
            query = query.startAfter(it)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val newPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }

                if (isRefresh) {
                    _postsList.value = newPosts
                    _isRefreshing.value = false
                } else {
                    val currentList = _postsList.value
                    val filteredNewPosts = newPosts.filter { newPost -> 
                        currentList.none { it.id == newPost.id }
                    }
                    _postsList.value = currentList + filteredNewPosts
                    _isLoading.value = false
                }

                if (snapshot.documents.isNotEmpty()) {
                    lastVisiblePost = snapshot.documents[snapshot.documents.size - 1]
                }
                
                if (snapshot.size() < PAGE_SIZE) {
                    isLastPage = true
                }
            }
            .addOnFailureListener { error ->
                _errorMessage.value = "Ошибка загрузки: ${error.message}"
                _isLoading.value = false
                _isRefreshing.value = false
            }
    }
}
