//java/com/nagpurpulse/ui/screens/profile/PublicProfileScreen.kt

package com.nagpurpulse.ui.screens.profile

import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nagpurpulse.ui.screens.settings.GlowIcon
import kotlinx.coroutines.delay
import com.nagpurpulse.data.model.memberSince
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.Profile
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.ProfileRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicProfileUiState(
    val profile: Profile? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val presenceRepository: PresenceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState
    val onlineUserIds: StateFlow<Set<String>> = presenceRepository.onlineUserIds

    init {
        presenceRepository.start()
    }

    private var lastUserId: String? = null

    fun load(userId: String) {
        lastUserId = userId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            profileRepository.getProfile(userId).fold(
                onSuccess = { profile ->
                    // A private / posts-hidden profile never needs its threads fetched.
                    if (profile.hideProfile || profile.hidePosts) {
                        _uiState.value = PublicProfileUiState(profile = profile, isLoading = false)
                        return@fold
                    }
                    postRepository.getPostsByUser(userId).fold(
                        onSuccess = { posts ->
                            _uiState.value = PublicProfileUiState(profile = profile, posts = posts, isLoading = false)
                        },
                        onFailure = {
                            // Keep the profile on screen even if the thread list failed.
                            _uiState.value = PublicProfileUiState(profile = profile, isLoading = false)
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = PublicProfileUiState(isLoading = false, error = e.message ?: "Couldn't load this profile")
                }
            )
        }
    }

    fun retry() { lastUserId?.let(::load) }
}

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val onlineUserIds by viewModel.onlineUserIds.collectAsState()
    val context = LocalContext.current

    val profile = uiState.profile
    val handle = profile?.username?.takeIf { it.isNotBlank() } ?: "User"
    // Respect the target user's visibility preference at the point of display too.
    val isOnline = userId in onlineUserIds && profile?.showOnlineStatus == true
    val canShare = profile != null && !profile.hideProfile

    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        containerColor = Background,
        topBar = {
            PublicProfileTopBar(
                handle = handle,
                isOnline = isOnline,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
                profile == null -> ProfileLoadError(onRetry = viewModel::retry)
                profile.hideProfile -> PrivateProfileView(profile = profile, onBack = onBack)
                else -> PublicProfileContent(
                    profile = profile,
                    posts = uiState.posts,
                    isOnline = isOnline,
                    onPostClick = onPostClick
                )
            }
        }
    }
}

// ── Top bar ─────────────────────────────────────────────────────────────────

@Composable
private fun PublicProfileTopBar(
    handle: String,
    isOnline: Boolean,
    canShare: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 130f))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("u/$handle", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedVisibility(visible = isOnline, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Text("Online now", color = GreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        // Balances the back button so the title stays optically centred.
        Spacer(Modifier.size(48.dp))
    }
}

// ── Reveal helper: calm, staggered entrance ───────────────────────────────────

@Composable
private fun Reveal(shown: Boolean, delayMs: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(450, delayMillis = delayMs)) +
                slideInVertically(tween(450, delayMillis = delayMs, easing = FastOutSlowInEasing)) { it / 8 }
    ) { content() }
}

// ── Private (locked) profile ─────────────────────────────────────────────────

