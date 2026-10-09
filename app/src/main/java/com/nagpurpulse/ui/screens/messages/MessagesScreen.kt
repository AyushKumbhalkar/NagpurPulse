// this is the MessagesScreen.kt file
//java/com/nagpurpulse/ui/screens/messages/MessagesScreen.kt

package com.nagpurpulse.ui.screens.messages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.local.ChatDraftStore
import com.nagpurpulse.data.local.ConnectivityObserver
import com.nagpurpulse.data.model.Conversation
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State & events ────────────────────────────────────────────────────────────

data class MessagesUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    // "all" | "unread" | "needs_reply"
    val activeFilter: String = "all",
    // Only set when the inbox has nothing to show; transient errors go through [MessagesEvent].
    val loadError: String? = null,
    // Local search over my chats (name + last message).
    val searchQuery: String = "",
    // conversationId -> unsent draft text
    val drafts: Map<String, String> = emptyMap(),
    val isOffline: Boolean = false
)

/** One-off UI events (snackbars) so they are shown exactly once. */
sealed interface MessagesEvent {
    data class Info(val text: String) : MessagesEvent
    data class DeletedWithUndo(val conversationId: String, val name: String) : MessagesEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val presenceRepository: PresenceRepository,
    private val authRepository: AuthRepository,
    private val draftStore: ChatDraftStore,
    private val connectivity: ConnectivityObserver
) : ViewModel() {
    val onlineUserIds: StateFlow<Set<String>> = presenceRepository.onlineUserIds
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState

    private val _events = MutableSharedFlow<MessagesEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<MessagesEvent> = _events

    val myUserId: String get() = authRepository.currentUserId ?: ""

    init {
        presenceRepository.start()
        load()
        subscribeRealtime()
        observeDraftsAndConnectivity()
    }

    private fun observeDraftsAndConnectivity() {
        viewModelScope.launch {
            draftStore.draftsFor(authRepository.currentUserId ?: "").collect { drafts ->
                _uiState.update { it.copy(drafts = drafts) }
            }
        }
        viewModelScope.launch {
            var wasOffline = false
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
                // Coming back online: refresh so the inbox isn't stale.
                if (online && wasOffline) load(refresh = true)
                wasOffline = !online
            }
        }
    }

    // Pinned chats first, then most recent.
    private fun List<Conversation>.inInboxOrder(): List<Conversation> =
        sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.lastMessageAt })

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.conversations.isEmpty(),
                    isRefreshing = refresh,
                    loadError = null
                )
            }
            messageRepository.getConversations().fold(
                onSuccess = { convs ->
                    _uiState.update {
                        it.copy(
                            conversations = convs.inInboxOrder(),
                            isLoading = false,
                            isRefreshing = false,
                            loadError = null
                        )
                    }
                },
                onFailure = { e ->
                    val hasData = _uiState.value.conversations.isNotEmpty()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            loadError = if (hasData) null else (e.message ?: "Couldn't load your chats")
                        )
                    }
                    if (hasData) _events.tryEmit(MessagesEvent.Info("Couldn't refresh. Check your connection."))
                }
            )
        }
    }

    fun setFilter(filter: String) { _uiState.update { it.copy(activeFilter = filter) } }

    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    /** Marks every unread chat as read (optimistically) – instant relief from the badge. */
    fun markAllRead() {
        val unread = _uiState.value.conversations.filter { it.myUnreadCount > 0 }
        if (unread.isEmpty()) return
        _uiState.update { s -> s.copy(conversations = s.conversations.map { it.copy(myUnreadCount = 0) }) }
        viewModelScope.launch {
            unread.forEach { messageRepository.markConversationRead(it.id) }
            _events.tryEmit(MessagesEvent.Info("All caught up"))
        }
    }

    /** True when the other person spoke last and it has been a while. */
    fun needsReply(conv: Conversation): Boolean {
        val sender = conv.lastMessageSender ?: return false
        if (sender == myUserId || conv.lastMessage.isNullOrBlank()) return false
        val mins = minutesSince(conv.lastMessageAt) ?: return false
        return mins >= NEEDS_REPLY_AFTER_MINUTES
    }

    fun filteredConversations(): List<Conversation> {
        val state = _uiState.value
        val byFilter = when (state.activeFilter) {
            "unread" -> state.conversations.filter { it.myUnreadCount > 0 }
            "needs_reply" -> state.conversations.filter { needsReply(it) }
            else -> state.conversations
        }
        val q = state.searchQuery.trim()
        if (q.isEmpty()) return byFilter
        return byFilter.filter {
            (it.otherUsername ?: "").contains(q, ignoreCase = true) ||
                (it.lastMessage ?: "").contains(q, ignoreCase = true)
        }
    }

    // ── Pin / mute (optimistic, reverted on failure) ─────────────────────────
    private fun updatePrefs(conv: Conversation, muted: Boolean, pinned: Boolean) {
        val before = _uiState.value.conversations
        _uiState.update { s ->
            s.copy(
                conversations = s.conversations
                    .map { if (it.id == conv.id) it.copy(isMuted = muted, isPinned = pinned) else it }
                    .inInboxOrder()
            )
        }
        viewModelScope.launch {
            messageRepository.setConversationPrefs(conv.id, muted, pinned).onFailure {
                _uiState.update { it.copy(conversations = before) }
                _events.tryEmit(MessagesEvent.Info("Couldn't update this chat. Try again."))
            }
        }
    }

    fun togglePin(conv: Conversation) = updatePrefs(conv, muted = conv.isMuted, pinned = !conv.isPinned)
    fun toggleMute(conv: Conversation) = updatePrefs(conv, muted = !conv.isMuted, pinned = conv.isPinned)

    // ── Delete ───────────────────────────────────────────────────────────────
    /** Removes the chat for me right away and offers Undo (restores it). */
    fun deleteConversationForMe(conv: Conversation) {
        val before = _uiState.value.conversations
        _uiState.update { s -> s.copy(conversations = s.conversations.filterNot { it.id == conv.id }) }
        viewModelScope.launch {
            messageRepository.deleteConversationForMe(conv.id).fold(
                onSuccess = {
                    _events.tryEmit(MessagesEvent.DeletedWithUndo(conv.id, conv.otherUsername ?: "chat"))
                },
                onFailure = {
                    _uiState.update { it.copy(conversations = before) }
                    _events.tryEmit(MessagesEvent.Info(it.message ?: "Couldn't delete this chat."))
                }
            )
        }
    }

    fun undoDelete(conversationId: String) {
        viewModelScope.launch {
            messageRepository.restoreConversationForMe(conversationId).fold(
                onSuccess = { load(refresh = true) },
                onFailure = { _events.tryEmit(MessagesEvent.Info("Couldn't restore this chat.")) }
            )
        }
    }

    fun deleteConversationForBoth(conversationId: String) {
        viewModelScope.launch {
            messageRepository.deleteConversationForBoth(conversationId).fold(
                onSuccess = {
                    _uiState.update { s -> s.copy(conversations = s.conversations.filterNot { it.id == conversationId }) }
                },
                onFailure = { err -> _events.tryEmit(MessagesEvent.Info(err.message ?: "Couldn't delete this chat.")) }
            )
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            messageRepository.subscribeToConversations().collect { updatedConv ->
                val current = _uiState.value.conversations.toMutableList()
                if (updatedConv.lastMessage == "__NAGPURPULSE_CONVERSATION_DELETED__" || updatedConv.lastMessage.isNullOrBlank()) {
                    // A conversation row is created when opening a chat, but it
                    // should not appear in either inbox until a message exists.
                    current.removeAll { it.id == updatedConv.id }
                } else {
                    val idx = current.indexOfFirst { it.id == updatedConv.id }
                    if (idx >= 0) {
                        // Realtime rows don't carry my per-user mute/pin, so keep what we already know.
                        current[idx] = updatedConv.copy(
                            isPinned = current[idx].isPinned,
                            isMuted = current[idx].isMuted
                        )
                    } else {
                        current.add(0, updatedConv)
                    }
                }
                _uiState.update { it.copy(conversations = current.inInboxOrder()) }
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
    val myId              = viewModel.myUserId
    val haptic            = rememberHaptic()
    // Muted chats shouldn't nag: leave them out of the tab-bar badge.
    val totalUnread       = uiState.conversations.filterNot { it.isMuted }.sumOf { it.myUnreadCount }
    val unreadChats       = uiState.conversations.count { it.myUnreadCount > 0 }
    val needsReplyCount   = uiState.conversations.count { viewModel.needsReply(it) }
    val snackbarHostState = remember { SnackbarHostState() }
    var headerVisible     by remember { mutableStateOf(false) }
    var showSafetyDialog  by remember { mutableStateOf(false) }
    var searchActive      by remember { mutableStateOf(false) }
    val searchFocus       = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(60); headerVisible = true }

    // Back closes the search first instead of leaving the screen.
    BackHandler(enabled = searchActive) {
        searchActive = false
        viewModel.setSearchQuery("")
    }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            delay(80)
            runCatching { searchFocus.requestFocus() }
        }
    }

    // One-off events: info snackbars and "chat deleted · Undo".
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MessagesEvent.Info -> snackbarHostState.showSnackbar(event.text)
                is MessagesEvent.DeletedWithUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Chat with ${event.name} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.conversationId)
                }
            }
        }
    }

    // Everyone currently online that I already chat with.
    val activeNow = remember(uiState.conversations, onlineUserIds) {
        uiState.conversations.filter { it.otherUserId != null && it.otherUserId in onlineUserIds }
    }

    if (showSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyDialog = false },
            icon = { Icon(Icons.Filled.Shield, contentDescription = null, tint = GreenSuccess) },
            title = { Text("Private by design") },
            text = {
                Text("You're in a space designed for private conversations. Your chats are intended for you and the person you're messaging. You can mute, block or report anyone from inside a chat. Still, never share passwords, OTPs, financial details, or anything you wouldn't want the other person to save or screenshot.")
            },
            confirmButton = {
                TextButton(onClick = { showSafetyDialog = false }) { Text("Got it") }
            },
            containerColor = Surface
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AnimatedVisibility(headerVisible, enter = fadeIn(tween(350)) + slideInVertically { -30 }) {
                Column(modifier = Modifier.background(Brush.verticalGradient(
                    listOf(Surface, Background), 0f, 220f
                )).statusBarsPadding()) {
                    if (searchActive) {
                        // Local search over my chats (name + last message).
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceAlt)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f)) {
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        singleLine = true,
                                        textStyle = TextStyle(color = PrimaryText, fontSize = 16.sp),
                                        cursorBrush = SolidColor(OrangePrimary),
                                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                                        decorationBox = { inner ->
                                            Box {
                                                if (uiState.searchQuery.isEmpty()) {
                                                    Text("Search chats", color = TertiaryText, fontSize = 16.sp)
                                                }
                                                inner()
                                            }
                                        }
                                    )
                                }
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = SecondaryText, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            TextButton(onClick = {
                                searchActive = false
                                viewModel.setSearchQuery("")
                            }) { Text("Cancel", color = OrangePrimary, fontWeight = FontWeight.SemiBold) }
                        }
                    } else {
                        // Slim title row: title + unread count + tagline, search, safety info.
                        Row(
                            Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Messages", color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (totalUnread > 0) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (totalUnread > 99) "99+" else "$totalUnread",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(OrangePrimary)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Incognito chats, real connections", color = SecondaryText, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TopBarIcon(Icons.Filled.Search, "Search chats") { searchActive = true }
                            Spacer(Modifier.width(8.dp))
                            TopBarIcon(Icons.Filled.Shield, "About private chats") { showSafetyDialog = true }
                        }
                    }

                    // Filter pills
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InboxFilterChip("All", 0, uiState.activeFilter == "all", OrangePrimary) {
                            haptic.tap(); viewModel.setFilter("all")
                        }
                        InboxFilterChip("Unread", unreadChats, uiState.activeFilter == "unread", OrangePrimary) {
                            haptic.tap(); viewModel.setFilter("unread")
                        }
                        // Only offered when it is useful (or already selected).
                        if (needsReplyCount > 0 || uiState.activeFilter == "needs_reply") {
                            InboxFilterChip("Needs reply", needsReplyCount, uiState.activeFilter == "needs_reply", BlueInfo) {
                                haptic.tap(); viewModel.setFilter("needs_reply")
                            }
                        }
                        // Instant relief from the badge.
                        if (unreadChats > 0) {
                            TextButton(onClick = { haptic.tap(); viewModel.markAllRead() }) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mark all read", color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    HorizontalDivider(color = Divider, thickness = 0.5.dp)
                    OfflineBanner(
                        visible = uiState.isOffline,
                        message = "You're offline. Chats may be out of date."
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.tap()
                    navController.navigate(Screen.UserSearch.route)
                },
                containerColor = OrangePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.AddComment, contentDescription = null) },
                text = { Text("New chat", fontWeight = FontWeight.SemiBold) }
            )
        },
        bottomBar = {
            BottomNavBar(navController = navController, onCreatePost = onCreatePost, hasAlertBadge = false, messageCount = totalUnread)
        }
    ) { padding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.load(true) },
            modifier = Modifier.padding(padding),
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    contentColor = OrangePrimary,
                    backgroundColor = Surface
                )
            }
        ) {
            when {
                uiState.isLoading -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(8) { ShimmerConversationRow() }
                }

                uiState.loadError != null && uiState.conversations.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = Icons.Filled.CloudOff,
                        title = "Can't load your chats",
                        subtitle = "Check your connection and try again.",
                        ctaLabel = "Try again",
                        onCta = { viewModel.load() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.conversations.isEmpty() -> InboxEmptyState(
                    onFindPeople = { navController.navigate(Screen.UserSearch.route) }
                )

                uiState.searchQuery.isNotBlank() && filtered.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No chats match \"${uiState.searchQuery.trim()}\"",
                        subtitle = "Try a different name, or find someone new.",
                        ctaLabel = "Find people",
                        onCta = { navController.navigate(Screen.UserSearch.route) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                filtered.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    val (icon, title, subtitle) = when (uiState.activeFilter) {
                        "unread" -> Triple(Icons.Filled.DoneAll, "You're all caught up", "No unread messages. Nicely done.")
                        else -> Triple(Icons.Filled.DoneAll, "Nobody's waiting on you", "You've replied to everyone. 🎉")
                    }
                    EmptyState(
                        icon = icon,
                        title = title,
                        subtitle = subtitle,
                        ctaLabel = "Show all chats",
                        onCta = { viewModel.setFilter("all") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    // Pinned chats get their own labelled section once there is something to separate.
                    val entries = remember(filtered, uiState.searchQuery) {
                        val pinned = filtered.filter { it.isPinned }
                        val others = filtered.filterNot { it.isPinned }
                        val sections = pinned.isNotEmpty() && others.isNotEmpty() && uiState.searchQuery.isBlank()
                        buildList<InboxEntry> {
                            if (sections) add(InboxEntry.Label("Pinned"))
                            pinned.forEach { add(InboxEntry.Chat(it)) }
                            if (sections) add(InboxEntry.Label("Chats"))
                            others.forEach { add(InboxEntry.Chat(it)) }
                        }
                    }

                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                        // "Active now" lives in the list so it scrolls away with the chats.
                        if (activeNow.isNotEmpty() && uiState.activeFilter == "all" && uiState.searchQuery.isBlank()) {
                            item(key = "active_now") {
                                ActiveNowRow(
                                    items = activeNow,
                                    onClick = { conv -> navController.navigate(Screen.Chat.createRoute(conv.id)) }
                                )
                            }
                        }
                        items(entries, key = { it.key }) { entry ->
                            when (entry) {
                                is InboxEntry.Label -> SectionLabel(entry.text, Modifier.animateItem())
                                is InboxEntry.Chat -> {
                                    val conv = entry.conv
                                    SwipeableConversationRow(
                                        conv = conv,
                                        isOnline = conv.otherUserId in onlineUserIds,
                                        isLastFromMe = conv.lastMessageSender != null && conv.lastMessageSender == myId,
                                        needsReply = viewModel.needsReply(conv),
                                        draft = uiState.drafts[conv.id],
                                        modifier = Modifier.animateItem(),
                                        onClick = { navController.navigate(Screen.Chat.createRoute(conv.id)) },
                                        onViewProfile = {
                                            conv.otherUserId?.takeIf { it.isNotBlank() }?.let {
                                                navController.navigate(Screen.UserProfile.createRoute(it))
                                            }
                                        },
                                        onTogglePin = {
                                            haptic.tap()
                                            viewModel.togglePin(conv)
                                        },
                                        onToggleMute = { viewModel.toggleMute(conv) },
                                        onDeleteForMe = {
                                            haptic.alert()
                                            viewModel.deleteConversationForMe(conv)
                                        },
                                        onDeleteForBoth = { viewModel.deleteConversationForBoth(conv.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Rows of the inbox list: a section label or a conversation. */
private sealed interface InboxEntry {
    val key: String

    data class Label(val text: String) : InboxEntry {
        override val key: String get() = "label_$text"
    }

    data class Chat(val conv: Conversation) : InboxEntry {
        override val key: String get() = conv.id
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TertiaryText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

// ── Pieces ────────────────────────────────────────────────────────────────────

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
            .semantics { this.contentDescription = contentDescription }
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = SecondaryText, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InboxFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else SurfaceAlt,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "inbox_filter_pill"
    )
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 0) "$label  $count" else label,
            color = if (selected) Color.White else SecondaryText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun ActiveNowRow(
    items: List<Conversation>,
    onClick: (Conversation) -> Unit
) {
    Column(Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(GreenSuccess))
            Spacer(Modifier.width(8.dp))
            Text("Active now", color = PrimaryText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { "active_${it.id}" }) { conv ->
                val seed = conv.otherAvatarSeed ?: conv.otherUserId ?: "anon"
                Column(
                    modifier = Modifier
                        .width(60.dp)
                        .semantics { contentDescription = "${conv.otherUsername ?: "Someone"} is active now" }
                        .pressScale { onClick(conv) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IncognitoAvatar(seed = seed, size = 54.dp, isOnline = true)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        conv.otherUsername ?: "Incognito",
                        color = SecondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Divider.copy(alpha = 0.6f), thickness = 0.5.dp)
    }
}

/** Empty inbox is the activation moment: explain how to start, not just "nothing here". */
@Composable
private fun InboxEmptyState(onFindPeople: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(OrangePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text("🦊", fontSize = 40.sp) }
        Spacer(Modifier.height(16.dp))
        Text("Your inbox is waiting", color = PrimaryText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Private, incognito chats with people around Nagpur.", color = SecondaryText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        listOf(
            "👋  Say hi to someone whose post you liked",
            "💬  Take a long comment thread to a private chat",
            "🛡️  Block or report anyone, anytime"
        ).forEach { tip ->
            Text(tip, color = SecondaryText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onFindPeople,
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary, contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Find people", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Swipe right to pin / unpin, swipe left to delete (with Undo). The row snaps back either way. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableConversationRow(
    conv: Conversation,
    isOnline: Boolean,
    isLastFromMe: Boolean,
    needsReply: Boolean,
    draft: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onViewProfile: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForBoth: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onTogglePin()
                SwipeToDismissBoxValue.EndToStart -> onDeleteForMe()
                else -> Unit
            }
            // Never "settle" in the dismissed position: pin keeps the row, delete removes it from the list.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bg = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> OrangePrimary
                SwipeToDismissBoxValue.EndToStart -> RedAlert
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd ->
                        Icon(Icons.Filled.PushPin, contentDescription = if (conv.isPinned) "Unpin" else "Pin", tint = Color.White)
                    SwipeToDismissBoxValue.EndToStart ->
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color.White)
                    else -> Unit
                }
            }
        }
    ) {
        ConversationRow(
            conv = conv,
            isOnline = isOnline,
            isLastFromMe = isLastFromMe,
            needsReply = needsReply,
            draft = draft,
            onClick = onClick,
            onViewProfile = onViewProfile,
            onTogglePin = onTogglePin,
            onToggleMute = onToggleMute,
            onDeleteForMe = onDeleteForMe,
            onDeleteForBoth = onDeleteForBoth
        )
    }
}

@Composable
fun ConversationRow(
    conv: Conversation,
    isOnline: Boolean = false,
    isLastFromMe: Boolean = false,
    needsReply: Boolean = false,
    draft: String? = null,
    onClick: () -> Unit,
    onViewProfile: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onDeleteForMe: () -> Unit = {},
    onDeleteForBoth: () -> Unit = {}
) {
    val seed = conv.otherAvatarSeed ?: conv.otherUserId ?: "anon"
    val displayName = conv.otherUsername ?: "Incognito"
    val hasUnread = conv.myUnreadCount > 0
    var showMenu by remember(conv.id) { mutableStateOf(false) }
    var confirmDeleteBoth by remember(conv.id) { mutableStateOf(false) }

    if (confirmDeleteBoth) {
        AlertDialog(
            onDismissRequest = { confirmDeleteBoth = false },
            containerColor = Surface,
            titleContentColor = PrimaryText,
            textContentColor = SecondaryText,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete chat for both?") },
            text = { Text("This deletes the shared conversation and its messages for both participants. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDeleteBoth = false; onDeleteForBoth() }) { Text("Delete for both", color = RedAlert) } },
            dismissButton = { TextButton(onClick = { confirmDeleteBoth = false }) { Text("Cancel", color = SecondaryText) } }
        )
    }

    val lastMessage = conv.lastMessage.orEmpty()
    val previewDeleted = isDeletedMessage(lastMessage)
    val previewText = when {
        previewDeleted -> "Message deleted"
        isLastFromMe -> "You: $lastMessage"
        else -> lastMessage
    }
    val badgeColor = if (conv.isMuted) TertiaryText else OrangePrimary

    Box(modifier = Modifier.fillMaxWidth().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(if (hasUnread && !conv.isMuted) OrangePrimary.copy(0.04f) else Color.Transparent)
                .pointerInput(conv.id) {
                    detectTapGestures(onTap = { onClick() }, onLongPress = { showMenu = true })
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IncognitoAvatar(seed = seed, size = 52.dp, isOnline = isOnline)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(displayName, color = PrimaryText, fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (conv.isPinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = OrangePrimary, modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatConvTime(conv.lastMessageAt),
                        color = if (hasUnread && !conv.isMuted) OrangePrimary else TertiaryText,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!draft.isNullOrBlank()) {
                        // Unfinished business stands out: "Draft: …" in orange.
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.SemiBold)) { append("Draft: ") }
                                append(draft)
                            },
                            color = TertiaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else Text(
                        previewText,
                        color = if (hasUnread) PrimaryText.copy(alpha = 0.85f) else TertiaryText,
                        fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                        fontStyle = if (previewDeleted) FontStyle.Italic else FontStyle.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        hasUnread -> Box(
                            Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).clip(CircleShape).background(badgeColor).padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (conv.myUnreadCount > 99) "99+" else "${conv.myUnreadCount}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        needsReply -> Text(
                            "Reply",
                            color = BlueInfo,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BlueInfo.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (conv.isMuted) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.NotificationsOff, contentDescription = "Muted", tint = TertiaryText, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        if (showMenu) {
            Dialog(onDismissRequest = { showMenu = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
                        )
                        ConversationOption(icon = Icons.Filled.ChatBubbleOutline, label = "Open chat", onClick = { showMenu = false; onClick() })
                        ConversationOption(icon = Icons.Filled.Person, label = "View profile", enabled = !conv.otherUserId.isNullOrBlank(), onClick = { showMenu = false; onViewProfile() })
                        ConversationOption(icon = Icons.Filled.PushPin, label = if (conv.isPinned) "Unpin" else "Pin to top", onClick = { showMenu = false; onTogglePin() })
                        ConversationOption(
                            icon = if (conv.isMuted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            label = if (conv.isMuted) "Unmute" else "Mute",
                            onClick = { showMenu = false; onToggleMute() }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Divider.copy(alpha = 0.65f))
                        ConversationOption(icon = Icons.Filled.DeleteOutline, label = "Delete for me", destructive = true, onClick = { showMenu = false; onDeleteForMe() })
                        ConversationOption(icon = Icons.Filled.DeleteForever, label = "Delete for both", destructive = true, onClick = { showMenu = false; confirmDeleteBoth = true })
                        TextButton(onClick = { showMenu = false }, modifier = Modifier.align(Alignment.End).padding(top = 4.dp)) { Text("Cancel", color = SecondaryText) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        destructive -> RedAlert
        else -> PrimaryText
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
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
