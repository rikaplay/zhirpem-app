package com.RIKAPLAY.zhirpem_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CommentsScreen(
    postId: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: CommentViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    val myUsername = prefs.getString("username", "") ?: ""

    // Подключаем живой поток данных
    val comments by viewModel.comments.collectAsState()
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    // Запускаем прослушивание Firestore при создании экрана
    LaunchedEffect(postId) {
        viewModel.listenToComments(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Комментарии", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (comments.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Пока нет ответов. Станьте первым!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        CommentItemRow(
                            comment = comment,
                            currentUserId = myUsername,
                            onLikeClick = {
                                // По клику вызываем нашу безопасную функцию-переключатель
                                viewModel.toggleLikeComment(comment.id, myUsername)
                            },
                            onUserClick = onUserClick,
                            onReply = { replyingTo = it }
                        )
                    }
                }
            }
            
            // Поле ввода комментария
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CommentInput(
                    postId = postId,
                    replyTo = replyingTo,
                    onCancelReply = { replyingTo = null }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CommentItemRow(
    comment: Comment,
    currentUserId: String,
    onLikeClick: () -> Unit,
    onUserClick: (String) -> Unit,
    onReply: (Comment) -> Unit
) {
    val isLikedByMe = comment.likedBy.contains(currentUserId)
    val isReply = !comment.replyToCommentId.isNullOrEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 24.dp else 0.dp)
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = { /* Клик */ },
                onLongClick = { onReply(comment) }
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .clickable { onUserClick(comment.authorUsername.replace("@", "")) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.author.take(1).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.clickable { onUserClick(comment.authorUsername.replace("@", "")) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    comment.authorUsername,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, fontSize = 14.sp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Лайк",
                    tint = if (isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (comment.likesCount > 0) {
                Text(
                    text = comment.likesCount.toString(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
