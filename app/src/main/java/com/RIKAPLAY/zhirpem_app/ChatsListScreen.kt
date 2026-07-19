package com.RIKAPLAY.zhirpem_app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(onChatClick: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", Context.MODE_PRIVATE) }
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    var chats by remember { mutableStateOf(listOf<Chat>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            db.collection("chats")
                .whereArrayContains("participants", myUsername)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                            .sortedByDescending { it.lastMessageTimestamp }
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщения", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(Icons.Default.Chat, contentDescription = "Начать чат")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (chats.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("💬", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("У вас пока нет чатов", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(chats) { chat ->
                    val peerId = chat.participants.firstOrNull { it != myUsername } ?: ""
                    var peerName by remember { mutableStateOf(peerId) }
                    var peerAvatarUrl by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(peerId) {
                        db.collection("users").document(peerId).get().addOnSuccessListener { doc ->
                            peerName = doc.getString("name") ?: peerId
                            peerAvatarUrl = doc.getString("avatarUrl")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatClick(chat.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            if (!peerAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = peerAvatarUrl,
                                    contentDescription = "Аватар",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        peerName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            PresenceIndicator(
                                username = peerId,
                                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                                size = 14.dp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(peerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(chat.lastMessage, maxLines = 1, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 86.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
    }

    if (showNewChatDialog) {
        NewChatDialog(
            onDismiss = { showNewChatDialog = false },
            onChatClick = { chatId ->
                showNewChatDialog = false
                onChatClick(chatId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(onDismiss: () -> Unit, onChatClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var users by remember { mutableStateOf(listOf<Pair<String, String>>()) } // Pair(username, name)
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val myUsername = context.getSharedPreferences("user_session", Context.MODE_PRIVATE).getString("username", "") ?: ""

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            db.collection("users")
                .whereGreaterThanOrEqualTo("username", searchQuery.lowercase())
                .whereLessThanOrEqualTo("username", searchQuery.lowercase() + "\uf8ff")
                .get()
                .addOnSuccessListener { snap ->
                    users = snap.documents.mapNotNull { 
                        val uname = it.id
                        if (uname != myUsername) {
                            uname to (it.getString("name") ?: uname)
                        } else null
                    }
                }
        } else {
            users = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый чат") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по юзернейму") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (users.isEmpty() && searchQuery.length >= 2) {
                    Text("Пользователи не найдены", modifier = Modifier.padding(8.dp), color = Color.Gray)
                }
                
                LazyColumn {
                    items(users) { (username, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val chatId = listOf(myUsername, username).sorted().joinToString("_")
                                    // Проверяем/создаем чат в БД
                                    val chatRef = db.collection("chats").document(chatId)
                                    chatRef.get().addOnSuccessListener { doc ->
                                        if (!doc.exists()) {
                                            val newChat = hashMapOf(
                                                "id" to chatId,
                                                "participants" to listOf(myUsername, username),
                                                "lastMessage" to "Чат создан",
                                                "lastMessageTimestamp" to System.currentTimeMillis()
                                            )
                                            chatRef.set(newChat)
                                        }
                                        onChatClick(chatId)
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Bold)
                                Text("@$username", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
