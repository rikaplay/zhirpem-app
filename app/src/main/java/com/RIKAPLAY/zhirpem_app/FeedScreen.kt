package com.RIKAPLAY.zhirpem_app

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeedScreen(
    onUserClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMenuClick: () -> Unit,
    onAdminAccess: () -> Unit,
    onShowWhatsNew: () -> Unit,
    currentAvatarUrl: String?,
    currentName: String,
    showBackupWarning: Boolean,
    onNavigateToSecurity: () -> Unit,
    onDismissBackupWarning: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE) }
    
    var topNewsVersion by remember { mutableStateOf("") }
    val lastReadVersion = remember(topNewsVersion) { sharedPrefs.getString("last_read_news_version", "") ?: "" }
    
    LaunchedEffect(Unit) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("update_news")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    topNewsVersion = snap.documents.first().getString("version") ?: ""
                }
            }
    }

    val isNewsUnread = topNewsVersion.isNotEmpty() && topNewsVersion != lastReadVersion

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

    LaunchedEffect(selectedTab) {
        listState.animateScrollToItem(0)
    }

    val coroutineScope = rememberCoroutineScope()

    var followingList by remember { mutableStateOf(setOf<String>()) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    // val sharedPrefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
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
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        JumpingUpdateIcon(
                            onClick = onShowWhatsNew,
                            isJumping = isNewsUnread
                        )
                    }
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
            val glassEnabled = LocalGlassEnabled.current
            val glassAlpha = LocalGlassAlpha.current

            val density = LocalDensity.current
            var barWidth by remember { mutableFloatStateOf(0f) }
            var dragX by remember { mutableFloatStateOf(-1f) }
            var isPressing by remember { mutableStateOf(false) }

            // Вычисляем активный сегмент
            val segmentWidthPx = if (tabs.isNotEmpty() && barWidth > 0) barWidth / tabs.size else 0f
            val activeIndex = if (dragX != -1f && segmentWidthPx > 0) {
                (dragX / segmentWidthPx).toInt().coerceIn(0, tabs.size - 1)
            } else {
                selectedTabIndex
            }

            // Анимации для стеклянного индикатора (линзы)
            val targetOffsetPx = if (dragX != -1f) {
                dragX - segmentWidthPx / 2
            } else {
                segmentWidthPx * selectedTabIndex
            }

            val indicatorOffset by animateDpAsState(
                targetValue = with(density) { targetOffsetPx.coerceIn(0f, (barWidth - segmentWidthPx).coerceAtLeast(0f)).toDp() },
                animationSpec = if (animationsEnabled) spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow) else androidx.compose.animation.core.snap(),
                label = "tabIndicatorOffset"
            )

            // Размеры индикатора теперь соответствуют форме вкладки
            val indicatorWidth by animateDpAsState(
                targetValue = if (isPressing) with(density) { (segmentWidthPx - 16f).toDp() } else 0.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                label = "tabIndicatorWidth"
            )

            val indicatorHeight by animateDpAsState(
                targetValue = if (isPressing) 40.dp else 0.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                label = "tabIndicatorHeight"
            )

            val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (glassEnabled) glassAlpha else 0f,
                animationSpec = if (animationsEnabled) androidx.compose.animation.core.tween(400) else androidx.compose.animation.core.snap(),
                label = "glassAlpha"
            )

            val blurRadius by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (glassEnabled) 25f else 0f,
                animationSpec = if (animationsEnabled) androidx.compose.animation.core.tween(400) else androidx.compose.animation.core.snap(),
                label = "blurRadius"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .onGloballyPositioned { barWidth = it.size.width.toFloat() }
                    .pointerInput(tabs.size) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isPressing = true
                                dragX = offset.x
                            },
                            onDrag = { change, _ ->
                                dragX = change.position.x
                            },
                            onDragEnd = {
                                isPressing = false
                                if (dragX != -1f && segmentWidthPx > 0) {
                                    val finalIndex = (dragX / segmentWidthPx).toInt().coerceIn(0, tabs.size - 1)
                                    selectedTabIndex = finalIndex
                                }
                                dragX = -1f
                            },
                            onDragCancel = { 
                                isPressing = false
                                dragX = -1f 
                            }
                        )
                    }
            ) {
                // 1. СЛОЙ СТЕКЛА (ФОН)
                if (animatedAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .graphicsLayer {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && blurRadius > 0.1f) {
                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                        blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP
                                    ).asComposeRenderEffect()
                                }
                            }
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = animatedAlpha))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    )
                }

                // 2. ЖИДКАЯ ЛИНЗА (При касании) - СГЛАЖЕННЫЙ ПРЯМОУГОЛЬНИК
                if (isPressing && segmentWidthPx > 0) {
                    val segmentWidthDp = with(density) { segmentWidthPx.toDp() }
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset + (segmentWidthDp - indicatorWidth) / 2)
                            .width(indicatorWidth)
                            .height(indicatorHeight)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(20.dp)) // Форма как у выделения
                            .graphicsLayer {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                        15f, 15f, android.graphics.Shader.TileMode.CLAMP
                                    ).asComposeRenderEffect()
                                }
                            }
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    )
                }

                // 3. СЛОЙ КОНТЕНТА
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        val isTargeted = index == activeIndex

                        val bgTabColor = if (animationsEnabled) {
                            animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "bgTab").value
                        } else {
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        }

                        val textTabColor = if (animationsEnabled) {
                            animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), label = "textTab").value
                        } else {
                            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        }

                        // Анимация масштаба текста
                        val textScale by animateFloatAsState(
                            targetValue = if (isTargeted) 1.15f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                            label = "tabTextScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgTabColor)
                                .clickable {
                                    if (selectedTabIndex == index) {
                                        coroutineScope.launch { listState.animateScrollToItem(0) }
                                    } else {
                                        selectedTabIndex = index
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = textTabColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.scale(textScale)
                            )
                        }
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

@Composable
fun JumpingUpdateIcon(onClick: () -> Unit, isJumping: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "jumpingIcon")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                (0f) at 0
                if (isJumping) {
                    (-6f) at 300
                    (0f) at 600
                    (-3f) at 800
                    (0f) at 1000
                }
                (0f) at 2000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cached,
            contentDescription = "What's New",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
