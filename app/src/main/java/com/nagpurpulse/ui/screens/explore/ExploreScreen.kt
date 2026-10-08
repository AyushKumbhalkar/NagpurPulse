// java/com/nagpurpulse/ui/screens/explore/ExploreScreen.kt

package com.nagpurpulse.ui.screens.explore

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.R
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.ProfileRepository
import com.nagpurpulse.ui.components.CategoryBadge
import com.nagpurpulse.ui.components.EmptyState
import com.nagpurpulse.ui.components.ErrorState
import com.nagpurpulse.ui.components.LivePulse
import com.nagpurpulse.ui.components.ShimmerPostCard
import com.nagpurpulse.ui.components.SmartPostCard
import com.nagpurpulse.ui.components.StaggeredItem
import com.nagpurpulse.ui.components.categoryColor
import com.nagpurpulse.ui.components.categoryDisplayName
import com.nagpurpulse.ui.components.categoryIcon
import com.nagpurpulse.ui.components.formatCount
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.screens.onboarding.nagpurAreas
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.OrangeSubtle
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.RedAlert
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.TertiaryText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.OffsetDateTime
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════════════════
//  UI STATE
// ══════════════════════════════════════════════════════════════════════════════

/** One category tile: [key] is the raw category (e.g. "lost_found"), which is what the nav route expects. */
data class CategoryStat(val key: String, val total: Int, val today: Int)

/** One area card. */
data class AreaStat(val name: String, val count: Int, val isYours: Boolean)

data class ExploreUiState(
    // Discovery
    val hero: Post? = null,
    val categories: List<CategoryStat> = emptyList(),
    val areas: List<AreaStat> = emptyList(),
    val hotPosts: List<Post> = emptyList(),
    val freshPosts: List<Post> = emptyList(),
    val morePosts: List<Post> = emptyList(),
    val newSinceLastVisit: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadError: Boolean = false,
    // Search / area results
    val results: List<Post> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchError: Boolean = false,
    val activeArea: String? = null,
    val recentSearches: List<String> = emptyList()
) {
    val hasDiscoveryContent: Boolean
        get() = hero != null || hotPosts.isNotEmpty() || freshPosts.isNotEmpty() || morePosts.isNotEmpty()
}

