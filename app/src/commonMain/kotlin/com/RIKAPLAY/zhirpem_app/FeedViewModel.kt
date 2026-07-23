package com.RIKAPLAY.zhirpem_app

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.orderBy
import dev.gitlive.firebase.firestore.Direction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedViewModel {
    private val db = Firebase.firestore
    
    private val _postsList = MutableStateFlow<List<Post>>(emptyList())
    val postsList: StateFlow<List<Post>> = _postsList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                db.collection("zhirpem_posts")
                    .orderBy("timestamp", Direction.DESCENDING)
                    .snapshots
                    .collect { snapshot ->
                        _postsList.value = snapshot.documents.map { doc ->
                            doc.data<Post>().copy(id = doc.id)
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
