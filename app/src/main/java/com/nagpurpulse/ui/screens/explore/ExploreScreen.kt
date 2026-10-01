// java/com/nagpurpulse/ui/screens/explore/ExploreScreen.kt

package com.nagpurpulse.ui.screens.explore

import androidx.compose.ui.text.style.TextOverflow
import com.nagpurpulse.data.model.categoryDisplay
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nagpurpulse.ui.navigation.Screen

data class ExploreUiState(
    val results: List<Post> = emptyList(),
    val trendingPosts: List<Post> = emptyList(),
    val trendingTopics: List<String> = emptyList(),
    val trendingSearches: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {



    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    init {
        loadTrending()
        loadTrendingTopics()
        viewModelScope.launch {
            _query.debounce(350).filter { it.isNotBlank() }.collect { q -> search(q) }
        }
    }

    fun onQueryChange(q: String) {
        _query.value = q
    }

    private fun loadTrendingTopics() {

        viewModelScope.launch {

            postRepository.getPosts(sortBy = "new")
                .fold(
                    onSuccess = { posts ->

                        val topics = posts
                            .groupBy { it.categoryDisplay() }
                            .mapValues { it.value.size }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)
                            .map { it.first }

                        _uiState.value = _uiState.value.copy(
                            trendingTopics = topics
                        )
                    },
                    onFailure = { }
                )
        }
    }

    private fun loadTrending() {
        viewModelScope.launch {
            postRepository.getPosts(sortBy = "hot").fold(onSuccess = { posts ->
                _uiState.value = _uiState.value.copy(
                    trendingPosts = posts.take(10),
                    trendingSearches = posts.take(5)
                )
            }, onFailure = {})
        }
    }

    fun search(q: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, hasSearched = true)
            postRepository.searchPosts(q).fold(onSuccess = { results ->
                _uiState.value = _uiState.value.copy(results = results, isSearching = false)
            }, onFailure = { _uiState.value = _uiState.value.copy(isSearching = false) })
        }
    }

    fun clearSearch() {
        _query.value = ""; _uiState.value =
            _uiState.value.copy(results = emptyList(), hasSearched = false)
    }
}



private val nagpurAreas = listOf(
    "Dharampeth",
    "Sitabuldi",
    "Manish Nagar",
    "Bajaj Nagar",
    "Hingna",
    "Wardha Road",
    "Ramdaspeth",
    "Civil Lines"
)

@OptIn(ExperimentalMaterial3Api::class)
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

    // Search bar entrance
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); headerVisible = true }

    Scaffold(
        containerColor = Background,
        topBar = {
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
                    initialOffsetY = { -30 })
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Surface, Background), 0f, 200f
                            )
                        )
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Animated search bar
                        val borderColor by animateColorAsState(
                            if (focused) OrangePrimary else Divider,
                            tween(200),
                            label = "search_border"
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(26.dp))
                                .background(SurfaceAlt)
                                .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                null,
                                tint = if (focused) OrangePrimary else TertiaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = query,
                                onValueChange = { viewModel.onQueryChange(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = PrimaryText
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) Text(
                                            "Search Nagpur…", color = TertiaryText, style = MaterialTheme.typography.titleMedium
                                        )
                                        inner()
                                    }
                                })
                            if (query.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.Close,
                                    null,
                                    tint = TertiaryText,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .pressScale { viewModel.clearSearch() })
                            }
                        }

                        if (focused) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Cancel",
                                color = OrangePrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.pressScale {
                                    focused = false; viewModel.clearSearch()
                                })
                        }
                    }

                    if (!focused && query.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled. TrendingUp,
                                null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                " Explore Nagpur",
                                color = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onCreatePost = onCreatePost,
                onProfileClick = onProfileClick
            )
        }) { paddingValues ->
        if (uiState.isSearching) {
            LazyColumn(
                Modifier.padding(paddingValues),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(5) { ShimmerPostCard() }
            }
            return@Scaffold
        }

        // Search results
        if (uiState.hasSearched && query.isNotEmpty()) {
            if (uiState.results.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    EmptyState(
                        icon = Icons.Filled.SearchOff,
                        title = "No results for \"$query\"",
                        subtitle = "Try a different search term or explore by category.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${uiState.results.size} results for \"$query\"",
                            color = SecondaryText,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    itemsIndexed(uiState.results) { i, post ->
                        StaggeredItem(i) {
                            SmartPostCard(post = post, onClick = { onPostClick(post.id) })
                        }
                    }
                }
            }
            return@Scaffold
        }

        // Discovery view (no search active)
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // ── Trending Topics ────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            " Trending Topics",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        itemsIndexed(uiState.trendingTopics) { i, topic ->
                            var tv by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { delay(i * 60L); tv = true }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = tv, enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                                    initialScale = 0.8f, animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(SurfaceAlt)
                                        .border(1.dp, Divider, RoundedCornerShape(22.dp))
                                        .pressScale {
                                            navController.navigate(
                                                Screen.CategoryPosts.createRoute(topic)
                                            )
                                        }

                                        .padding(horizontal = 14.dp, vertical = 9.dp)) {
                                    Text(
                                        topic,
                                        color = PrimaryText,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = Divider,
                    thickness = 0.5.dp
                )
            }

            // ── Recent Searches ────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.TrendingUp,
                            null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            " Trending Searches",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.trendingSearches.forEachIndexed { i, post ->
                            var rv by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(i * 50L + 100L)
                                rv = true
                            }

                            AnimatedVisibility(
                                visible = rv,
                                enter = fadeIn(animationSpec = tween(300)) +
                                        slideInHorizontally(initialOffsetX = { -20 })
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SurfaceAlt)
                                        .pressScale {
                                            onPostClick(post.id)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceAlt),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = TertiaryText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Text(
                                        text = post.title,
                                        maxLines = 1,


                                        overflow = TextOverflow.Ellipsis,
                                        color = SecondaryText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Icon(
                                        Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = TertiaryText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = Divider,
                        thickness = 0.5.dp
                    )

                }
            }

            // ── Browse by Area ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            " Browse by Area",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // 2-column grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        nagpurAreas.chunked(2).forEachIndexed { ri, row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEachIndexed { ci, area ->
                                    var av by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) {
                                        delay((ri * 2 + ci) * 55L + 200L); av = true
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = av,
                                            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                                                initialScale = 0.88f
                                            )
                                        ) {

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Surface)
                                                    .border(
                                                        1.dp,
                                                        Divider,
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .pressScale {
                                                        viewModel.onQueryChange(area); viewModel.search(
                                                        area
                                                    )
                                                    }
                                                    .padding(
                                                        horizontal = 12.dp, vertical = 12.dp
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(OrangePrimary.copy(0.7f))
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    area,
                                                    color = PrimaryText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                }
            }

            // ── Trending Posts ─────────────────────────────────────────
            if (uiState.trendingPosts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            " Top Posts Today",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                itemsIndexed(uiState.trendingPosts) { i, post ->
                    StaggeredItem(i) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 10.dp)
                        ) {
                            SmartPostCard(
                                post = post,
                                onClick = { onPostClick(post.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}


