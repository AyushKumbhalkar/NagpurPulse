// java/com/nagpurpulse/ui/screens/explore/CategoryPostsScreen.kt
//
// NOTE: This reuses `PostCard` from com.nagpurpulse.ui.components — the post
// card composable you shared (PostCard.kt). If your project's actual file is
// named SmartPostCard with a different composable name, just swap the
// reference in CategoryPostsFeed() below; everything else stays the same.

package com.nagpurpulse.ui.screens.explore


import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.SavedPostsRepository
import com.nagpurpulse.ui.components.*   // PostCard, ShimmerPostCard, StaggeredItem, pressScale, categoryColor, formatCount
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ───────────────────────────────────────────────────────────────

data class CategoryPostsUiState(
    val posts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val userVotes: Map<String, String?> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val sortBy: String = "top",
    val postCount: Int = 0
)

// ── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class CategoryPostsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val savedPostsRepository: SavedPostsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryPostsUiState())
    val uiState: StateFlow<CategoryPostsUiState> = _uiState

    private var category: String = ""

    /** Called once from the screen with the nav-arg category. Safe to call again — no-ops if unchanged. */
    fun setCategory(newCategory: String) {
        if (category == newCategory) return
        category = newCategory
        loadPosts()
    }

    fun loadPosts(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !refresh,
                isRefreshing = refresh,
                error = null
            )
            postRepository.getPosts(
                category = category,
                sortBy = _uiState.value.sortBy
            ).fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts,
                        postCount = posts.size,
                        isLoading = false,
                        isRefreshing = false
                    )
                    loadSavedPostIds()
                    loadUserVotes()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private suspend fun loadSavedPostIds() {
        val userId = authRepository.currentUserId ?: return
        savedPostsRepository.getSavedPostIds(userId).fold(
            onSuccess = { ids -> _uiState.value = _uiState.value.copy(savedPostIds = ids.toSet()) },
            onFailure = {}
        )
    }

    private suspend fun loadUserVotes() {
        val userId = authRepository.currentUserId ?: return
        val votes = mutableMapOf<String, String?>()
        _uiState.value.posts.forEach { post ->
            postRepository.getUserVote(userId, post.id).getOrNull()?.let { vote ->
                votes[post.id] = vote
            }
        }
        _uiState.value = _uiState.value.copy(userVotes = votes)
    }

    fun setSortBy(sort: String) {
        if (_uiState.value.sortBy == sort) return
        _uiState.value = _uiState.value.copy(sortBy = sort)
        loadPosts()
    }

    fun toggleSave(postId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val isSaved = postId in _uiState.value.savedPostIds
            if (isSaved) savedPostsRepository.unsavePost(userId, postId)
            else savedPostsRepository.savePost(userId, postId)
            loadSavedPostIds()
        }
    }

    fun vote(postId: String, voteType: String) {
        val currentVote = _uiState.value.userVotes[postId]
        val userId = authRepository.currentUserId ?: return

        // Optimistic local update — mirrors HomeViewModel's vote() behavior.
        _uiState.value = _uiState.value.copy(
            posts = _uiState.value.posts.map { post ->
                if (post.id != postId) return@map post
                when {
                    voteType == "up" && currentVote == "up" ->
                        post.copy(upvotes = (post.upvotes - 1).coerceAtLeast(0))

                    voteType == "down" && currentVote == "down" ->
                        post.copy(downvotes = (post.downvotes - 1).coerceAtLeast(0))

                    voteType == "up" && currentVote == "down" ->
                        post.copy(
                            upvotes = post.upvotes + 1,
                            downvotes = (post.downvotes - 1).coerceAtLeast(0)
                        )

                    voteType == "down" && currentVote == "up" ->
                        post.copy(
                            upvotes = (post.upvotes - 1).coerceAtLeast(0),
                            downvotes = post.downvotes + 1
                        )

                    voteType == "up" -> post.copy(upvotes = post.upvotes + 1)
                    voteType == "down" -> post.copy(downvotes = post.downvotes + 1)
                    else -> post
                }
            },
            userVotes = _uiState.value.userVotes.toMutableMap().apply {
                when {
                    voteType == "up" && currentVote == "up" -> put(postId, null)
                    voteType == "down" && currentVote == "down" -> put(postId, null)
                    else -> put(postId, voteType)
                }
            }
        )

        viewModelScope.launch {
            postRepository.votePost(userId, postId, voteType)
        }
    }

    fun getCurrentUserId(): String? = authRepository.currentUserId

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
            loadPosts(refresh = true)
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────

