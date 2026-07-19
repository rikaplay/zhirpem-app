package com.RIKAPLAY.zhirpem_app

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onUserClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onAdminAccess: () -> Unit,
    currentAvatarUrl: String?,
    currentName: String,
    showBackupWarning: Boolean,
    onNavigateToSecurity: () -> Unit,
    onDismissBackupWarning: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val viewModel: FeedViewModel = viewModel()
    val postsList by viewModel.postsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val tabs = listOf("Для вас", "Вы читаете", "Популярное", "Медиа")
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = tabs[selectedTabIndex]

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var followingList by remember { mutableStateOf(setOf<String>()) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val sharedPrefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            db.collection("follows")
                .whereEqualTo("follower", myUsername)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        followingList = snapshot.documents.mapNotNull { it.getString("following") }.toSet()
                    }
                }
        }
    }

    val filteredPosts = when (selectedTab) {
        "Медиа" -> postsList.filter { (it.isMedia || !it.imageUrl.isNullOrEmpty()) && !it.isAuthorBanned && it.communityId == null }
        "Вы читаете" -> postsList.filter { followingList.contains(it.handle.replace("@", "")) && !it.isAuthorBanned && it.communityId == null }
        "Популярное" -> postsList.filter { !it.isAuthorBanned && it.communityId == null }.sortedByDescending { it.likes }
        else -> postsList.filter { !it.isAuthorBanned && it.communityId == null }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { ZhirpemLogo(onAdminAccess = onAdminAccess) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = currentAvatarUrl,
                                contentDescription = "Аватар",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(currentName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                },
                actions = {
                    Box(modifier = Modifier.size(40.dp).padding(end = 12.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (showBackupWarning) {
                BackupCodeWarningBanner(
                    onCreateCode = onNavigateToSecurity,
                    onDismiss = onDismissBackupWarning
                )
            }

            // Tabs
            val animationsEnabled = LocalAnimationsEnabled.current
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index

                    val bgTabColor = if (animationsEnabled) {
                        androidx.compose.animation.animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "bgTab").value
                    } else {
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    }

                    val textTabColor = if (animationsEnabled) {
                        androidx.compose.animation.animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), label = "textTab").value
                    } else {
                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgTabColor)
                            .clickable {
                                selectedTabIndex = index
                                coroutineScope.launch { listState.scrollToItem(0) }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = tab, color = textTabColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // List
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(5) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ShimmerPostItem()
                            }
                        }
                    }
                } else if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                } else if (filteredPosts.isEmpty()) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Здесь пока пусто.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredPosts, key = { post -> post.id }) { post ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                PostItem(post = post, onUserClick = onUserClick, onHashtagClick = onHashtagClick)
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}
