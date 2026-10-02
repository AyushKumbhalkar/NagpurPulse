//java/com/nagpurpulse/ui/screens/profile/PublicProfileScreen.kt

package com.nagpurpulse.ui.screens.profile

import androidx.compose.ui.graphics.vector.ImageVector
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

    fun load(userId: String) {
        viewModelScope.launch {
            profileRepository.getProfile(userId).fold(
                onSuccess = { profile ->
                    android.util.Log.d(
                        "PUBLIC_PROFILE_DEBUG",
                        """
    username=${profile.username}
    hideProfile=${profile.hideProfile}
    userId=${profile.id}
    """.trimIndent()
                    )

                    postRepository.getPostsByUser(userId).fold(
                        onSuccess = { posts ->
                            _uiState.value = PublicProfileUiState(
                                profile   = profile,
                                posts     = posts,
                                isLoading = false
                            )
                        },
                        onFailure = {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }
}

@Composable
fun PublicProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    viewModel: PublicProfileViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val displayName = uiState.profile?.username ?: "User"
    val onlineUserIds by viewModel.onlineUserIds.collectAsState()
    val isProfileOnline = userId in onlineUserIds
    val avatarUrl = uiState.profile?.avatarUrl
    var avatarVisible by remember {
        mutableStateOf(false)
    }

    var infoVisible by remember {
        mutableStateOf(false)
    }

    var statsVisible by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        delay(80)
        avatarVisible = true

        delay(120)
        infoVisible = true

        delay(100)
        statsVisible = true
    }

    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        containerColor = Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Surface, Background),
                            0f,
                            130f
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryText)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("u/$displayName", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isProfileOnline) {
                        Text("Online", color = Color(0xFF22C55E), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Share, null, tint = SecondaryText)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {


            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return@Scaffold
        }

        val profile = uiState.profile

        if (
            profile != null &&
            profile.hideProfile
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 40.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    OrangePrimary.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 24.dp,
                            bottom = 12.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {



                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            OrangePrimary.copy(0.20f),
                                            OrangePrimary.copy(0.05f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = profile.username,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = OrangePrimary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 8.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    "Private Profile",
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))




                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Surface
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(48.dp)
                                )

                                Spacer(Modifier.height(16.dp))

                                Text(
                                    "This profile is private",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "${profile.username} has chosen to keep their profile and activity private.",
                                    textAlign = TextAlign.Center,
                                    color = SecondaryText
                                )

                                Spacer(Modifier.height(20.dp))

                                HorizontalDivider(color = Divider)

                                Spacer(Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {

                                    PrivacyInfoItem(
                                        icon = Icons.Default.VisibilityOff,
                                        title = "Content",
                                        subtitle = "Hidden"
                                    )

                                    PrivacyInfoItem(
                                        icon = Icons.Default.NotificationsOff,
                                        title = "Activity",
                                        subtitle = "Hidden"
                                    )

                                    PrivacyInfoItem(
                                        icon = Icons.Default.PersonOff,
                                        title = "Info",
                                        subtitle = "Hidden"
                                    )
                                }

                        }

                            Spacer(Modifier.height(20.dp))



                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Surface
                            )
                        ) {

                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = OrangePrimary
                                )

                                Spacer(Modifier.width(14.dp))

                                Column {

                                    Text(
                                        "Respect Privacy",
                                        color = PrimaryText,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        "This user has chosen to keep their profile private.",
                                        color = SecondaryText,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            // Profile header card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Surface, Background),
                                0f,
                                480f
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier.size(180.dp)
                            .background(Brush.radialGradient(listOf(OrangePrimary.copy(0.07f), Color.Transparent)))
                            .align(Alignment.TopCenter)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        AnimatedVisibility(
                            visible = avatarVisible,
                            enter = fadeIn(tween(400)) +
                                    scaleIn(initialScale = 0.7f)
                        ) {

                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(132.dp)
                                        .clip(CircleShape)
                                        .background(OrangePrimary.copy(0.12f))
                                )

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
                                )

                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(108.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceAlt),
                                    contentScale = ContentScale.Crop
                                )

}
                        }
                        Spacer(Modifier.height(14.dp))

                        AnimatedVisibility(
                            visible = infoVisible,
                            enter = fadeIn(tween(400))
                        ) {

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                        val profileScreenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
                        Text(
                            displayName,
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (profileScreenWidth < 360) 23.sp else if (profileScreenWidth < 400) 26.sp else 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )

                        Text(
                            "u/$displayName",
                            color = SecondaryText,
                            fontSize = 14.sp
                        )
                        if (!uiState.profile?.tagline.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(uiState.profile!!.tagline!!, color = SecondaryText, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(6.dp))
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

                                    append(
                                        uiState.profile?.areas?.firstOrNull()
                                            ?: "Nagpur"
                                    )

                                    append(" • ")

                                    append(
                                        uiState.profile?.memberSince()
                                            ?.replace(
                                                "Member since ",
                                                "Joined "
                                            )
                                            ?: "Joined 2026"
                                    )
                                },
                                color = SecondaryText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(18.dp))}}
                        // Follow + Message buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(25.dp)
                            ) {

                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.width(8.dp))

                                Text(
                                    "Follow User",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        // Stats
                        AnimatedVisibility(
                            visible = statsVisible,
                            enter = fadeIn(tween(400)) +
                                    slideInVertically { 30 }
                        ) {

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                PublicStatBox(
                                    formatCount(uiState.profile?.karma ?: 0),
                                    "Karma",
                                    OrangePrimary,
                                    Modifier.weight(1f)
                                )

                                PublicStatBox(
                                    formatCount(uiState.posts.size),
                                    "Threads",
                                    BlueInfo,
                                    Modifier.weight(1f)
                                )

                                PublicStatBox(
                                    "0",
                                    "Comments",
                                    GreenSuccess,
                                    Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Divider, thickness = 0.5.dp)
                    }
                }
            }

            // Posts header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Threads (${uiState.posts.size})", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }

            if (uiState.posts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.ChatBubbleOutline,
                        title = "No posts yet",
                        subtitle = "This user hasn't posted anything yet.",
                        modifier = Modifier.padding(40.dp)
                    )
                }
            } else {
                itemsIndexed(uiState.posts) { i, post ->
                    StaggeredItem(i) {
                        PostCard(
                            post     = post,
                            onClick  = { onPostClick(post.id) },
                            modifier = Modifier.padding(horizontal = 14.dp).padding(top = 10.dp)
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PublicStatBox(value: String, label: String, accentColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, accentColor.copy(0.15f), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 26.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(label, color = SecondaryText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
@Composable
private fun PrivacyInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(OrangePrimary.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = OrangePrimary
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            title,
            color = PrimaryText,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )

        Text(
            subtitle,
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}