@Composable
private fun PrivateProfileView(profile: Profile, onBack: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(Modifier.fillMaxSize()) {
        // Soft ambient glow behind the hero
        Box(
            Modifier
                .size(300.dp)
                .align(Alignment.TopCenter)
                .offset(y = 10.dp)
                .background(Brush.radialGradient(listOf(OrangePrimary.copy(alpha = 0.16f), Color.Transparent)), CircleShape)
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Reveal(shown, 0) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    // We deliberately do not show the real photo: private means private.
                    UserAvatar(name = profile.username, imageUrl = null, size = 104.dp)
                    Box(
                        Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Background)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(OrangeLight, OrangePrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Reveal(shown, 90) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        profile.displayName?.takeIf { it.isNotBlank() } ?: profile.username,
                        color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 26.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                    )
                    Text("u/${profile.username}", color = SecondaryText, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(OrangePrimary.copy(alpha = 0.12f))
                            .border(1.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Private profile", color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Reveal(shown, 180) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Surface)
                        .border(1.dp, OrangePrimary.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlowIcon(Icons.Filled.Shield, OrangePrimary, active = true, size = 48.dp)
                    Spacer(Modifier.height(2.dp))
                    Text("Keeping things to themselves", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 19.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "u/${profile.username} prefers to keep their profile and activity private. That's their call, and it's a good thing the app lets everyone make it.",
                        color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PrivacyInfoItem(Icons.Filled.VisibilityOff, "Threads", "Private")
                        PrivacyInfoItem(Icons.Filled.NotificationsOff, "Activity", "Private")
                        PrivacyInfoItem(Icons.Filled.PersonOff, "Details", "Private")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Reveal(shown, 270) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(PinkEvents.copy(alpha = 0.12f)),
                        Alignment.Center
                    ) { Icon(Icons.Filled.FavoriteBorder, null, tint = PinkEvents, modifier = Modifier.size(19.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Good communities respect boundaries", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("If you know them, they can always share their profile with you directly.", color = SecondaryText, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Reveal(shown, 360) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f))
                ) {
                    Text("Back to the community", color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PrivacyInfoItem(icon: ImageVector, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(OrangePrimary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(title, color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(subtitle, color = SecondaryText, fontSize = 12.sp)
    }
}

// ── Error ────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileLoadError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlowIcon(Icons.Filled.CloudOff, SecondaryText, active = false, size = 56.dp)
        Spacer(Modifier.height(8.dp))
        Text("We couldn't open this profile", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "It may have been removed, or your connection dropped. Nothing is wrong on your end.",
            color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.White)
        ) { Text("Try again", fontWeight = FontWeight.SemiBold) }
    }
}

// ── Public profile ───────────────────────────────────────────────────────────

@Composable
private fun PublicProfileContent(
    profile: Profile,
    posts: List<Post>,
    isOnline: Boolean,
    onPostClick: (String) -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val nameSize = if (screenWidth < 360) 23.sp else if (screenWidth < 400) 26.sp else 28.sp
    val name = profile.displayName?.takeIf { it.isNotBlank() } ?: profile.username

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 480f))
            ) {
                Box(
                    Modifier
                        .size(200.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.radialGradient(listOf(OrangePrimary.copy(0.09f), Color.Transparent)))
                )
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Reveal(shown, 0) { ProfileAvatar(profile.avatarUrl, profile.username, isOnline) }

                    Spacer(Modifier.height(16.dp))

                    Reveal(shown, 90) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text(
                                    name, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = nameSize,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (profile.isVerified) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = BlueInfo, modifier = Modifier.size(22.dp))
                                }
                            }
                            Text("u/${profile.username}", color = SecondaryText, fontSize = 14.sp)
                            if (!profile.tagline.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    profile.tagline, color = PrimaryText.copy(alpha = 0.85f), fontSize = 14.sp,
                                    textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                InfoChip(Icons.Filled.LocationOn, profile.areas.firstOrNull() ?: "Nagpur")
                                InfoChip(Icons.Filled.CalendarMonth, profile.memberSince().replace("Member since ", "Joined "))
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Reveal(shown, 170) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Check out u/${profile.username} on NagpurPulse")
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share profile"))
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share profile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString("u/${profile.username}"))
                                    Toast.makeText(context, "Username copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(25.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Copy handle", color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Reveal(shown, 230) { CommunityLevelCard(progress) }

                    Spacer(Modifier.height(18.dp))

                    Reveal(shown, 300) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PublicStatBox(formatCount(profile.karma), "Karma", OrangePrimary, Modifier.weight(1f))
                            PublicStatBox(
                                if (profile.hidePosts) "Hidden" else formatCount(posts.size),
                                "Threads", BlueInfo, Modifier.weight(1f)
                            )
                            PublicStatBox(
                                if (profile.hideComments) "Hidden" else "—",
                                "Comments", GreenSuccess, Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Divider, thickness = 0.5.dp)
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(50)).background(OrangePrimary))
                Spacer(Modifier.width(8.dp))
                Text("Threads", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (!profile.hidePosts) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${posts.size}", color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(OrangePrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 2.dp)
                    )
                    if (posts.size > 1) {
                        Spacer(Modifier.weight(1f))
                        SortPill("Latest", selected = !sortTop) { sortTop = false }
                        Spacer(Modifier.width(6.dp))
                        SortPill("Top", selected = sortTop) { sortTop = true }
                    }
                }
            }
        }

        when {
            profile.hidePosts -> item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlowIcon(Icons.Filled.VisibilityOff, OrangePrimary, active = false, size = 52.dp)
                    Spacer(Modifier.height(6.dp))
                    Text("Threads are private", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "u/${profile.username} has chosen not to show their threads.",
                        color = SecondaryText, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
            }
            posts.isEmpty() -> item {
                EmptyState(
                    icon = Icons.Filled.ChatBubbleOutline,
                    title = "Nothing here yet",
                    subtitle = "When u/${profile.username} posts, their threads will show up here.",
                    modifier = Modifier.padding(40.dp)
                )
            }
            else -> itemsIndexed(posts) { i, post ->
                StaggeredItem(i) {
                    PostCard(
                        post = post,
                        onClick = { onPostClick(post.id) },
                        modifier = Modifier.padding(horizontal = 14.dp).padding(top = 10.dp)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun ProfileAvatar(avatarUrl: String?, username: String, isOnline: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(OrangePrimary.copy(0.10f)))
        Box(
            Modifier
                .size(122.dp)
                .border(
                    2.5.dp,
                    Brush.sweepGradient(listOf(OrangePrimary, OrangeLight, OrangePrimary.copy(0.3f), OrangePrimary)),
                    CircleShape
                )
        )
        UserAvatar(name = username, imageUrl = avatarUrl, size = 108.dp)
        if (isOnline) {
            Box(+                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-10).dp, y = (-10).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Background)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(GreenSuccess)
                    .semantics { contentDescription = "Online" }
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PublicStatBox(value: String, label: String, accentColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, accentColor.copy(0.18f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accentColor, fontWeight = FontWeight.Bold, fontSize = if (value.length > 5) 16.sp else 24.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(label, color = SecondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CommunityLevelCard(progress: LevelProgress) {
    val start = Color(progress.level.colorStart)
    val end = Color(progress.level.colorEnd)
    val animated by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(900, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "level_progress"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, start.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(start, end))),
            contentAlignment = Alignment.Center
        ) { Text(progress.level.emoji, fontSize = 22.sp) }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(progress.level.title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Level ${progress.level.number}", color = start, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(start.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(start.copy(alpha = 0.15f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animated.coerceIn(0.04f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(start, end)))
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                if (progress.next != null) "${progress.remaining} karma to ${progress.next.emoji} ${progress.next.title}"
                else "Top level reached — a true city legend",
                color = SecondaryText, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SortPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) OrangePrimary else SurfaceAlt, tween(200), label = "sort_bg")
    Text(
        label,
        color = if (selected) Color.White else SecondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}