// ══════════════════════════════════════════════════════════════════════════════
//  VIEW MODEL
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ExploreUiState(recentSearches = readRecents()))
    val uiState: StateFlow<ExploreUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    /** Baseline for "new since your last visit"; captured once so the pill stays stable during the session. */
    private val lastVisitMs: Long = prefs.getLong(KEY_LAST_VISIT, 0L)
    private var loadedOnce = false
    private var searchJob: Job? = null
    private var lastSearched: String = ""

    init {
        load()
        viewModelScope.launch {
            _query.debounce(350).filter { it.isNotBlank() }.collect { q -> search(q) }
        }
    }

    // ── Discovery ────────────────────────────────────────────────────────────

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && !it.hasDiscoveryContent,
                    isRefreshing = refresh,
                    loadError = false
                )
            }

            // Fetch in parallel. Previously these ran one after the other.
            val (newRes, hotRes, userAreas) = coroutineScope {
                val n = async { postRepository.getPosts(sortBy = "new", pageSize = 60) }
                val h = async { postRepository.getPosts(sortBy = "hot", pageSize = 30) }
                val a = async {
                    authRepository.currentUserId
                        ?.let { id -> profileRepository.getProfile(id).getOrNull()?.areas }
                        .orEmpty()
                }
                Triple(n.await(), h.await(), a.await())
            }

            val newPosts = newRes.getOrNull()
            val hotPosts = hotRes.getOrNull()

            if (newPosts == null && hotPosts == null) {
                // Keep whatever we already show on a failed refresh; only show the error state when empty.
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, loadError = !it.hasDiscoveryContent)
                }
                return@launch
            }

            val discovery = buildDiscovery(
                newPosts = newPosts.orEmpty(),
                hotPosts = hotPosts.orEmpty(),
                userAreas = userAreas,
                lastVisitMs = lastVisitMs
            )
            loadedOnce = true
            _uiState.update {
                it.copy(
                    hero = discovery.hero,
                    categories = discovery.categories,
                    areas = discovery.areas,
                    hotPosts = discovery.hot,
                    freshPosts = discovery.fresh,
                    morePosts = discovery.more,
                    newSinceLastVisit = discovery.newSince,
                    isLoading = false,
                    isRefreshing = false,
                    loadError = false
                )
            }
        }
    }

    /** Called when the user leaves Explore, so the next visit can show "N new since your last visit". */
    fun markVisited() {
        if (!loadedOnce) return
        prefs.edit().putLong(KEY_LAST_VISIT, System.currentTimeMillis()).apply()
    }

    // ── Search ───────────────────────────────────────────────────────────────

    fun onQueryChange(q: String) {
        _query.value = q
    }

    /** Explicit submit (keyboard "search" action): searches right away and remembers the term. */
    fun submitSearch() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        saveRecent(q)
        search(q, force = true)
    }

    fun search(q: String, force: Boolean = false) {
        val term = q.trim()
        if (term.isEmpty()) return
        // The debounce and an explicit submit can both fire for the same text; only run once.
        if (!force && term == lastSearched && _uiState.value.hasSearched) return
        lastSearched = term
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isSearching = true, hasSearched = true, searchError = false, activeArea = null)
            }
            postRepository.searchPosts(term).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(results = results, isSearching = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(results = emptyList(), isSearching = false, searchError = true) }
                }
            )
        }
    }

    fun openArea(area: String) {
        searchJob?.cancel()
        _query.value = ""
        lastSearched = ""
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeArea = area,
                    isSearching = true,
                    hasSearched = false,
                    searchError = false,
                    results = emptyList()
                )
            }
            postRepository.getPostsByArea(area).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(results = results, isSearching = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isSearching = false, searchError = true) }
                }
            )
        }
    }

    fun retryArea() {
        _uiState.value.activeArea?.let { openArea(it) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _query.value = ""
        lastSearched = ""
        _uiState.update {
            it.copy(
                results = emptyList(),
                hasSearched = false,
                isSearching = false,
                searchError = false,
                activeArea = null
            )
        }
    }

    // ── Recent searches ──────────────────────────────────────────────────────

    fun saveRecent(term: String) {
        val t = term.trim().replace('\n', ' ')
        if (t.isEmpty()) return
        val updated = (listOf(t) + _uiState.value.recentSearches.filterNot { it.equals(t, true) })
            .take(MAX_RECENTS)
        writeRecents(updated)
    }

    fun removeRecent(term: String) {
        writeRecents(_uiState.value.recentSearches.filterNot { it == term })
    }

    fun clearRecents() = writeRecents(emptyList())

    private fun writeRecents(list: List<String>) {
        prefs.edit().putString(KEY_RECENTS, list.joinToString("\n")).apply()
        _uiState.update { it.copy(recentSearches = list) }
    }

    private fun readRecents(): List<String> =
        prefs.getString(KEY_RECENTS, "").orEmpty().split('\n').filter { it.isNotBlank() }

    private companion object {
        const val PREFS = "explore_prefs"
        const val KEY_RECENTS = "recent_searches"
        const val KEY_LAST_VISIT = "last_visit_ms"
        const val MAX_RECENTS = 8
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DISCOVERY DATA (pure functions, no I/O)
// ══════════════════════════════════════════════════════════════════════════════

private class Discovery(
    val hero: Post?,
    val categories: List<CategoryStat>,
    val areas: List<AreaStat>,
    val hot: List<Post>,
    val fresh: List<Post>,
    val more: List<Post>,
    val newSince: Int
)

/** Categories that always get a tile, so the screen is useful even on a quiet day. */
private val knownCategories = listOf(
    "events", "food", "jobs", "college", "nightlife",
    "neighborhoods", "lost_found", "traffic", "rants", "alerts"
)

private fun Post.createdInstant(): Instant? = try {
    OffsetDateTime.parse(createdAt).toInstant()
} catch (e: Exception) {
    try {
        Instant.parse(createdAt)
    } catch (e2: Exception) {
        null
    }
}

private fun Post.isWithinHours(hours: Long): Boolean {
    val created = createdInstant() ?: return false
    return Duration.between(created, Instant.now()).toHours() < hours
}

private fun buildDiscovery(
    newPosts: List<Post>,
    hotPosts: List<Post>,
    userAreas: List<String>,
    lastVisitMs: Long
): Discovery {
    val pool = (newPosts + hotPosts).distinctBy { it.id }

    // Hero: the most relevant "happening now" item.
    val hero = pool.firstOrNull { it.isAlert && it.isWithinHours(24) }
        ?: newPosts.firstOrNull { it.category == "events" && it.isWithinHours(72) }
        ?: pool.filter { it.isWithinHours(24) }
            .maxByOrNull { it.upvotes + it.commentCount * 2 }
        ?: hotPosts.firstOrNull()

    val used = mutableSetOf<String>()
    hero?.let { used += it.id }

    val hot = hotPosts.filter { it.id !in used }.take(5)
    used += hot.map { it.id }
    val fresh = newPosts.filter { it.id !in used }.take(5)
    used += fresh.map { it.id }
    val more = hotPosts.filter { it.id !in used }.take(10)

    // Category stats (real counts instead of a plain label).
    val byCategory = pool.filter { it.category.isNotBlank() }.groupBy { it.category }
    val categoryKeys = (knownCategories + byCategory.keys).distinct()
    val categories = categoryKeys
        .map { key ->
            val posts = byCategory[key].orEmpty()
            CategoryStat(key, total = posts.size, today = posts.count { it.isWithinHours(24) })
        }
        .sortedWith(compareByDescending<CategoryStat> { it.today }.thenByDescending { it.total })

    // Areas: the user's own areas first, then the busiest ones.
    val mine = userAreas.map { it.trim() }.filter { it.isNotEmpty() }
    val ordered = (mine + nagpurAreas + pool.mapNotNull { it.areaTag?.trim() }.filter { it.isNotEmpty() })
        .distinctBy { it.lowercase() }
    val areas = ordered
        .map { name ->
            AreaStat(
                name = name,
                count = pool.count { it.areaTag?.trim().equals(name, ignoreCase = true) },
                isYours = mine.any { it.equals(name, ignoreCase = true) }
            )
        }
        .sortedWith(compareByDescending<AreaStat> { it.isYours }.thenByDescending { it.count })
        .take(8)

    val newSince = if (lastVisitMs > 0L) {
        newPosts.count { (it.createdInstant()?.toEpochMilli() ?: 0L) > lastVisitMs }
    } else 0

    return Discovery(hero, categories, areas, hot, fresh, more, newSince)
}

// ══════════════════════════════════════════════════════════════════════════════
//  SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ExploreScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Remember when the user left, for "new since your last visit".
    DisposableEffect(Unit) { onDispose { viewModel.markVisited() } }

    // System back: leave area results / search / keyboard before leaving the screen.
    BackHandler(enabled = uiState.activeArea != null || focused || query.isNotEmpty()) {
        focusManager.clearFocus()
        viewModel.clearSearch()
    }

    // Header entrance
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); headerVisible = true }

    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 }
    }
    val searchActive = focused || query.isNotEmpty() || uiState.activeArea != null
    val dividerAlpha by animateFloatAsState(
        targetValue = if (collapsed || searchActive) 1f else 0f,
        animationSpec = tween(200),
        label = "header_divider"
    )

    Scaffold(
        containerColor = Background,
        topBar = {
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { -30 })
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 200f))
                        .statusBarsPadding()
                ) {
                    // Greeting + title collapse away as soon as the user scrolls or searches.
                    AnimatedVisibility(visible = !searchActive && !collapsed) {
                        Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp)) {
                            Text(
                                greetingText(),
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                stringResource(R.string.explore_title),
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                    ExploreSearchBar(
                        query = query,
                        focused = focused,
                        onQueryChange = viewModel::onQueryChange,
                        onFocusChange = { focused = it },
                        onSubmit = {
                            haptic.tap()
                            viewModel.submitSearch()
                            focusManager.clearFocus()
                        },
                        onClear = { haptic.tap(); viewModel.clearSearch() },
                        onCancel = {
                            haptic.tap()
                            focusManager.clearFocus()
                            viewModel.clearSearch()
                        }
                    )
                    HorizontalDivider(color = Divider.copy(alpha = dividerAlpha), thickness = 0.5.dp)
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onCreatePost = onCreatePost,
                onProfileClick = onProfileClick
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                // 1. Loading a search or an area
                uiState.isSearching -> {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) { items(5) { ShimmerPostCard() } }
                }

                // 2. Area results
                uiState.activeArea != null -> {
                    val area = uiState.activeArea.orEmpty()
                    ResultsList(
                        header = stringResource(R.string.explore_area_results_title, area),
                        results = uiState.results,
                        hasError = uiState.searchError,
                        emptyTitle = stringResource(R.string.explore_no_area_posts_title, area),
                        emptySubtitle = stringResource(R.string.explore_no_area_posts_subtitle),
                        onRetry = viewModel::retryArea,
                        onClear = { viewModel.clearSearch() },
                        onPostClick = onPostClick
                    )
                }

                // 3. Search results
                uiState.hasSearched && query.isNotEmpty() -> {
                    ResultsList(
                        header = stringResource(R.string.explore_results_for, uiState.results.size, query),
                        results = uiState.results,
                        hasError = uiState.searchError,
                        emptyTitle = stringResource(R.string.explore_no_results_title, query),
                        emptySubtitle = stringResource(R.string.explore_no_results_subtitle),
                        onRetry = { viewModel.search(query, force = true) },
                        onClear = {
                            focusManager.clearFocus()
                            viewModel.clearSearch()
                        },
                        onPostClick = { id ->
                            viewModel.saveRecent(query)
                            onPostClick(id)
                        }
                    )
                }

                // 4. Search bar focused, nothing typed yet: recents and shortcuts
                focused && query.isEmpty() -> {
                    SuggestionsPanel(
                        recents = uiState.recentSearches,
                        categories = uiState.categories,
                        areas = uiState.areas,
                        onRecentClick = { term ->
                            haptic.tap()
                            viewModel.onQueryChange(term)
                            viewModel.search(term, force = true)
                            focusManager.clearFocus()
                        },
                        onRemoveRecent = viewModel::removeRecent,
                        onClearRecents = viewModel::clearRecents,
                        onCategoryClick = { key ->
                            haptic.tap()
                            focusManager.clearFocus()
                            navController.navigate(Screen.CategoryPosts.createRoute(key))
                        },
                        onAreaClick = { name ->
                            haptic.tap()
                            focusManager.clearFocus()
                            viewModel.openArea(name)
                        }
                    )
                }

                // 5. First load
                uiState.isLoading -> DiscoverySkeleton()

                // 6. Nothing could be loaded
                uiState.loadError -> {
                    ErrorState(
                        message = stringResource(R.string.explore_error_message),
                        onRetry = { viewModel.load() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 7. Discovery
                else -> {
                    DiscoveryContent(
                        uiState = uiState,
                        listState = listState,
                        onRefresh = { haptic.tap(); viewModel.load(refresh = true) },
                        onPostClick = onPostClick,
                        onCategoryClick = { key ->
                            haptic.tap()
                            navController.navigate(Screen.CategoryPosts.createRoute(key))
                        },
                        onAreaClick = { name ->
                            haptic.tap()
                            viewModel.openArea(name)
                        }
                    )

                    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
                    AnimatedVisibility(
                        visible = showTop,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SurfaceAlt)
                                .border(1.dp, OrangePrimary.copy(0.4f), CircleShape)
                                .pressScale {
                                    haptic.tap()
                                    scope.launch { listState.animateScrollToItem(0) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.explore_scroll_top),
                                tint = OrangePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun greetingText(): String {
    val hour = remember { LocalTime.now().hour }
    return when {
        hour < 12 -> stringResource(R.string.explore_greeting_morning)
        hour < 17 -> stringResource(R.string.explore_greeting_afternoon)
        else -> stringResource(R.string.explore_greeting_evening)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SEARCH BAR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExploreSearchBar(
    query: String,
    focused: Boolean,
    onQueryChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit
) {
    val hints = listOf(
        stringResource(R.string.explore_hint_1),
        stringResource(R.string.explore_hint_2),
        stringResource(R.string.explore_hint_3),
        stringResource(R.string.explore_hint_4)
    )
    var hintIndex by remember { mutableStateOf(0) }
    val isEmpty = query.isEmpty()
    // Rotating placeholder: teaches people what they can search for.
    LaunchedEffect(isEmpty) {
        if (isEmpty) {
            while (true) {
                delay(3200)
                hintIndex = (hintIndex + 1) % hints.size
            }
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (focused) OrangePrimary else Divider,
        animationSpec = tween(200),
        label = "search_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(SurfaceAlt)
                .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = if (focused) OrangePrimary else TertiaryText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { onFocusChange(it.isFocused) },
                textStyle = MaterialTheme.typography.titleMedium.copy(color = PrimaryText),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { inner ->
                    Box {
                        if (isEmpty) {
                            Crossfade(
                                targetState = hintIndex,
                                animationSpec = tween(400),
                                label = "search_hint"
                            ) { i ->
                                Text(
                                    hints[i],
                                    color = TertiaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        inner()
                    }
                }
            )
            if (!isEmpty) {
                // 40dp touch target (the icon itself stays small).
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .pressScale(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.explore_clear_search),
                        tint = TertiaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (focused) {
            Text(
                stringResource(R.string.explore_cancel),
                color = OrangePrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .pressScale(onClick = onCancel)
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SEARCH / AREA RESULTS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultsList(
    header: String,
    results: List<Post>,
    hasError: Boolean,
    emptyTitle: String,
    emptySubtitle: String,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onPostClick: (String) -> Unit
) {
    when {
        hasError -> Box(Modifier.fillMaxSize()) {
            ErrorState(
                message = stringResource(R.string.explore_search_error),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        results.isEmpty() -> Box(Modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = emptyTitle,
                subtitle = emptySubtitle,
                ctaLabel = stringResource(R.string.explore_clear_cta),
                onCta = onClear,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "results_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        header,
                        color = SecondaryText,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .pressScale(onClick = onClear),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.explore_clear_search),
                            tint = TertiaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            itemsIndexed(results, key = { _, p -> p.id }) { i, post ->
                StaggeredItem(i) {
                    SmartPostCard(post = post, onClick = { onPostClick(post.id) })
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SUGGESTIONS (search bar focused, nothing typed)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SuggestionsPanel(
    recents: List<String>,
    categories: List<CategoryStat>,
    areas: List<AreaStat>,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onAreaClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        if (recents.isNotEmpty()) {
            item(key = "recents") {
                Column {
                    SectionHeader(
                        icon = Icons.Filled.History,
                        title = stringResource(R.string.explore_section_recent),
                        trailing = {
                            Text(
                                stringResource(R.string.explore_clear_all),
                                color = OrangePrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .pressScale(onClick = onClearRecents)
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    recents.forEach { term ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale { onRecentClick(term) }
                                .padding(start = 16.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = TertiaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                term,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .pressScale { onRemoveRecent(term) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.explore_remove_recent),
                                    tint = TertiaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "suggest_categories") {
            Column {
                SectionHeader(
                    icon = Icons.Filled.GridView,
                    title = stringResource(R.string.explore_browse_categories)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.take(8), key = { it.key }) { c ->
                        SuggestionChip(
                            label = categoryDisplayName(c.key),
                            icon = categoryIcon(c.key),
                            accent = categoryColor(c.key),
                            onClick = { onCategoryClick(c.key) }
                        )
                    }
                }
            }
        }

        item(key = "suggest_areas") {
            Column {
                SectionHeader(
                    icon = Icons.Filled.LocationOn,
                    title = stringResource(R.string.explore_jump_area)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(areas, key = { it.name }) { a ->
                        SuggestionChip(
                            label = a.name,
                            icon = Icons.Filled.LocationOn,
                            accent = OrangePrimary,
                            onClick = { onAreaClick(a.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.3f), shape)
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = PrimaryText,
            maxLines = 1,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  DISCOVERY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DiscoveryContent(
    uiState: ExploreUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onRefresh: () -> Unit,
    onPostClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAreaClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val swipeState = rememberSwipeRefreshState(uiState.isRefreshing)

    val showPill = uiState.newSinceLastVisit > 0
    val hasTop = showPill || uiState.hero != null
    // Items above "Fresh in Nagpur": top block (pill + hero), categories, areas, hot.
    val freshIndex = (if (hasTop) 1 else 0) + 2 + (if (uiState.hotPosts.isNotEmpty()) 1 else 0)

    SwipeRefresh(
        state = swipeState,
        onRefresh = onRefresh,
        indicator = { s, trigger ->
            SwipeRefreshIndicator(
                state = s,
                refreshTriggerDistance = trigger,
                backgroundColor = SurfaceAlt,
                contentColor = OrangePrimary
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)
        ) {
            // ── Pill + hero ──────────────────────────────────────────────
            if (hasTop) {
                item(key = "top") {
                    Column(Modifier.padding(bottom = 26.dp)) {
                        if (showPill) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                NewSincePill(
                                    count = uiState.newSinceLastVisit,
                                    onClick = { scope.launch { listState.animateScrollToItem(freshIndex) } }
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                        uiState.hero?.let { hero ->
                            RevealOnce(key = "hero_${hero.id}", modifier = Modifier.padding(horizontal = 16.dp)) {
                                HeroCard(post = hero, onClick = { onPostClick(hero.id) })
                            }
                        }
                    }
                }
            }

            // ── Categories ───────────────────────────────────────────────
            item(key = "categories") {
                Column(Modifier.padding(bottom = 26.dp)) {
                    SectionHeader(
                        icon = Icons.Filled.GridView,
                        title = stringResource(R.string.explore_section_categories)
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(uiState.categories, key = { _, c -> c.key }) { i, c ->
                            RevealOnce(key = "cat_${c.key}", delayMs = i * 45L) {
                                CategoryTile(stat = c, onClick = { onCategoryClick(c.key) })
                            }
                        }
                    }
                }
            }

            // ── Areas ────────────────────────────────────────────────────
            item(key = "areas") {
                Column(Modifier.padding(bottom = 26.dp)) {
                    SectionHeader(
                        icon = Icons.Filled.LocationOn,
                        title = stringResource(R.string.explore_section_areas)
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.areas.chunked(2).forEachIndexed { ri, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEachIndexed { ci, area ->
                                    RevealOnce(
                                        key = "area_${area.name}",
                                        delayMs = (ri * 2 + ci) * 45L,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        AreaCard(
                                            stat = area,
                                            onClick = { onAreaClick(area.name) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Hot right now ────────────────────────────────────────────
            if (uiState.hotPosts.isNotEmpty()) {
                item(key = "hot") {
                    Column(Modifier.padding(bottom = 26.dp)) {
                        SectionHeader(
                            icon = Icons.Filled.LocalFireDepartment,
                            title = stringResource(R.string.explore_section_hot)
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.hotPosts.forEachIndexed { i, post ->
                                RevealOnce(key = "hot_${post.id}", delayMs = i * 55L) {
                                    CompactPostRow(
                                        post = post,
                                        rank = i + 1,
                                        onClick = { onPostClick(post.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Fresh in Nagpur ──────────────────────────────────────────
            if (uiState.freshPosts.isNotEmpty()) {
                item(key = "fresh") {
                    Column(Modifier.padding(bottom = 26.dp)) {
                        SectionHeader(
                            icon = Icons.Filled.AutoAwesome,
                            title = stringResource(R.string.explore_section_fresh)
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.freshPosts.forEachIndexed { i, post ->
                                RevealOnce(key = "fresh_${post.id}", delayMs = i * 55L) {
                                    CompactPostRow(
                                        post = post,
                                        rank = null,
                                        onClick = { onPostClick(post.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── More to explore ──────────────────────────────────────────
            if (uiState.morePosts.isNotEmpty()) {
                item(key = "more_header") {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        SectionHeader(
                            icon = Icons.Filled.Explore,
                            title = stringResource(R.string.explore_section_more)
                        )
                    }
                }
                itemsIndexed(uiState.morePosts, key = { _, p -> "more_${p.id}" }) { i, post ->
                    StaggeredItem(i) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 10.dp)
                        ) {
                            SmartPostCard(post = post, onClick = { onPostClick(post.id) })
                        }
                    }
                }
            }

            // ── Closure: "you're all caught up" ──────────────────────────
            if (uiState.hasDiscoveryContent) {
                item(key = "caught_up") { CaughtUpFooter() }
            }
        }
    }
}

@Composable
private fun DiscoverySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp).shimmerEffect(RoundedCornerShape(24.dp)))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(Modifier.weight(1f).height(110.dp).shimmerEffect(RoundedCornerShape(20.dp)))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Box(Modifier.weight(1f).height(60.dp).shimmerEffect(RoundedCornerShape(14.dp)))
                    }
                }
            }
        }
        ShimmerPostCard()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BUILDING BLOCKS
// ══════════════════════════════════════════════════════════════════════════════

/** One header style for every section (consistent icon size, spacing and weight). */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = OrangePrimary,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/**
 * Fades + slides content in once. The "already played" flag is saved per key, so items that
 * scroll off and back on screen (or come back after a rotation) don't replay the animation.
 * It animates alpha/translation only, so layout never jumps.
 */
@Composable
private fun RevealOnce(
    key: String,
    modifier: Modifier = Modifier,
    delayMs: Long = 0L,
    content: @Composable () -> Unit
) {
    var played by rememberSaveable(key) { mutableStateOf(false) }
    var go by remember { mutableStateOf(played) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(delayMs)
            go = true
            played = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (go) 1f else 0f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "reveal"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 24.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun NewSincePill(count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(OrangeSubtle)
            .border(1.dp, OrangePrimary.copy(alpha = 0.45f), shape)
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LivePulse(color = OrangePrimary, size = 4.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(
                R.string.explore_new_since_visit,
                if (count >= 60) "60+" else count.toString()
            ),
            color = OrangePrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

/** Big "Happening now" card: photo (or colour gradient) + scrim + title + live badge. */
@Composable
private fun HeroCard(post: Post, onClick: () -> Unit) {
    val accent = categoryColor(post.category)
    val live = post.isAlert || post.isWithinHours(3)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pressScale(pressedScale = 0.98f, onClick = onClick)
            .clip(shape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), Color(0xFF1A1A1A))))
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
    ) {
        if (!post.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        // Scrim keeps the white text readable on any photo.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.82f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (live) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LivePulse(color = RedAlert, size = 3.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        stringResource(R.string.explore_live),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            CategoryBadge(post.category)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    post.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                val where = post.areaTag?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                Text(
                    "${post.timeAgo()}$where",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(stat: CategoryStat, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = categoryColor(stat.key)
    val shape = RoundedCornerShape(20.dp)
    val subtitle = when {
        stat.today > 0 -> stringResource(R.string.explore_cat_today, stat.today)
        stat.total > 0 -> stringResource(R.string.explore_cat_posts, stat.total)
        else -> stringResource(R.string.explore_cat_empty)
    }
    Column(
        modifier = modifier
            .width(136.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.06f)))
            )
            .border(1.dp, accent.copy(alpha = 0.28f), shape)
            .pressScale(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIcon(stat.key),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            categoryDisplayName(stat.key),
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            color = if (stat.today > 0) accent else TertiaryText,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AreaCard(stat: AreaStat, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    val border = if (stat.isYours) OrangePrimary.copy(alpha = 0.45f) else Divider
    Row(
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (stat.isYours) OrangeSubtle else Surface)
            .border(1.dp, border, shape)
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stat.name,
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (stat.count > 0) stringResource(R.string.explore_area_posts, stat.count)
                else stringResource(R.string.explore_area_empty),
                color = TertiaryText,
                maxLines = 1,
                fontSize = 12.sp
            )
        }
        if (stat.isYours) {
            Text(
                stringResource(R.string.explore_area_yours),
                color = OrangePrimary,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        } else if (stat.count >= 5) {
            LivePulse(color = OrangePrimary, size = 3.dp)
        }
    }
}

/** Compact list row. With a [rank] it's a leaderboard row; without, it shows the category icon. */
@Composable
private fun CompactPostRow(post: Post, rank: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = categoryColor(post.category)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, Divider, shape)
            .pressScale(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                rank.toString(),
                color = OrangePrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon(post.category),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                post.title,
                color = PrimaryText,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${categoryDisplayName(post.category)} · ${post.timeAgo()}",
                color = TertiaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
        if (post.commentCount > 0) {
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = TertiaryText,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(formatCount(post.commentCount), color = SecondaryText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CaughtUpFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = OrangePrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.explore_caught_up),
            color = SecondaryText,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.explore_caught_up_hint),
            color = TertiaryText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