@Composable
fun CategoryPostsScreen(
    category: String,
    navController: NavController,
    viewModel: CategoryPostsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(category) {
        viewModel.setCategory(category)
    }

    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        headerVisible = true
    }

    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                CategoryTopBar(
                    category = category,
                    onBack = { navController.popBackStack() },
                    onSearch = { navController.navigate(Screen.Explore.route) },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out the ${categoryDisplayName(category)} community on NagpurPulse!"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    onReport = {
                        Toast.makeText(context, "Reported", Toast.LENGTH_SHORT).show()
                    }
                )

                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(380)) + slideInVertically { -20 }
                ) {
                    Column {
                        CategoryHeaderSection(
                            category = category,
                            postCount = uiState.postCount
                        )

                        CategorySortTabs(
                            selected = uiState.sortBy,
                            onSelected = viewModel::setSortBy,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        Spacer(Modifier.height(2.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.loadPosts(refresh = true) },
            modifier = Modifier.padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> CategoryLoadingState()

                uiState.posts.isEmpty() -> CategoryEmptyState(
                    category = category,
                    onCreatePost = { navController.navigate(Screen.CreatePost.route) }
                )

                else -> CategoryPostsFeed(
                    posts = uiState.posts,
                    savedPostIds = uiState.savedPostIds,
                    currentUserId = viewModel.getCurrentUserId(),
                    onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) },
                    onUserClick = { navController.navigate(Screen.UserProfile.createRoute(it)) },
                    onUpvote = { viewModel.vote(it, "up") },
                    onDownvote = { viewModel.vote(it, "down") },
                    onToggleSave = { viewModel.toggleSave(it) },
                    onEdit = { navController.navigate(Screen.CreatePost.createEditRoute(it)) },
                    onDelete = { viewModel.deletePost(it) }
                )
            }
        }
    }
}

// ── Top App Bar ────────────────────────────────────────────────────────────

@Composable
private fun CategoryTopBar(
    category: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pressScale(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryDisplayName(category),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Posts from the ${categoryDisplayName(category)} category",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Search
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pressScale(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Overflow menu
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pressScale(onClick = { menuExpanded = true }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    text = { Text("Share category", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { menuExpanded = false; onShare() }
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                    text = { Text("Report an issue", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { menuExpanded = false; onReport() }
                )
            }
        }
    }
}

// ── Category Header (subreddit-style banner) ──────────────────────────────

@Composable
private fun CategoryHeaderSection(
    category: String,
    postCount: Int,
    modifier: Modifier = Modifier
) {
    val accent = categoryColor(category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = categoryDisplayName(category),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${formatCount(postCount)} ${if (postCount == 1) "post" else "posts"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Sort Tabs — sliding pill indicator ─────────────────────────────────────

@Composable
private fun CategorySortTabs(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember { listOf("top" to "Top", "new" to "New", "hot" to "Hot") }
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / options.size

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "sort_indicator_offset"
        )

        // Sliding pill background
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(40.dp)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(OrangeSubtle)
        )

        // Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEach { (key, label) ->
                val isSelected = key == selected
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelected(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

// ── Feed ────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryPostsFeed(
    posts: List<Post>,
    savedPostIds: Set<String>,
    currentUserId: String?,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DensityManager.cardPadding.dp,
            vertical = DensityManager.itemSpacing.dp
        ),
        verticalArrangement = Arrangement.spacedBy(DensityManager.itemSpacing.dp)
    ) {
        itemsIndexed(items = posts, key = { _, post -> post.id }) { index, post ->
            StaggeredItem(index = index) {
                PostCard(
                    post = post,
                    isSaved = post.id in savedPostIds,
                    isOwnPost = post.userId == currentUserId,
                    onClick = { onPostClick(post.id) },
                    onUserClick = onUserClick,
                    onUpvote = { onUpvote(post.id) },
                    onDownvote = { onDownvote(post.id) },
                    onToggleSave = { onToggleSave(post.id) },
                    onEdit = { onEdit(post.id) },
                    onDelete = { onDelete(post.id) }
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Loading State — shimmer, matches HomeScreen ────────────────────────────

@Composable
private fun CategoryLoadingState(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DensityManager.cardPadding.dp,
            vertical = DensityManager.itemSpacing.dp
        ),
        verticalArrangement = Arrangement.spacedBy(DensityManager.itemSpacing.dp)
    ) {
        items(6) {
            ShimmerPostCard()
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryEmptyState(
    category: String,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "No posts yet in this category",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Be the first to share something in ${categoryDisplayName(category)}!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(OrangeSubtle)
                    .border(1.dp, OrangePrimary.copy(0.5f), RoundedCornerShape(22.dp))
                    .pressScale(onClick = onCreatePost)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "+ Create Post",
                    color = OrangePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun categoryDisplayName(category: String): String {
    if (category.isBlank()) return "Posts"
    return category.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "community" -> Icons.Filled.Groups
    "food" -> Icons.Filled.Restaurant
    "events" -> Icons.Filled.Event
    "traffic" -> Icons.Filled.Traffic
    "alerts" -> Icons.Filled.Campaign
    "nightlife" -> Icons.Filled.Nightlife
    "jobs" -> Icons.Filled.Work
    "weather" -> Icons.Filled.Cloud
    "civic", "infrastructure" -> Icons.Filled.AccountBalance
    "sports" -> Icons.Filled.EmojiEvents
    else -> Icons.Filled.LocationOn
}