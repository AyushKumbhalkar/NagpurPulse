// this is the ProfileScreen.kt file

// java/com/nagpurpulse/ui/screens/profile/ProfileScreen.kt

package com.nagpurpulse.ui.screens.profile

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.async
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.*
import com.nagpurpulse.data.repository.*
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: Profile? = null,
    val posts: List<Post> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val commentCount: Int = -1,
    val savedCount: Int = 0,
    val unreadNotifCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activeTab: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val savedPostsRepository: SavedPostsRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    val guestAvatarUrl: String?
        get() = authRepository.guestAvatarUrl

    val guestUsername: String?
        get() = authRepository.guestUsername

    init { refresh(showLoading = true) }

    /**
     * Load only the data needed for the current profile tab. Comments and saved posts
     * are fetched on demand instead of blocking every profile opening.
     */
    fun refreshProfile() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            profileRepository.getProfile(userId).onSuccess { profile ->
                _uiState.value = _uiState.value.copy(profile = profile)
            }
        }
    }

    fun refresh(showLoading: Boolean = false) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = showLoading && _uiState.value.profile == null,
                isRefreshing = !showLoading
            )
            try {
                val profileJob = async { profileRepository.getProfile(userId) }
                val postsJob = async { postRepository.getPostsByUser(userId) }
                val badgesJob = async { profileRepository.getBadges(userId) }
                val notifJob = async { notificationRepository.getUnreadCount(userId) }
                val savedIdsJob = async { savedPostsRepository.getSavedPostIds(userId) }
                profileJob.await().onSuccess { p -> _uiState.value = _uiState.value.copy(profile = p) }
                postsJob.await().onSuccess { p -> _uiState.value = _uiState.value.copy(posts = p) }
                badgesJob.await().onSuccess { b -> _uiState.value = _uiState.value.copy(badges = b) }
                notifJob.await().onSuccess { count -> _uiState.value = _uiState.value.copy(unreadNotifCount = count) }
                savedIdsJob.await().onSuccess { ids -> _uiState.value = _uiState.value.copy(savedCount = ids.size) }
                if (_uiState.value.activeTab == 1) loadComments(userId)
                if (_uiState.value.activeTab == 2) loadSavedPosts(userId)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    private suspend fun loadComments(userId: String) {
        postRepository.getCommentsByUser(userId).onSuccess { comments ->
            _uiState.value = _uiState.value.copy(comments = comments, commentCount = comments.size)
        }
    }

    private suspend fun loadSavedPosts(userId: String) {
        savedPostsRepository.getSavedPostIds(userId).onSuccess { ids ->
            postRepository.getSavedPosts(ids).onSuccess { posts ->
                _uiState.value = _uiState.value.copy(savedPosts = posts, savedCount = posts.size)
            }
        }
    }

    fun setActiveTab(tab: Int) {
        if (_uiState.value.activeTab == tab) return
        _uiState.value = _uiState.value.copy(activeTab = tab)
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            when (tab) {
                1 -> loadComments(userId)
                2 -> loadSavedPosts(userId)
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch { authRepository.signOut(); onSuccess() }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId).fold(
                onSuccess = {
                    _uiState.value =
                        _uiState.value.copy(posts = _uiState.value.posts.filter { it.id != postId })
                },
                onFailure = {}
            )
        }
    }

    fun toggleSave(postId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            savedPostsRepository.unsavePost(userId, postId)
            _uiState.value =
                _uiState.value.copy(savedPosts = _uiState.value.savedPosts.filter { it.id != postId })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshProfile()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val guestAvatarUrl = viewModel.guestAvatarUrl
    val guestUsername = viewModel.guestUsername

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Premium logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),
            icon = {
                Box(
                    Modifier.size(54.dp).clip(CircleShape)
                        .background(RedAlert.copy(alpha = 0.12f))
                        .border(1.dp, RedAlert.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = RedAlert, modifier = Modifier.size(24.dp))
                }
            },
            title = { Text("Sign out?", color = Color(0xFFF7F3F5), fontWeight = FontWeight.Bold) },
            text = { Text("You'll need to sign in again to post or comment.", color = Color(0xFFC7C0CA)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        android.util.Log.e("AYUSH_LOGOUT", "BUTTON CLICKED")
                        onLogout()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
                ) { Text("Sign out", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Stay signed in", color = Color(0xFFC7C0CA))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.weight(1f))

                // Notification bell
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale(onClick = onNotifications),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    if (uiState.unreadNotifCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary)
                                .align(Alignment.TopEnd)
                                .offset(x = (-3).dp, y = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale { navController.navigate(com.nagpurpulse.ui.navigation.Screen.Settings.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = { BottomNavBar(navController = navController, onCreatePost = onCreatePost) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues), Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return@Scaffold
        }

        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isRefreshing),
            onRefresh = { viewModel.refresh(showLoading = false) },
            indicator = { state, trigger -> com.google.accompanist.swiperefresh.SwipeRefreshIndicator(state, trigger, contentColor = OrangePrimary) }
        ) {
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                ProfileHeader(
                    profile = uiState.profile,
                    guestAvatarUrl = guestAvatarUrl,
                    guestUsername = guestUsername,
                    badges = uiState.badges,
                    postCount = uiState.posts.size,
                    savedCount = uiState.savedCount,
                    onEditProfile = {
                        navController.navigate(Screen.AccountProfile.route)
                    }
                )
            }

            // ── Tabs ─────────────────────────────────────────────────────────
            item {
                val tabs = listOf(
                    "Threads (${uiState.posts.size})",
                    if (uiState.commentCount >= 0) "Comments (${uiState.commentCount})" else "Comments",
                    "Saved (${uiState.savedCount})"
                )
                TabRow(
                    selectedTabIndex = uiState.activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = OrangePrimary,
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[uiState.activeTab])
                                .height(2.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            OrangePrimary,
                                            OrangeLight
                                        )
                                    )
                                )
                        )
                    },
                    divider = {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { i, label ->
                        Tab(
                            selected = uiState.activeTab == i,
                            onClick = { viewModel.setActiveTab(i) },
                            text = {
                                Text(
                                    label,
                                    color = if (uiState.activeTab == i) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (uiState.activeTab == i) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                }
            }

            // ── Tab content ──────────────────────────────────────────────────
            when (uiState.activeTab) {
                0 -> {
                    if (uiState.posts.isEmpty()) {
                        item {
                            EmptyTabState(
                                Icons.Filled.ChatBubbleOutline,
                                "No posts yet\nShare what's happening in Nagpur!"
                            )
                        }
                    } else {
                        itemsIndexed(uiState.posts) { i, item ->
                            StaggeredItem(i) {
                                PostCard(
                                    post = item,
                                    onClick = { onPostClick(item.id) },
                                    isOwnPost = true,

                                    onEdit = {
                                        navController.navigate(
                                            Screen.CreatePost.createEditRoute(item.id)
                                        )
                                    },

                                    onDelete = { viewModel.deletePost(item.id) },
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp)
                                        .padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    if (uiState.comments.isEmpty()) {
                        item {
                            EmptyTabState(
                                Icons.Filled.ChatBubbleOutline,
                                "No comments yet\nJoin the conversation!"
                            )
                        }
                    } else {
                        itemsIndexed(uiState.comments) { i, item ->
                            StaggeredItem(i) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp)
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            "On a thread:",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Text(
                                            item.body,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            item.timeAgo(),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (uiState.savedPosts.isEmpty()) {
                        item {
                            EmptyTabState(
                                Icons.Filled.BookmarkBorder,
                                "Nothing saved yet\nTap the bookmark icon on any post to save it"
                            )
                        }
                    } else {
                        itemsIndexed(uiState.savedPosts) { i, item ->
                            StaggeredItem(i) {
                                PostCard(
                                    post = item,
                                    onClick = { onPostClick(item.id) },
                                    isSaved = true,
                                    onToggleSave = { viewModel.toggleSave(item.id) },
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp)
                                        .padding(top = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: Profile?,
    guestAvatarUrl: String?,
    guestUsername: String?,
    badges: List<Badge>,
    postCount: Int,
    savedCount: Int,
    onEditProfile: () -> Unit
) {
    val displayName =
        profile?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: profile?.username
            ?: guestUsername?.takeIf { it.isNotBlank() }
            ?: "NagpurUser"


    val avatarUrl =
        profile?.avatarUrl
            ?: guestAvatarUrl
            ?: "https://api.dicebear.com/7.x/avataaars/png?seed=$displayName"

    // val bannerUrl = profile?.coverUrl

    // android.util.Log.d("PROFILE_UI", "banner=$bannerUrl")


    // Staggered entrance
    var av by remember { mutableStateOf(false) }
    var nv by remember { mutableStateOf(false) }
    var sv by remember { mutableStateOf(false) }
    var bv by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60); av = true
        delay(100); nv = true
        delay(80); sv = true
        delay(80); bv = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OrangePrimary.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Background city motif (subtle)
        /*

        if (!bannerUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(bannerUrl)
                    .crossfade(true)
                    .listener(
                        onSuccess = { _, _ ->
                            android.util.Log.d("BANNER_LOAD", "SUCCESS")
                        },
                        onError = { _, result ->
                            android.util.Log.e(
                                "BANNER_LOAD",
                                result.throwable.message ?: "ERROR"
                            )
                        }
                    )
                    .build(),
                contentDescription = "Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                OrangePrimary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        */




        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with glow ring
            AnimatedVisibility(
                av,
                enter = fadeIn(tween(500)) + scaleIn(
                    initialScale = 0.65f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy)
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Glow
                    Box(
                        modifier = Modifier
                            .size(132.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary.copy(0.12f))
                    )
                    // Ring
                    Box(
                        modifier = Modifier
                            .size(122.dp)
                            .border(
                                2.5.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        OrangePrimary,
                                        OrangeLight,
                                        OrangePrimary.copy(0.3f),
                                        OrangePrimary
                                    )
                                ),
                                CircleShape
                            )
                            .clip(CircleShape)
                    )
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .listener(
                                onSuccess = { _, _ ->
                                    android.util.Log.d("AVATAR_LOAD", "SUCCESS")
                                },
                                onError = { _, result ->
                                    android.util.Log.e(
                                        "AVATAR_LOAD",
                                        result.throwable.message ?: "ERROR"
                                    )
                                }
                            )
                            .build(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    // Edit button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(OrangePrimary)
                            .align(Alignment.BottomEnd)
                            .pressScale(onClick = onEditProfile),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Name + meta
            AnimatedVisibility(nv, enter = fadeIn(tween(400)) + slideInVertically { 20 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val profileScreenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                    Text(
                        displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (profileScreenWidth < 360) 23.sp else if (profileScreenWidth < 400) 26.sp else 28.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "u/${profile?.username ?: ""}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (profile?.isVerified == true) {
                            Spacer(Modifier.width(5.dp))

                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = OrangePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (!profile?.tagline.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))


                        Text(
                            profile!!.tagline!!,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            buildString {
                                if (!profile?.location.isNullOrBlank()) {
                                    append(profile.location)
                                }

                                val joinedText = profile?.memberSince()
                                    ?.replace("Member since ", "Joined ")

                                if (!joinedText.isNullOrBlank()) {
                                    if (!profile?.location.isNullOrBlank()) {
                                        append(" • ")
                                    }
                                    append(joinedText)
                                }
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(10.dp))


                }
            }

            Spacer(Modifier.height(20.dp))

            // Stats row
            AnimatedVisibility(sv, enter = fadeIn(tween(400)) + slideInVertically { 30 }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(profile?.karma ?: 0, "Karma", OrangePrimary, Modifier.weight(1f))
                    StatCard(postCount, "Threads", BlueInfo, Modifier.weight(1f))
                    StatCard(savedCount, "Saved", GreenSuccess, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))



            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "Edit Profile",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                bv,
                enter = fadeIn(tween(400)) + slideInVertically { 25 }
            ) {
                Column {

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Earned Badges",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            Modifier.pressScale(onClick = {}),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "View all",
                                color = OrangePrimary,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    val displayBadges: List<Triple<String, ImageVector, String>> = if (badges.isEmpty()) {
                        listOf(
                            Triple("food_expert", Icons.Filled.Restaurant, "Food Expert"),
                            Triple("night_owl", Icons.Filled.Nightlight, "Night Owl"),
                            Triple("top_contributor", Icons.Filled.LocalFireDepartment, "Top Contributor"),
                            Triple("trend_spotter", Icons.Filled.Star, "Trend Spotter")
                        )
                    } else {
                        badges.mapNotNull { badge ->
                            val icon = when (badge.badgeType) {
                                "food_expert"     -> Icons.Filled.Restaurant
                                "night_owl"      -> Icons.Filled.Nightlight
                                "top_contributor" -> Icons.Filled.LocalFireDepartment
                                "trend_spotter"  -> Icons.Filled.Star
                                else             -> null
                            }

                            icon?.let {
                                Triple(
                                    badge.badgeType,
                                    it,
                                    badge.displayName()
                                )
                            }
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayBadges.forEachIndexed { i, (type, icon, label) ->
                            var cv by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                delay(i * 80L)
                                cv = true
                            }

                            AnimatedVisibility(
                                cv,
                                enter = fadeIn(tween(300)) +
                                        scaleIn(
                                            initialScale = 0.7f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioHighBouncy
                                            )
                                        )
                            ) {
                                BadgeChip(icon, label, type)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

        }
    }
}

@Composable
private fun StatCard(value: Int, label: String, accentColor: Color, modifier: Modifier = Modifier) {
    val animated by animateIntAsState(
        value,
        tween(1000, easing = FastOutSlowInEasing),
        label = "stat_$label"
    )
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(250); visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "stat_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accentColor.copy(0.30f), RoundedCornerShape(14.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatCount(animated),
                color = accentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BadgeChip(icon: ImageVector, label: String, badgeType: String) {
    val bg = when (badgeType) {
        "food_expert" -> OrangePrimary.copy(alpha = 0.15f)
        "night_owl" -> MaterialTheme.colorScheme.primaryContainer
        "top_contributor" -> OrangePrimary.copy(alpha = 0.12f)
        "trend_spotter" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyTabState(icon: ImageVector, text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(48.dp), Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
