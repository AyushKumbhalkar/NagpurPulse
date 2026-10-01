// this is the MessagesScreen.kt file
//java/com/nagpurpulse/ui/screens/messages/MessagesScreen.kt

package com.nagpurpulse.ui.screens.messages

import com.nagpurpulse.ui.navigation.BottomNavBar
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.model.Conversation
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activeFilter: String = "all",
    val error: String? = null
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val presenceRepository: PresenceRepository
) : ViewModel() {
    val onlineUserIds: StateFlow<Set<String>> = presenceRepository.onlineUserIds
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState

    init { load(); subscribeRealtime() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = !refresh, isRefreshing = refresh)
            messageRepository.getConversations().fold(
                onSuccess = { convs ->
                    _uiState.value = _uiState.value.copy(conversations = convs, isLoading = false, isRefreshing = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, error = e.message)
                }
            )
        }
    }

    fun setFilter(filter: String) { _uiState.value = _uiState.value.copy(activeFilter = filter) }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            messageRepository.subscribeToConversations().collect { updatedConv ->
                val current = _uiState.value.conversations.toMutableList()
                val idx = current.indexOfFirst { it.id == updatedConv.id }
                if (idx >= 0) current[idx] = updatedConv else current.add(0, updatedConv)
                _uiState.value = _uiState.value.copy(conversations = current.sortedByDescending { it.lastMessageAt })
            }
        }
    }

    fun filteredConversations(): List<Conversation> = when (_uiState.value.activeFilter) {
        "unread"   -> _uiState.value.conversations.filter { it.myUnreadCount > 0 }
        else       -> _uiState.value.conversations
    }
}

