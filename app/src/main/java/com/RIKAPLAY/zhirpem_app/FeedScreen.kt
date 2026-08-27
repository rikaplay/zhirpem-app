package com.RIKAPLAY.zhirpem_app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Call
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
    onDismissBackupWarning: () -> Unit,
    is2faEnabled: Boolean,
    isCallActive: Boolean = false,
    callDuration: Long = 0,
    onCallClick: () -> Unit = {}
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

    val isNewsUnread = (topNewsVersion.isNotEmpty()) && (topNewsVersion != lastReadVersion)

    // Проверка 2FA уведомления
    var showTotpWarning by remember { mutableStateOf(false) }
    val lastRemindedTotp = remember { sharedPrefs.getLong("last_reminded_totp", 0L) }
    
    LaunchedEffect(is2faEnabled, lastRemindedTotp) {
        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()
        showTotpWarning = !is2faEnabled && (currentTime - lastRemindedTotp > oneWeekMillis)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val viewModel: FeedViewModel = viewModel()
    val postsList by viewModel.postsList.collectAsState()
    val recommendedPosts by viewModel.recommendedPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val tabs = listOf("Для вас", "Вы читаете", "Популярное", "Медиа")
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = tabs[selectedTabIndex]

    val listState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()

    var followingList by remember { mutableStateOf(setOf<String>()) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val myUsername = sharedPrefs.getString("username", "") ?: ""

    LaunchedEffect(selectedTab) {
        listState.animateScrollToItem(0)
        if (selectedTab == "Для вас" && recommendedPosts.isEmpty() && myUsername.isNotEmpty()) {
            viewModel.fetchForYouPosts(myUsername, isRefresh = true)
        }
    }

    LaunchedEffect(myUsername) {
        if (myUsername.isNotEmpty()) {
            viewModel.fetchForYouPosts(myUsername, isRefresh = true)
            db.collection("follows")
                .whereEqualTo("follower", myUsername.lowercase())
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        followingList = it.documents.asSequence().mapNotNull { doc -> doc.getString("following") }.toSet()
                    }
                }
        }
    }

    val filteredPosts by remember(selectedTab, postsList, recommendedPosts, followingList) {
        derivedStateOf {
            val uniquePosts = postsList.distinctBy { it.id }
            when (selectedTab) {
                "Для вас" -> recommendedPosts.filter { !it.isAuthorBanned && it.communityId == null }
                "Медиа" -> uniquePosts.filter { ((it.isMedia || !it.imageUrl.isNullOrEmpty()) && !it.isAuthorBanned && it.communityId == null) }
                "Вы читаете" -> {
                    val followingSet = followingList.map { it.lowercase() }.toSet()
                    uniquePosts.asSequence().filter { 
                        val handle = it.handle.replace("@", "").lowercase()
                        followingSet.contains(handle) && !it.isAuthorBanned && it.communityId == null 
                    }.toList()
                }
                "Популярное" -> uniquePosts.asSequence().filter { !it.isAuthorBanned && it.communityId == null }.sortedByDescending { it.likes }.toList()
                else -> uniquePosts.filter { !it.isAuthorBanned && it.communityId == null }
            }
        }
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
                        contentAlignment = Alignment.Center,
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
                            .height(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCallActive) {
                            AnimatedCallIcon(onClick = onCallClick, duration = callDuration)
                        } else {
                            JumpingUpdateIcon(
                                onClick = onShowWhatsNew,
                                isJumping = isNewsUnread
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
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

            if (showTotpWarning) {
                TotpVulnerabilityBanner(
                    onNavigateToSecurity = onNavigateToSecurity,
                    onRemindLater = {
                        sharedPrefs.edit().putLong("last_reminded_totp", System.currentTimeMillis()).apply()
                        showTotpWarning = false
                    }
                )
            }

            // Tabs
            val animationsEnabled = LocalAnimationsEnabled.current
            val glassEnabled = LocalGlassEnabled.current
            val glassAlpha = LocalGlassAlpha.current

            val density = LocalDensity.current
            var barWidth by remember { mutableFloatStateOf(0f) }
            var dragX by remember { mutableFloatStateOf(-1f) }
            var isPressing by remember { mutableStateOf(value = false) }

            // Вычисляем активный сегмент
            val segmentWidthPx = if (barWidth > 0) barWidth / tabs.size else 0f
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

            val animatedAlpha by animateFloatAsState(
                targetValue = if (glassEnabled) glassAlpha else 0f,
                animationSpec = if (animationsEnabled) androidx.compose.animation.core.tween(400) else androidx.compose.animation.core.snap(),
                label = "glassAlpha"
            )

            val blurRadius by animateFloatAsState(
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
                            contentAlignment = Alignment.Center,
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
                if ((isRefreshing || isLoading) && postsList.isEmpty()) {
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
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            if (selectedTab == "Для вас") {
                                viewModel.fetchForYouPosts(myUsername, isRefresh = true)
                            } else {
                                viewModel.fetchPosts(isRefresh = true)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (filteredPosts.isEmpty() && !isLoading && !isRefreshing) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(bottom = 100.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text("📭", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (selectedTab == "Вы читаете") "Здесь пока нет постов от тех, на кого вы подписаны." else "Здесь пока пусто.",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 32.dp)
                                        )
                                    }
                                }
                            }

                            items(filteredPosts, key = { post -> post.id }) { post ->
                                AnimatedPostWrapper {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        PostItem(post = post, onUserClick = onUserClick, onHashtagClick = onHashtagClick)
                                    }
                                }
                            }

                            // Триггер загрузки новых постов при прокрутке до конца
                            item {
                                LaunchedEffect(Unit) {
                                    if (!isLoading) {
                                        if (selectedTab == "Для вас") {
                                            viewModel.fetchForYouPosts(myUsername)
                                        } else {
                                            viewModel.fetchPosts()
                                        }
                                    }
                                }
                                
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
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
}

@Composable
fun AnimatedPostWrapper(
    content: @Composable () -> Unit
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    if (!animationsEnabled) {
        content()
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutExpo),
        label = "postAlpha"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "postScale"
    )

    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "postTranslateY"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = translateY
            }
    ) {
        content()
    }
}

@Composable
fun TotpVulnerabilityBanner(
    onNavigateToSecurity: () -> Unit,
    onRemindLater: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B0000)), // Темно-красный
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "⚠️ Ваш аккаунт может быть уязвим",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Включите двухфакторную аутентификацию (TOTP) для максимальной защиты.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToSecurity,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("В настройки", color = Color(0xFF8B0000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onRemindLater,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Напомнить через неделю", fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AnimatedCallIcon(onClick: () -> Unit, duration: Long) {
    var showPhoneIcon by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            showPhoneIcon = true
            kotlinx.coroutines.delay(1000)
            showPhoneIcon = false
            kotlinx.coroutines.delay(3000)
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (showPhoneIcon) Color(0xFF4CAF50) else Color(0xFF1B5E20),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = showPhoneIcon,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            },
            label = "iconTransition"
        ) { isPhone ->
            if (isPhone) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                val minutes = duration / 60
                val seconds = duration % 60
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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
