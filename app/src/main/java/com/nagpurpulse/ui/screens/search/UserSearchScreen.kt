package com.nagpurpulse.ui.screens.search

import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nagpurpulse.data.model.Profile
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class UserSearchUiState(
    val results: List<Profile> = emptyList(),
    val isSearching: Boolean = false,
    val isOpeningChat: Boolean = false,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserSearchUiState())
    val uiState: StateFlow<UserSearchUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    init {
        viewModelScope.launch {
            _query.debounce(300)
                .filter { it.length >= 2 }
                .collect { q -> search(q) }
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    private fun search(q: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            messageRepository.searchUsers(q).fold(
                onSuccess = { profiles ->
                    _uiState.value = _uiState.value.copy(results = profiles, isSearching = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isSearching = false, error = e.message)
                }
            )
        }
    }

    fun startChat(otherUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOpeningChat = true)
            messageRepository.getOrCreateConversation(otherUserId).fold(
                onSuccess = { conv ->
                    _uiState.value = _uiState.value.copy(isOpeningChat = false)
                    onSuccess(conv.id)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isOpeningChat = false, error = e.message)
                }
            )
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun UserSearchScreen(
    navController: NavController,
    viewModel: UserSearchViewModel = hiltViewModel()
) {
    val uiState     = viewModel.uiState.collectAsState().value
    val query       by viewModel.query.collectAsState()
    val focusReq    = remember { FocusRequester() }
    val borderColor by animateColorAsState(
        if (query.isNotEmpty()) OrangePrimary else DividerColor,
        tween(200), label = "border"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        focusReq.requestFocus()
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(SurfaceOne, BackgroundDark), 0f, 160f))
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }

                    // Search bar
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(26.dp))
                            .background(SurfaceTwo)
                            .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, null, tint = if (query.isNotEmpty()) OrangePrimary else TextTertiary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value         = query,
                            onValueChange = { viewModel.onQueryChange(it) },
                            modifier      = Modifier.weight(1f).focusRequester(focusReq),
                            textStyle     = TextStyle(color = TextPrimary, fontSize = 15.sp),
                            singleLine    = true,
                            cursorBrush   = SolidColor(OrangePrimary),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) Text("Search by username…", color = TextTertiary, fontSize = 15.sp)
                                    inner()
                                }
                            }
                        )
                        if (query.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Close, null, tint = TextTertiary,
                                modifier = Modifier.size(16.dp).pressScale { viewModel.onQueryChange("") })
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Text("Cancel", color = OrangePrimary, fontSize = 14.sp,
                        modifier = Modifier.pressScale { navController.popBackStack() })
                }
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                query.length < 2 -> {
                    // Hint state
                    Column(
                        modifier            = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Find people on NagpurPulse", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Type at least 2 characters to search by username", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                uiState.isSearching -> {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(5) { ShimmerUserRow() }
                    }
                }

                uiState.results.isEmpty() -> {
                    EmptyState(
                        icon     = Icons.Filled.PersonSearch,
                        title    = "No users found",
                        subtitle = "Try a different username",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("${uiState.results.size} result${if (uiState.results.size > 1) "s" else ""} for \"$query\"",
                                color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        itemsIndexed(uiState.results) { i, profile ->
                            StaggeredItem(i) {
                                UserResultCard(
                                    profile     = profile,
                                    isLoading   = uiState.isOpeningChat,
                                    onMessage   = {
                                        viewModel.startChat(profile.id) { convId ->
                                            navController.navigate(Screen.Chat.createRoute(convId)) {
                                                popUpTo(Screen.UserSearch.route) { inclusive = true }
                                            }
                                        }
                                    },
                                    onViewProfile = {
                                        navController.navigate(Screen.UserProfile.createRoute(profile.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Error snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier        = Modifier.align(Alignment.BottomCenter).padding(
    DensityManager.cardPadding.dp
),
                    containerColor  = SurfaceThree,
                    contentColor    = TextPrimary
                ) { Text(uiState.error) }
            }
        }
    }
}

@Composable
private fun UserResultCard(
    profile: Profile,
    isLoading: Boolean,
    onMessage: () -> Unit,
    onViewProfile: () -> Unit
) {
    val avatarSeed = profile.username.ifBlank { profile.id }
    val avatarColor = incognitoColor(avatarSeed)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceOne)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .pressScale(onClick = onViewProfile)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(avatarColor.copy(0.5f), avatarColor.copy(0.15f))))
                .border(1.5.dp, avatarColor.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "u/${profile.username ?: "unknown"}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!profile.tagline.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(profile.tagline, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = TextTertiary, modifier = Modifier.size(11.dp))
                Text(" ${profile.areas?.firstOrNull() ?: "Nagpur"}", color = TextTertiary, fontSize = 12.sp)
                Text("  ·  ", color = TextTertiary, fontSize = 12.sp)
                Text("${profile.karma} karma", color = OrangePrimary, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.width(10.dp))

        // Message button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(OrangePrimary)
                .pressScale(onClick = onMessage)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Message, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Message", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ShimmerUserRow() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceOne).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(50.dp).shimmerEffect(RoundedCornerShape(25.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.width(100.dp).height(14.dp).shimmerEffect())
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(160.dp).height(12.dp).shimmerEffect())
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(80.dp).height(38.dp).shimmerEffect(RoundedCornerShape(22.dp)))
    }
}