@Composable
fun MessagesScreen(
    navController: NavController,
    onCreatePost: () -> Unit,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val uiState           = viewModel.uiState.collectAsState().value
    val onlineUserIds     = viewModel.onlineUserIds.collectAsState().value
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)
    val filtered          = viewModel.filteredConversations()
    val totalUnread       = uiState.conversations.sumOf { it.myUnreadCount }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.takeIf { it.isNotBlank() }?.let { snackbarHostState.showSnackbar(it) }
    }
    var headerVisible     by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); headerVisible = true }

    // Only show filters backed by working data. Pinned chats and message
    // requests are not persisted/implemented yet, so don't expose dead tabs.
    val filterLabels = listOf("all", "unread")

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(headerVisible, enter = fadeIn(tween(350)) + slideInVertically { -30 }) {
                Column(modifier = Modifier.background(Brush.verticalGradient(
                    listOf(Surface, Background),
                    0f,
                    260f
                )).statusBarsPadding()) {
                    // Title row
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Messages", color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.TheaterComedy,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        TopBarIcon(Icons.Filled.Search, "Search people") {
                            navController.navigate(Screen.UserSearch.route)
                        }
                        Spacer(Modifier.width(8.dp))
                        TopBarIcon(Icons.Filled.AddComment, "New message") {
                            navController.navigate(Screen.UserSearch.route)
                        }
                    }
                    Text("Incognito chats, real connections", color = SecondaryText, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 14.dp))

                    // Action cards row
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionCard(
                            Icons.Filled.Add,
                            "New Chat",
                            "Start an incognito chat",
                            OrangePrimary,
                            Modifier.weight(1f)
                        ) { navController.navigate(Screen.UserSearch.route) }

                        ActionCard(
                            Icons.Filled.Shield,
                            "Private by design",
                            "Start a chat from a profile",
                            GreenSuccess,
                            Modifier.weight(1f)
                        ) { navController.navigate(Screen.UserSearch.route) }
                    }

                    // Filter tabs
                    ScrollableTabRow(
                        selectedTabIndex = filterLabels.indexOf(uiState.activeFilter),
                        containerColor   = Color.Transparent,
                        contentColor     = OrangePrimary,
                        edgePadding      = 16.dp,
                        indicator = {}, divider = {}
                    ) {
                        filterLabels.forEach { filter ->
                            val sel = uiState.activeFilter == filter
                            val count = when(filter) {
                                "all"    -> uiState.conversations.size
                                "unread" -> uiState.conversations.count { it.myUnreadCount > 0 }
                                else     -> 0
                            }
                            val pillColor by animateColorAsState(
                                targetValue = if (sel) OrangePrimary else SurfaceAlt,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                label = "message_filter_pill"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(pillColor)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { viewModel.setFilter(filter) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = filter.replaceFirstChar { it.uppercase() } + if (count > 0) "  $count" else "",
                                    color = if (sel) Color.White else SecondaryText,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                }
            }
        },
        bottomBar = {
            BottomNavBar(navController = navController, onCreatePost = onCreatePost, hasAlertBadge = false, messageCount = totalUnread)
        }
    ) { padding ->
        SwipeRefresh(state = swipeRefreshState, onRefresh = { viewModel.load(true) }, modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(8) { ShimmerConversationRow() }
                }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = Icons.Filled.ChatBubbleOutline,
                        title = "No messages yet",
                        subtitle = "Search for someone and start a private incognito chat",
                        ctaLabel = "Find People",
                        onCta = { navController.navigate(Screen.UserSearch.route) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    itemsIndexed(filtered) { i, conv ->
                        StaggeredItem(i) {
                            ConversationRow(conv = conv, isOnline = conv.otherUserId in onlineUserIds, onClick = { navController.navigate(Screen.Chat.createRoute(conv.id)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = SecondaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, tint.copy(0.18f), RoundedCornerShape(16.dp)).pressScale(onClick = onClick).padding(12.dp)) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(title, color = PrimaryText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = SecondaryText, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
fun ConversationRow(conv: Conversation, isOnline: Boolean = false, onClick: () -> Unit) {
    val seed        = conv.otherAvatarSeed ?: conv.otherUserId ?: "anon"
    val displayName = conv.otherUsername ?: "Incognito"
    val hasUnread   = conv.myUnreadCount > 0
    val avatarColor = incognitoColor(seed)

    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (hasUnread) OrangePrimary.copy(0.03f) else Color.Transparent)
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(avatarColor.copy(0.5f), avatarColor.copy(0.2f))))
                    .border(1.5.dp, avatarColor.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(incognitoEmoji(seed), fontSize = 22.sp) }
            if (isOnline) {
                Box(
                    Modifier.size(13.dp).clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, Background, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, color = PrimaryText, fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (conv.isPinned) Icon(Icons.Filled.PushPin, null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatConvTime(conv.lastMessageAt), color = TertiaryText, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conv.lastMessage ?: "", color = if (hasUnread) SecondaryText else TertiaryText,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                if (hasUnread) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(OrangePrimary), contentAlignment = Alignment.Center) {
                        Text(if (conv.myUnreadCount > 99) "99+" else "${conv.myUnreadCount}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else if (conv.isMuted) {
                    Icon(Icons.Filled.NotificationsOff, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
    HorizontalDivider(
        color = Divider.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 80.dp)
    )
}

@Composable
private fun ShimmerConversationRow() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).shimmerEffect(RoundedCornerShape(26.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.width(130.dp).height(14.dp).shimmerEffect())
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.7f).height(12.dp).shimmerEffect())
        }
    }
}

private fun formatConvTime(ts: String): String = try {
    val inst = java.time.Instant.parse(ts)
    val diff = java.time.Duration.between(inst, java.time.Instant.now())
    when {
        diff.toMinutes() < 60 -> "${diff.toMinutes()}m"
        diff.toHours() < 24   -> { val d = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault()); "${d.hour}:${d.minute.toString().padStart(2,'0')}" }
        diff.toDays() < 2     -> "Yesterday"
        diff.toDays() < 7     -> { val d = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault()); d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) }
        else -> { val d = java.time.ZonedDateTime.ofInstant(inst, java.time.ZoneId.systemDefault()); "${d.dayOfMonth} ${d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}" }
    }
} catch (_: Exception) { "" }


