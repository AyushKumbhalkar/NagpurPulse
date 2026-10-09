// This is the ChatScreen.kt file

// java/com/nagpurpulse/ui/screens/messages/ChatScreen.kt

package com.nagpurpulse.ui.screens.messages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.local.ChatDraftStore
import com.nagpurpulse.data.local.ConnectivityObserver
import com.nagpurpulse.data.model.Message
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.data.repository.TypingSession
import com.nagpurpulse.ui.components.EmptyState
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ── ViewModel ─────────────────────────────────────────────────────────────────
private const val CHAT_PAGE_SIZE = 30
private const val MAX_MESSAGE_LENGTH = 2000
private const val STATE_SENDING = "sending"
private const val STATE_FAILED = "failed"

data class ChatUiState(
    // Ascending (oldest -> newest). Includes locally pending / failed messages.
    val messages: List<Message> = emptyList(),
    val otherUsername: String = "Chat",
    val otherAvatarSeed: String = "anon",
    val otherUserId: String = "",
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val hasMoreOlder: Boolean = false,
    val loadError: String? = null,
    val isOtherOnline: Boolean = false,
    val isOtherTyping: Boolean = false,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isOffline: Boolean = false,
    // First message that was unread when the chat was opened (for the "New messages" divider).
    val firstUnreadId: String? = null,
    val replyingTo: Message? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val presenceRepository: PresenceRepository,
    private val draftStore: ChatDraftStore,
    private val connectivity: ConnectivityObserver,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val conversationId: String = savedStateHandle["conversationId"] ?: ""
    val myUserId get() = authRepository.currentUserId ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    /** Unsent text from the last visit to this chat (shown in the input and as "Draft:" in the inbox). */
    val initialDraft: String = draftStore.get(authRepository.currentUserId ?: "", conversationId)

    // One-off snackbar texts.
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events

    // How many rows the server has returned so far (offset for the next older page).
    private var serverLoaded = 0

    private var typingSession: TypingSession? = null
    private var typingStopJob: Job? = null
    private var iAmTyping = false

    init {
        presenceRepository.start()
        viewModelScope.launch {
            presenceRepository.onlineUserIds.collect { ids ->
                _uiState.update { it.copy(isOtherOnline = it.otherUserId in ids) }
            }
        }
        loadMessages()
        subscribeRealtime()
        openTyping()
        observeConnectivity()
    }

    // ── Connectivity ─────────────────────────────────────────────────────────
    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
                // Back online: push out anything that failed while we were away.
                if (online) {
                    _uiState.value.messages
                        .filter { it.sendState == STATE_FAILED }
                        .forEach { retrySend(it.id) }
                }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            val pageTask = async { messageRepository.getMessagesPage(conversationId, 0, CHAT_PAGE_SIZE) }
            val conversationTask = async { messageRepository.getConversationById(conversationId) }
            val blockedTask = async { messageRepository.getBlockedUserIds() }
            val pageResult = pageTask.await()
            val conv = conversationTask.await().getOrNull()
            val blocked = blockedTask.await()

            pageResult.fold(
                onSuccess = { page ->
                    serverLoaded = page.serverCount
                    val otherId = conv?.otherUserId ?: ""
                    val me = myUserId
                    // Remember where the unread block starts BEFORE we mark everything read.
                    val firstUnread = page.messages.firstOrNull { it.senderId != me && !it.isRead }?.id
                    _uiState.update {
                        it.copy(
                            messages = page.messages,
                            hasMoreOlder = page.hasMore,
                            otherUsername = conv?.otherUsername ?: "Chat",
                            otherAvatarSeed = conv?.otherAvatarSeed ?: "anon",
                            otherUserId = otherId,
                            isOtherOnline = otherId in presenceRepository.onlineUserIds.value,
                            isMuted = conv?.isMuted ?: false,
                            isPinned = conv?.isPinned ?: false,
                            isBlockedByMe = otherId.isNotBlank() && otherId in blocked,
                            firstUnreadId = firstUnread,
                            isLoading = false,
                            loadError = null
                        )
                    }
                    messageRepository.markConversationRead(conversationId)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.message ?: "Couldn't load this chat") }
                }
            )
        }
    }

    fun retryLoad() = loadMessages()

    /** Prepends the next page of older messages. */
    fun loadOlder() {
        val s = _uiState.value
        if (s.isLoading || s.isLoadingOlder || !s.hasMoreOlder) return
        _uiState.update { it.copy(isLoadingOlder = true) }
        viewModelScope.launch {
            messageRepository.getMessagesPage(conversationId, serverLoaded, CHAT_PAGE_SIZE).fold(
                onSuccess = { page ->
                    serverLoaded += page.serverCount
                    _uiState.update { cur ->
                        val existing = cur.messages.map { it.id }.toSet()
                        val older = page.messages.filter { it.id !in existing }
                        cur.copy(
                            messages = older + cur.messages,
                            hasMoreOlder = page.hasMore,
                            isLoadingOlder = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingOlder = false) }
                    _events.tryEmit("Couldn't load earlier messages.")
                }
            )
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {
            messageRepository.subscribeToMessages(conversationId).collect { incoming ->
                val me = myUserId
                _uiState.update { s ->
                    val list = s.messages.toMutableList()
                    val idx = list.indexOfFirst { it.id == incoming.id }
                    when {
                        // Existing message changed: read receipt, edit, delete or reaction.
                        idx >= 0 -> list[idx] = incoming.copy(
                            senderUsername = incoming.senderUsername ?: list[idx].senderUsername
                        )
                        // My own message arriving over realtime: replace the matching pending bubble.
                        incoming.senderId == me -> {
                            val pendingIdx = list.indexOfFirst {
                                it.sendState == STATE_SENDING && it.senderId == me && it.content == incoming.content
                            }
                            if (pendingIdx >= 0) list[pendingIdx] = incoming else list.add(incoming)
                        }
                        else -> list.add(incoming)
                    }
                    s.copy(messages = list)
                }
                if (incoming.senderId != me) {
                    messageRepository.markConversationRead(conversationId)
                }
            }
        }
    }

    // ── Typing indicator ─────────────────────────────────────────────────────
    private fun openTyping() {
        viewModelScope.launch {
            val session = messageRepository.openTypingSession(viewModelScope, conversationId) ?: return@launch
            typingSession = session
            session.othersTyping.collect { ids ->
                _uiState.update { it.copy(isOtherTyping = ids.isNotEmpty()) }
            }
        }
    }

    /** Call on every keystroke; announces "typing" and auto-clears after 3s of silence. */
    fun onInputChanged(text: String) {
        draftStore.set(myUserId, conversationId, text)
        if (text.isBlank()) { stopTyping(); return }
        typingStopJob?.cancel()
        if (!iAmTyping) {
            iAmTyping = true
            viewModelScope.launch { typingSession?.setTyping(true) }
        }
        typingStopJob = viewModelScope.launch {
            delay(3_000)
            stopTyping()
        }
    }

    private fun stopTyping() {
        typingStopJob?.cancel()
        if (iAmTyping) {
            iAmTyping = false
            viewModelScope.launch { typingSession?.setTyping(false) }
        }
    }

    // ── Sending (optimistic) ─────────────────────────────────────────────────
    fun sendMessage(content: String) {
        val text = content.trim()
        if (text.isEmpty() || _uiState.value.isBlockedByMe) return
        val reply = _uiState.value.replyingTo
        val localId = "local-" + java.util.UUID.randomUUID()
        val pending = Message(
            id = localId,
            conversationId = conversationId,
            senderId = myUserId,
            content = text,
            createdAt = java.time.Instant.now().toString(),
            replyToId = reply?.id,
            sendState = STATE_SENDING
        )
        // Show it instantly; confirm (or fail) in the background.
        _uiState.update { it.copy(messages = it.messages + pending, replyingTo = null) }
        draftStore.set(myUserId, conversationId, "")
        stopTyping()
        dispatchSend(localId, text, reply?.id)
    }

    private fun dispatchSend(localId: String, text: String, replyToId: String?) {
        viewModelScope.launch {
            messageRepository.sendMessage(conversationId, text, replyToId).fold(
                onSuccess = { real ->
                    _uiState.update { s ->
                        val list = s.messages.toMutableList()
                        val localIdx = list.indexOfFirst { it.id == localId }
                        val realIdx = list.indexOfFirst { it.id == real.id }
                        when {
                            // Realtime already inserted the real row: just drop the pending bubble.
                            localIdx >= 0 && realIdx >= 0 -> list.removeAt(localIdx)
                            localIdx >= 0 -> list[localIdx] = real
                            realIdx < 0 -> list.add(real)
                        }
                        s.copy(messages = list)
                    }
                },
                onFailure = { e ->
                    updateMessage(localId) { it.copy(sendState = STATE_FAILED) }
                    // While offline the banner already explains it; don't stack a snackbar per message.
                    if (!_uiState.value.isOffline) {
                        _events.tryEmit(e.message?.takeIf { it.length < 80 } ?: "Message not sent. Tap it to retry.")
                    }
                }
            )
        }
    }

    fun retrySend(localId: String) {
        val msg = _uiState.value.messages.firstOrNull { it.id == localId && it.sendState == STATE_FAILED } ?: return
        updateMessage(localId) { it.copy(sendState = STATE_SENDING) }
        dispatchSend(localId, msg.content, msg.replyToId)
    }

    fun discardFailed(localId: String) {
        _uiState.update { s -> s.copy(messages = s.messages.filterNot { it.id == localId && it.sendState == STATE_FAILED }) }
    }

    private fun updateMessage(id: String, transform: (Message) -> Message) {
        _uiState.update { s -> s.copy(messages = s.messages.map { if (it.id == id) transform(it) else it }) }
    }

    // ── Reply / reactions ────────────────────────────────────────────────────
    fun startReply(msg: Message) {
        if (msg.sendState.isNotEmpty() || isDeletedMessage(msg.content)) return
        _uiState.update { it.copy(replyingTo = msg) }
    }

    fun cancelReply() { _uiState.update { it.copy(replyingTo = null) } }

    fun toggleReaction(messageId: String, emoji: String) {
        val target = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (target.sendState.isNotEmpty() || isDeletedMessage(target.content)) return
        val previous = target.reactions
        val me = myUserId
        updateMessage(messageId) { it.copy(reactions = applyReaction(it.reactions, me, emoji)) }
        viewModelScope.launch {
            messageRepository.toggleReaction(messageId, emoji).onFailure {
                updateMessage(messageId) { m -> m.copy(reactions = previous) }
                _events.tryEmit("Couldn't add that reaction.")
            }
        }
    }

    // ── Existing message actions ─────────────────────────────────────────────
    fun deleteMessageForMe(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForMe(messageId).fold(
                onSuccess = { _uiState.update { s -> s.copy(messages = s.messages.filterNot { it.id == messageId }) } },
                onFailure = { err -> _events.tryEmit(err.message ?: "Couldn't delete that message.") }
            )
        }
    }

    fun deleteMessageForBoth(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForBoth(messageId).fold(
                onSuccess = { updateMessage(messageId) { it.copy(content = DELETED_MESSAGE_TEXT, reactions = emptyMap()) } },
                onFailure = { err -> _events.tryEmit(err.message ?: "Couldn't delete that message.") }
            )
        }
    }

    fun editMessage(messageId: String, content: String) {
        val text = content.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            messageRepository.editMessage(messageId, text).fold(
                onSuccess = { updateMessage(messageId) { it.copy(content = text, editedAt = java.time.Instant.now().toString()) } },
                onFailure = { err -> _events.tryEmit(err.message ?: "Couldn't edit that message.") }
            )
        }
    }

    // ── Safety: mute / block / report ────────────────────────────────────────
    fun toggleMute() {
        val s = _uiState.value
        val newMuted = !s.isMuted
        _uiState.update { it.copy(isMuted = newMuted) }
        viewModelScope.launch {
            messageRepository.setConversationPrefs(conversationId, newMuted, s.isPinned).fold(
                onSuccess = { _events.tryEmit(if (newMuted) "Chat muted" else "Chat unmuted") },
                onFailure = {
                    _uiState.update { it.copy(isMuted = !newMuted) }
                    _events.tryEmit("Couldn't update this chat.")
                }
            )
        }
    }

    fun blockOther() {
        val other = _uiState.value.otherUserId
        if (other.isBlank()) return
        viewModelScope.launch {
            messageRepository.blockUser(other).fold(
                onSuccess = {
                    stopTyping()
                    _uiState.update { it.copy(isBlockedByMe = true, replyingTo = null) }
                    _events.tryEmit("Blocked ${_uiState.value.otherUsername}")
                },
                onFailure = { _events.tryEmit("Couldn't block. Please try again.") }
            )
        }
    }

    fun unblockOther() {
        val other = _uiState.value.otherUserId
        if (other.isBlank()) return
        viewModelScope.launch {
            messageRepository.unblockUser(other).fold(
                onSuccess = {
                    _uiState.update { it.copy(isBlockedByMe = false) }
                    _events.tryEmit("Unblocked ${_uiState.value.otherUsername}")
                },
                onFailure = { _events.tryEmit("Couldn't unblock. Please try again.") }
            )
        }
    }

    fun reportOther(reason: String, alsoBlock: Boolean) {
        val other = _uiState.value.otherUserId
        if (other.isBlank()) return
        viewModelScope.launch {
            messageRepository.reportUser(conversationId, other, reason).fold(
                onSuccess = {
                    _events.tryEmit("Thanks. We'll review this report.")
                    if (alsoBlock) blockOther()
                },
                onFailure = { _events.tryEmit("Couldn't send the report. Please try again.") }
            )
        }
    }

    override fun onCleared() {
        // Leave the typing channel even though viewModelScope is already cancelled.
        val session = typingSession
        typingSession = null
        if (session != null) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                session.setTyping(false)
                session.close()
            }
        }
        super.onCleared()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
