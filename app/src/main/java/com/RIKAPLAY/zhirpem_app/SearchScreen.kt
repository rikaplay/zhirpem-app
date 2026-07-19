package com.RIKAPLAY.zhirpem_app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray

// ==========================================
// 1. МЕНЕДЖЕР ИСТОРИИ ПОИСКА
// ==========================================
object SearchHistoryManager {
    private const val PREFS_NAME = "search_prefs"
    private const val KEY_HISTORY = "search_history"

    fun saveSearchQuery(context: Context, query: String) {
        if (query.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentHistory = getSearchHistory(context).toMutableList()

        currentHistory.remove(query)
        currentHistory.add(0, query)

        val limitedHistory = currentHistory.take(10)
        val jsonArray = JSONArray(limitedHistory)
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun getSearchHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}

// ==========================================
// 2. ЭКРАН ПОИСКА
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    onNavigateToProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var searchQuery by remember { mutableStateOf(initialQuery) }
    var searchHistory by remember { mutableStateOf(SearchHistoryManager.getSearchHistory(context)) }

    var foundPosts by remember { mutableStateOf(listOf<Post>()) }
    var foundUsers by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var foundComments by remember { mutableStateOf(listOf<Comment>()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Посты", "Люди", "Комменты")

    val performSearch = { query: String ->
        if (query.isNotBlank()) {
            SearchHistoryManager.saveSearchQuery(context, query)
            searchHistory = SearchHistoryManager.getSearchHistory(context)

            val endQuery = query + "\uf8ff"

            // 1. Поиск постов
            db.collection("zhirpem_posts")
                .orderBy("text")
                .startAt(query)
                .endAt(endQuery)
                .get()
                .addOnSuccessListener { snapshot ->
                    foundPosts = snapshot.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
                }

            // 2. Поиск пользователей
            db.collection("users")
                .orderBy("name")
                .startAt(query)
                .endAt(endQuery)
                .get()
                .addOnSuccessListener { snapshot ->
                    foundUsers = snapshot.documents.mapNotNull { doc ->
                        doc.data?.plus("id" to doc.id)
                    }
                }

            // 3. Поиск комментариев
            db.collection("comments")
                .orderBy("text")
                .startAt(query)
                .endAt(endQuery)
                .get()
                .addOnSuccessListener { snapshot ->
                    foundComments = snapshot.documents.mapNotNull { it.toObject(Comment::class.java)?.copy(id = it.id) }
                }
        }
    }

    // Выполняем поиск сразу, если есть начальный запрос
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotEmpty()) {
            performSearch(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            Column {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { performSearch(searchQuery) },
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Найти посты, людей...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                foundPosts = emptyList()
                                foundUsers = emptyList()
                                foundComments = emptyList()
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}

                if (searchQuery.isNotEmpty()) {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontSize = 14.sp) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            if (searchQuery.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Недавние запросы", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (searchHistory.isNotEmpty()) {
                            TextButton(onClick = {
                                SearchHistoryManager.clearHistory(context)
                                searchHistory = emptyList()
                            }) {
                                Text("Очистить", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    LazyColumn {
                        items(searchHistory) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = historyItem
                                        performSearch(historyItem)
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(historyItem, fontSize = 16.sp)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> { // Посты
                        if (foundPosts.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ничего не найдено") }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(16.dp)) {
                                items(foundPosts, key = { it.id }) { post ->
                                    PostItem(
                                        post = post, 
                                        onUserClick = onNavigateToProfile,
                                        onHashtagClick = { hashtag ->
                                            searchQuery = hashtag
                                            performSearch(hashtag)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // Люди
                        if (foundUsers.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Пользователи не найдены") }
                        } else {
                            LazyColumn {
                                items(foundUsers) { user ->
                                    val uid = user["id"] as? String ?: ""
                                    val name = user["name"] as? String ?: "Аноним"
                                    val avatarUrl = user["avatarUrl"] as? String
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToProfile(uid) }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box {
                                            if (!avatarUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            PresenceIndicator(
                                                username = uid,
                                                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                                                size = 10.dp
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column {
                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("@$uid", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    2 -> { // Комменты
                        if (foundComments.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Комментарии не найдены") }
                        } else {
                            LazyColumn {
                                items(foundComments) { comment ->
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(comment.text, fontSize = 15.sp)
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