private val ConversationStarters = listOf("👋 Hey!", "How's it going?", "Saw your post 🙂")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOtherProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState      = viewModel.uiState.collectAsState().value
    var messageText  by remember { mutableStateOf(viewModel.initialDraft) }
    val listState    = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    var showChatMenu by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }
    var deletingMessage by remember { mutableStateOf<Message?>(null) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var pendingLink by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    val reversedMessages = remember(uiState.messages) { uiState.messages.asReversed() }
    val messagesById = remember(uiState.messages) { uiState.messages.associateBy { it.id } }
    val myId = viewModel.myUserId

    // ── Scrolling rules ──────────────────────────────────────────────────────
    // Newest message is index 0 (reverseLayout). "At bottom" tolerates the typing bubble at index 0.
    val atBottom by remember { derivedStateOf { listState.firstVisibleItemIndex <= 1 } }
    var newBelow by remember { mutableIntStateOf(0) }
    var lastSeenNewestId by remember { mutableStateOf<String?>(null) }
    val newestMessage = uiState.messages.lastOrNull()

    LaunchedEffect(newestMessage?.id) {
        val newest = newestMessage ?: return@LaunchedEffect
        if (lastSeenNewestId == null) {
            // First render: reverseLayout already shows the newest message.
            lastSeenNewestId = newest.id
            return@LaunchedEffect
        }
        lastSeenNewestId = newest.id
        // A soft tick when a message lands while you're reading the latest.
        if (newest.senderId != myId && atBottom) haptic.tap()
        // Follow along for my own messages or when already reading the latest;
        // otherwise don't yank the user away from history, count it instead.
        if (newest.senderId == myId || atBottom) {
            listState.animateScrollToItem(0)
            newBelow = 0
        } else {
            newBelow += 1
        }
    }
    LaunchedEffect(atBottom) { if (atBottom) newBelow = 0 }

    // When the keyboard opens, keep the latest message in view (unless reading old history).
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (imeVisible && listState.firstVisibleItemIndex <= 3) listState.animateScrollToItem(0)
    }

    // Load older pages as the user nears the top of the history.
    val nearOldest by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearOldest, uiState.messages.size) {
        if (nearOldest) viewModel.loadOlder()
    }

    val sendEnabled  = messageText.isNotBlank()
    val sendBg by animateColorAsState(
        if (sendEnabled) OrangePrimary else SurfaceAlt,
        tween(200),
        label = "send_bg"
    )

    // ── Dialogs (all theme-aware) ────────────────────────────────────────────
    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            containerColor = Surface,
            titleContentColor = PrimaryText,
            textContentColor = SecondaryText,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { if (it.length <= MAX_MESSAGE_LENGTH) editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        cursorColor = OrangePrimary,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    )
                )
            },
            confirmButton = { TextButton(onClick = { editingMessage?.let { viewModel.editMessage(it.id, editText) }; editingMessage = null }) { Text("Save", color = OrangePrimary) } },
            dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Cancel", color = SecondaryText) } }
        )
    }
    if (deletingMessage != null) {
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            containerColor = Surface,
            titleContentColor = PrimaryText,
            textContentColor = SecondaryText,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete message for both?") },
            text = { Text("This replaces the message content for both participants.") },
            confirmButton = { TextButton(onClick = { deletingMessage?.let { viewModel.deleteMessageForBoth(it.id) }; deletingMessage = null }) { Text("Delete for both", color = RedAlert) } },
            dismissButton = { TextButton(onClick = { deletingMessage = null }) { Text("Cancel", color = SecondaryText) } }
        )
    }
    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            containerColor = Surface,
            titleContentColor = PrimaryText,
            textContentColor = SecondaryText,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Filled.Block, contentDescription = null, tint = RedAlert) },
            title = { Text("Block ${uiState.otherUsername}?") },
            text = { Text("They won't be able to message you and you won't be able to message them. You can unblock any time from this chat.") },
            confirmButton = { TextButton(onClick = { showBlockConfirm = false; viewModel.blockOther() }) { Text("Block", color = RedAlert, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel", color = SecondaryText) } }
        )
    }
    pendingLink?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingLink = null },
            containerColor = Surface,
            titleContentColor = PrimaryText,
            textContentColor = SecondaryText,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Filled.Link, contentDescription = null, tint = OrangePrimary) },
            title = { Text("Open ${urlHost(url)}?", maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Text(url, color = PrimaryText, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Text("Only open links from people you trust. Never enter passwords or OTPs on a site you reached from a message.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingLink = null
                    runCatching { uriHandler.openUri(url) }
                }) { Text("Open", color = OrangePrimary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(url))
                        pendingLink = null
                        scope.launch { snackbarHostState.showSnackbar("Link copied") }
                    }) { Text("Copy", color = SecondaryText) }
                    TextButton(onClick = { pendingLink = null }) { Text("Cancel", color = SecondaryText) }
                }
            }
        )
    }
    if (showReport) {
        ReportDialog(
            username = uiState.otherUsername,
            isAlreadyBlocked = uiState.isBlockedByMe,
            onDismiss = { showReport = false },
            onSubmit = { reason, alsoBlock ->
                showReport = false
                viewModel.reportOther(reason, alsoBlock)
            }
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 120f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }

                IncognitoAvatar(
                    seed = uiState.otherAvatarSeed,
                    size = 40.dp,
                    isOnline = uiState.isOtherOnline,
                    modifier = Modifier.pressScale(onClick = {
                        uiState.otherUserId.takeIf { it.isNotBlank() }?.let(onOtherProfileClick)
                    })
                )

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f).pressScale(onClick = {
                    uiState.otherUserId.takeIf { it.isNotBlank() }?.let(onOtherProfileClick)
                })) {
                    Text(
                        text = uiState.otherUsername,
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // typing > active now > profile hint
                    val (statusText, statusColor) = when {
                        uiState.isBlockedByMe -> "Blocked" to RedAlert
                        uiState.isOtherTyping -> "typing…" to OrangePrimary
                        uiState.isOtherOnline -> "Active now" to GreenSuccess
                        else -> "View profile" to SecondaryText
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = if (uiState.isOtherTyping) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (uiState.isMuted) {
                    Icon(Icons.Filled.NotificationsOff, contentDescription = "Muted", tint = TertiaryText, modifier = Modifier.size(18.dp))
                }

                Box {
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Chat options", tint = SecondaryText)
                    }
                    DropdownMenu(
                        expanded = showChatMenu,
                        onDismissRequest = { showChatMenu = false },
                        modifier = Modifier
                            .background(Surface)
                            .border(1.dp, Divider, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("View profile", color = PrimaryText) },
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = OrangePrimary) },
                            enabled = uiState.otherUserId.isNotBlank(),
                            onClick = {
                                showChatMenu = false
                                uiState.otherUserId.takeIf { it.isNotBlank() }?.let(onOtherProfileClick)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (uiState.isMuted) "Unmute notifications" else "Mute notifications", color = PrimaryText) },
                            leadingIcon = {
                                Icon(
                                    if (uiState.isMuted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                    null, tint = OrangePrimary
                                )
                            },
                            onClick = { showChatMenu = false; viewModel.toggleMute() }
                        )
                        HorizontalDivider(color = Divider.copy(alpha = 0.6f))
                        DropdownMenuItem(
                            text = { Text(if (uiState.isBlockedByMe) "Unblock" else "Block", color = RedAlert) },
                            leadingIcon = { Icon(Icons.Filled.Block, null, tint = RedAlert) },
                            enabled = uiState.otherUserId.isNotBlank(),
                            onClick = {
                                showChatMenu = false
                                if (uiState.isBlockedByMe) viewModel.unblockOther() else showBlockConfirm = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Report", color = RedAlert) },
                            leadingIcon = { Icon(Icons.Filled.Flag, null, tint = RedAlert) },
                            enabled = uiState.otherUserId.isNotBlank(),
                            onClick = { showChatMenu = false; showReport = true }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                OfflineBanner(
                    visible = uiState.isOffline,
                    message = "You're offline. Messages will send when you're back."
                )

                // Reply preview
                AnimatedVisibility(visible = uiState.replyingTo != null && !uiState.isBlockedByMe) {
                    val target = uiState.replyingTo
                    if (target != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceAlt)
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(Modifier.width(4.dp).fillMaxHeight().background(OrangePrimary))
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        if (target.senderId == myId) "Replying to yourself" else "Replying to ${uiState.otherUsername}",
                                        color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text(target.content, color = SecondaryText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { viewModel.cancelReply() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply", tint = SecondaryText)
                            }
                        }
                    }
                }

                if (uiState.isBlockedByMe) {
                    // Blocked: replace the composer with a clear, reversible state.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, tint = RedAlert, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("You blocked ${uiState.otherUsername}", color = SecondaryText, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { viewModel.unblockOther() }) { Text("Unblock", color = OrangePrimary, fontWeight = FontWeight.SemiBold) }
                    }
                } else {
                    // One-tap answers when the other person spoke last and you haven't started typing.
                    val lastMsg = uiState.messages.lastOrNull()
                    val showQuickReplies = messageText.isEmpty() &&
                        uiState.replyingTo == null &&
                        lastMsg != null &&
                        lastMsg.senderId != myId &&
                        lastMsg.sendState.isEmpty() &&
                        !isDeletedMessage(lastMsg.content) &&
                        (minutesSince(lastMsg.createdAt) ?: Long.MAX_VALUE) < 12 * 60
                    AnimatedVisibility(visible = showQuickReplies) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickReplies.forEach { reply ->
                                Text(
                                    reply,
                                    color = OrangePrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(OrangePrimary.copy(alpha = 0.12f))
                                        .border(1.dp, OrangePrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                        .clickable {
                                            haptic.tap()
                                            viewModel.sendMessage(reply)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }

                    var inputFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(SurfaceAlt)
                                .border(1.dp, if (inputFocused) OrangePrimary.copy(alpha = 0.7f) else Divider, RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = messageText,
                                onValueChange = {
                                    if (it.length <= MAX_MESSAGE_LENGTH) {
                                        messageText = it
                                        viewModel.onInputChanged(it)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().onFocusChanged { inputFocused = it.isFocused },
                                textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp, lineHeight = 20.sp),
                                cursorBrush = SolidColor(OrangePrimary),
                                maxLines = 5,
                                decorationBox = { inner ->
                                    Box {
                                        if (messageText.isEmpty()) {
                                            Text("Type a message…", color = TertiaryText, fontSize = 15.sp)
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(sendBg)
                                .semantics { contentDescription = "Send message" }
                                .pressScale(pressedScale = 0.88f) {
                                    if (sendEnabled) {
                                        haptic.tap()
                                        viewModel.sendMessage(messageText)
                                        messageText = ""
                                        scope.launch { listState.animateScrollToItem(0) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, null,
                                tint = if (sendEnabled) Color.White else TertiaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(DensityManager.cardPadding.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(6) { i ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (i % 2 == 0) 0.65f else 0.55f)
                                .height(48.dp)
                                .align(if (i % 2 == 0) Alignment.End else Alignment.Start)
                                .shimmerEffect(RoundedCornerShape(18.dp))
                        )
                    }
                }
            }

            uiState.loadError != null && uiState.messages.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding)) {
                    EmptyState(
                        icon = Icons.Filled.CloudOff,
                        title = "Couldn't open this chat",
                        subtitle = "Check your connection and try again.",
                        ctaLabel = "Try again",
                        onCta = { viewModel.retryLoad() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            uiState.messages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IncognitoAvatar(seed = uiState.otherAvatarSeed, size = 76.dp, isOnline = uiState.isOtherOnline)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Say hi to ${uiState.otherUsername}", color = PrimaryText,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Start a private conversation. A friendly first line goes a long way.",
                            color = SecondaryText, fontSize = 14.sp, lineHeight = 20.sp,
                            maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                        )
                        if (!uiState.isBlockedByMe) {
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ConversationStarters.forEach { starter ->
                                    Text(
                                        starter,
                                        color = OrangePrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(OrangePrimary.copy(alpha = 0.12f))
                                            .clickable {
                                                haptic.tap()
                                                messageText = starter.removePrefix("👋 ").trim().ifEmpty { starter }
                                                viewModel.onInputChanged(messageText)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> Box(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    // Typing bubble sits at the very bottom (index 0 in a reversed list).
                    if (uiState.isOtherTyping) {
                        item(key = "typing_indicator") {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp).animateItem()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                                        .background(SurfaceAlt)
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) { TypingDots() }
                            }
                        }
                    }

                    itemsIndexed(reversedMessages, key = { _, msg -> msg.id }) { idx, msg ->
                        val isMe = msg.senderId == myId
                        val newer = reversedMessages.getOrNull(idx - 1)
                        val older = reversedMessages.getOrNull(idx + 1)

                        // Consecutive messages from the same person within 5 minutes form a group.
                        val joinsNewer = newer != null && newer.senderId == msg.senderId &&
                            sameDay(newer.createdAt, msg.createdAt) && withinMinutes(newer.createdAt, msg.createdAt, 5)
                        val joinsOlder = older != null && older.senderId == msg.senderId &&
                            sameDay(older.createdAt, msg.createdAt) && withinMinutes(older.createdAt, msg.createdAt, 5)
                        val isLastInGroup = !joinsNewer
                        val isFirstInGroup = !joinsOlder

                        // Date chip goes ABOVE the first message of each day.
                        val showDate = older == null || !sameDay(older.createdAt, msg.createdAt)
                        val showUnreadDivider = msg.id == uiState.firstUnreadId

                        Column(Modifier.fillMaxWidth().animateItem()) {
                            if (showDate) DateChip(formatMsgDate(msg.createdAt))
                            if (showUnreadDivider) UnreadDivider()
                            MessageRow(
                                msg = msg,
                                isMe = isMe,
                                myId = myId,
                                otherName = uiState.otherUsername,
                                repliedTo = msg.replyToId?.let { messagesById[it] },
                                isFirstInGroup = isFirstInGroup,
                                isLastInGroup = isLastInGroup,
                                onReply = { viewModel.startReply(msg) },
                                onReact = { emoji -> viewModel.toggleReaction(msg.id, emoji) },
                                onEdit = { if (isMe) { editingMessage = msg; editText = msg.content } },
                                onDeleteForMe = { viewModel.deleteMessageForMe(msg.id) },
                                onDeleteForBoth = { if (isMe) deletingMessage = msg },
                                onCopy = { text ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    scope.launch { snackbarHostState.showSnackbar("Message copied") }
                                },
                                onRetry = { viewModel.retrySend(msg.id) },
                                onDiscard = { viewModel.discardFailed(msg.id) },
                                onLinkClick = { url -> pendingLink = url }
                            )
                        }
                    }

                    if (uiState.isLoadingOlder) {
                        item(key = "loading_older") {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = OrangePrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                // Jump to latest, with a count of what arrived while reading history.
                AnimatedVisibility(
                    visible = !atBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 12.dp)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Surface)
                                .border(1.dp, Divider, CircleShape)
                                .semantics { contentDescription = "Scroll to latest message" }
                                .pressScale { scope.launch { listState.animateScrollToItem(0) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = PrimaryText)
                        }
                        if (newBelow > 0) {
                            Text(
                                if (newBelow > 9) "9+" else newBelow.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pieces ────────────────────────────────────────────────────────────────────

@Composable
private fun DateChip(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), Alignment.Center) {
        Text(
            text, color = TertiaryText, fontSize = 11.sp,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceAlt).padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UnreadDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = OrangePrimary.copy(alpha = 0.5f), thickness = 1.dp)
        Text(
            "New messages", color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        HorizontalDivider(Modifier.weight(1f), color = OrangePrimary.copy(alpha = 0.5f), thickness = 1.dp)
    }
}

/**
 * One message with: grouped corners/spacing, swipe-right to reply, double-tap to ❤️,
 * long-press menu (reactions + actions), reply quote, reactions, edited label and delivery ticks.
 */
@Composable
private fun MessageRow(
    msg: Message,
    isMe: Boolean,
    myId: String,
    otherName: String,
    repliedTo: Message?,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForBoth: () -> Unit,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var showMeta by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }
    val deleted = isDeletedMessage(msg.content)
    val sending = msg.sendState == STATE_SENDING
    val failed = msg.sendState == STATE_FAILED
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    val triggerPx = with(LocalDensity.current) { 56.dp.toPx() }

    val bubbleBg = if (isMe)
        Brush.linearGradient(listOf(OrangePrimary, OrangeLight))
    else
        Brush.linearGradient(listOf(SurfaceAlt, SurfaceAlt))
    val textColor = if (isMe) Color.White else PrimaryText

    // 1–3 emoji on their own are shown big and bubble-less.
    val emojiCount = remember(msg.content, deleted) { if (deleted) 0 else emojiOnlyCount(msg.content) }
    val jumbo = emojiCount in 1..3 && msg.replyToId == null

    val a11yLabel = remember(msg.id, msg.content, msg.createdAt, msg.isRead, msg.sendState, isMe, otherName) {
        val who = if (isMe) "You" else otherName
        val body = if (deleted) "Deleted message" else msg.content
        val status = when {
            sending -> ", sending"
            failed -> ", not sent"
            isMe && msg.isRead -> ", seen"
            else -> ""
        }
        "$who, ${formatMsgTime(msg.createdAt)}: $body$status"
    }

    // Tight corners where bubbles in a group join; the "tail" only on the last bubble.
    val inner = 6.dp
    val shape = RoundedCornerShape(
        topStart = if (!isMe && !isFirstInGroup) inner else 18.dp,
        topEnd = if (isMe && !isFirstInGroup) inner else 18.dp,
        bottomStart = if (!isMe) (if (isLastInGroup) 4.dp else inner) else 18.dp,
        bottomEnd = if (isMe) (if (isLastInGroup) 4.dp else inner) else 18.dp
    )

    Box(Modifier.fillMaxWidth().padding(top = if (isFirstInGroup) 8.dp else 2.dp)) {
        // Reply affordance revealed behind the bubble while swiping.
        Icon(
            Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(22.dp)
                .alpha((offsetX.value / triggerPx).coerceIn(0f, 1f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(msg.id, msg.sendState) {
                    if (msg.sendState.isNotEmpty() || deleted) return@pointerInput
                    var triggered = false
                    detectHorizontalDragGestures(
                        onDragStart = { triggered = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val target = (offsetX.value + dragAmount).coerceIn(0f, triggerPx * 1.4f)
                            scope.launch { offsetX.snapTo(target) }
                            if (!triggered && target >= triggerPx) {
                                triggered = true
                                haptic.tap()
                            }
                        },
                        onDragEnd = {
                            if (triggered) onReply()
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f) } }
                    )
                },
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .clip(shape)
                        .alpha(if (sending) 0.7f else 1f)
                        .then(if (jumbo) Modifier else Modifier.background(bubbleBg))
                        .pointerInput(msg.id, msg.sendState, deleted) {
                            detectTapGestures(
                                onTap = { if (failed) onRetry() else showMeta = !showMeta },
                                onDoubleTap = {
                                    if (msg.sendState.isEmpty() && !deleted) {
                                        haptic.like()
                                        onReact("❤️")
                                    }
                                },
                                onLongPress = {
                                    if (!sending) {
                                        haptic.tap()
                                        showMenu = true
                                    }
                                }
                            )
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = a11yLabel
                            customActions = buildList<CustomAccessibilityAction> {
                                if (msg.sendState.isEmpty() && !deleted) {
                                    add(CustomAccessibilityAction("Reply") { onReply(); true })
                                    add(CustomAccessibilityAction("React with heart") { onReact("❤️"); true })
                                }
                                if (failed) add(CustomAccessibilityAction("Retry sending") { onRetry(); true })
                            }
                        }
                        .padding(
                            if (jumbo) PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            else PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        )
                ) {
                    // Quoted message when this is a reply.
                    if (msg.replyToId != null && !deleted) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = if (isMe) 0.14f else 0.10f))
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(Modifier.width(3.dp).fillMaxHeight().background(if (isMe) Color.White.copy(alpha = 0.85f) else OrangePrimary))
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    if (repliedTo?.senderId == myId) "You" else otherName,
                                    color = if (isMe) Color.White else OrangePrimary,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
                                )
                                Text(
                                    when {
                                        repliedTo == null -> "Original message"
                                        isDeletedMessage(repliedTo.content) -> "Message deleted"
                                        else -> repliedTo.content
                                    },
                                    color = textColor.copy(alpha = 0.85f),
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (deleted) {
                        Text(
                            "This message was deleted",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    } else if (jumbo) {
                        Text(
                            msg.content,
                            fontSize = when (emojiCount) { 1 -> 46.sp; 2 -> 40.sp; else -> 34.sp }
                        )
                    } else {
                        MessageText(
                            text = msg.content,
                            color = textColor,
                            linkColor = if (isMe) Color.White else BlueInfo,
                            onLinkClick = onLinkClick
                        )
                    }
                }

                // Long-press menu: quick reactions on top, then actions.
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Surface)
                ) {
                    if (!failed && !deleted) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            QuickReactions.forEach { emoji ->
                                val mine = msg.reactions[emoji]?.contains(myId) == true
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (mine) OrangePrimary.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { showMenu = false; onReact(emoji) },
                                    contentAlignment = Alignment.Center
                                ) { Text(emoji, fontSize = 22.sp) }
                            }
                        }
                        HorizontalDivider(color = Divider.copy(alpha = 0.6f))
                        DropdownMenuItem(
                            text = { Text("Reply", color = PrimaryText) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null, tint = OrangePrimary) },
                            onClick = { showMenu = false; onReply() }
                        )
                    }
                    if (failed) {
                        DropdownMenuItem(
                            text = { Text("Retry", color = PrimaryText) },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null, tint = OrangePrimary) },
                            onClick = { showMenu = false; onRetry() }
                        )
                    }
                    if (!deleted) {
                        DropdownMenuItem(
                            text = { Text("Copy", color = PrimaryText) },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null, tint = SecondaryText) },
                            onClick = { showMenu = false; onCopy(msg.content) }
                        )
                    }
                    if (isMe && !failed && !deleted) {
                        DropdownMenuItem(
                            text = { Text("Edit", color = PrimaryText) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = SecondaryText) },
                            onClick = { showMenu = false; onEdit() }
                        )
                    }
                    if (failed) {
                        DropdownMenuItem(
                            text = { Text("Discard", color = RedAlert) },
                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = RedAlert) },
                            onClick = { showMenu = false; onDiscard() }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Delete for me", color = RedAlert) },
                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = RedAlert) },
                            onClick = { showMenu = false; onDeleteForMe() }
                        )
                        if (isMe && !deleted) {
                            DropdownMenuItem(
                                text = { Text("Delete for both", color = RedAlert) },
                                leadingIcon = { Icon(Icons.Filled.DeleteForever, null, tint = RedAlert) },
                                onClick = { showMenu = false; onDeleteForBoth() }
                            )
                        }
                    }
                }
            }

            // Reactions under the bubble. Tapping one toggles my own reaction.
            if (msg.reactions.isNotEmpty() && !deleted) {
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    msg.reactions.forEach { (emoji, users) ->
                        val mine = users.contains(myId)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (mine) OrangePrimary.copy(alpha = 0.2f) else SurfaceAlt)
                                .border(1.dp, if (mine) OrangePrimary.copy(alpha = 0.6f) else Divider, RoundedCornerShape(12.dp))
                                .clickable { haptic.like(); onReact(emoji) }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(emoji, fontSize = 13.sp)
                            if (users.size > 1) {
                                Spacer(Modifier.width(3.dp))
                                Text(users.size.toString(), color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Footer: time + edited + delivery state. Shown at the end of a group, on tap, or while pending/failed.
            if (isLastInGroup || showMeta || sending || failed) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp).padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (failed) {
                        Text("Not sent · tap to retry", color = RedAlert, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        if (msg.editedAt != null && !deleted) {
                            Text("edited · ", color = TertiaryText, fontSize = 10.sp)
                        }
                        Text(formatMsgTime(msg.createdAt), color = TertiaryText, fontSize = 10.sp)
                        if (isMe) {
                            Spacer(Modifier.width(4.dp))
                            when {
                                sending -> Icon(Icons.Filled.Schedule, "Sending", tint = TertiaryText, modifier = Modifier.size(12.dp))
                                msg.isRead -> Icon(Icons.Filled.DoneAll, "Seen", tint = BlueInfo, modifier = Modifier.size(14.dp))
                                else -> Icon(Icons.Filled.Done, "Sent", tint = TertiaryText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Message body with tappable links (opened only after the user confirms in a safety dialog). */
@Composable
private fun MessageText(
    text: String,
    color: Color,
    linkColor: Color,
    onLinkClick: (String) -> Unit
) {
    val links = remember(text) { findLinks(text) }
    if (links.isEmpty()) {
        Text(text, color = color, fontSize = 15.sp, lineHeight = 21.sp)
        return
    }
    val annotated = remember(text, linkColor, onLinkClick) {
        buildAnnotatedString {
            var cursor = 0
            links.forEach { link ->
                append(text.substring(cursor, link.start))
                withLink(
                    LinkAnnotation.Clickable(
                        tag = link.url,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                        ),
                        linkInteractionListener = { onLinkClick(link.url) }
                    )
                ) {
                    append(text.substring(link.start, link.end))
                }
                cursor = link.end
            }
            append(text.substring(cursor))
        }
    }
    Text(annotated, color = color, fontSize = 15.sp, lineHeight = 21.sp)
}
