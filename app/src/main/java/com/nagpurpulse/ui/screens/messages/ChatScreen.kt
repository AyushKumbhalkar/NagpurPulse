// This is the ChatScreen.kt file

// java/com/nagpurpulse/ui/screens/messages/ChatScreen.kt

package com.nagpurpulse.ui.screens.messages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.model.Message
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nagpurpulse.ui.preferences.DensityManager
// ── ViewModel ─────────────────────────────────────────────────────────────────
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val otherUsername: String = "Chat",
    val otherAvatarSeed: String = "anon",
    val otherUserId: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val isOtherOnline: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val presenceRepository: PresenceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val conversationId: String = savedStateHandle["conversationId"] ?: ""
    val myUserId get() = authRepository.currentUserId ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        presenceRepository.start()
        viewModelScope.launch {
            presenceRepository.onlineUserIds.collect { ids ->
                _uiState.value = _uiState.value.copy(
                    isOtherOnline = _uiState.value.otherUserId in ids
                )
            }
        }
        loadMessages()
        subscribeRealtime()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            val messagesTask = async { messageRepository.getMessages(conversationId) }
            val conversationTask = async { messageRepository.getConversationById(conversationId) }
            val messagesResult = messagesTask.await()
            val conversationResult = conversationTask.await()
            messagesResult.fold(
                onSuccess = { msgs ->
                    val conv = conversationResult.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        messages = msgs,
                        otherUsername = conv?.otherUsername ?: "Chat",
                        otherAvatarSeed = conv?.otherAvatarSeed ?: "anon",
                        otherUserId = conv?.otherUserId ?: "",
                        isOtherOnline = (conv?.otherUserId ?: "") in presenceRepository.onlineUserIds.value,
                        isLoading = false
                    )
                    messageRepository.markConversationRead(conversationId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    private fun subscribeRealtime() {
        viewModelScope.launch {

            android.util.Log.d("CHAT_RT", "Realtime started")

            messageRepository.subscribeToMessages(conversationId)
                .collect { newMsg ->

                    android.util.Log.d(
                        "CHAT_RT",
                        "Realtime received: ${newMsg.content}"
                    )

                    val current = _uiState.value.messages.toMutableList()

                    if (current.none { it.id == newMsg.id }) {
                        current.add(newMsg)

                        _uiState.value = _uiState.value.copy(
                            messages = current
                        )
                    }

                    if (newMsg.senderId != myUserId) {
                        messageRepository.markConversationRead(conversationId)
                    }
                }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)
            // Use the server-confirmed message as the single source of truth.
            // Optimistic + Realtime + request-response inserts made the same
            // message appear to animate twice on slower connections.
            messageRepository.sendMessage(conversationId, content).fold(
                onSuccess = { real ->
                    val updated = _uiState.value.messages.toMutableList()
                    val existingIndex = updated.indexOfFirst { it.id == real.id }
                    if (existingIndex >= 0) updated[existingIndex] = real
                    else updated.add(real)
                    _uiState.value = _uiState.value.copy(
                        messages = updated,
                        isSending = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(conversationId).fold(
                onSuccess = { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        error = null,
                        isLoading = false
                    )
                    messageRepository.markConversationRead(conversationId)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            )
        }
    }

    fun deleteMessageForMe(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForMe(messageId).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(messages = _uiState.value.messages.filterNot { it.id == messageId }) },
                onFailure = { err -> _uiState.value = _uiState.value.copy(error = err.message) }
            )
        }
    }

    fun deleteMessageForBoth(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForBoth(messageId).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(messages = _uiState.value.messages.map { if (it.id == messageId) it.copy(content = "This message was deleted") else it }) },
                onFailure = { err -> _uiState.value = _uiState.value.copy(error = err.message) }
            )
        }
    }

    fun editMessage(messageId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            messageRepository.editMessage(messageId, content.trim()).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(messages = _uiState.value.messages.map { if (it.id == messageId) it.copy(content = content.trim()) else it }) },
                onFailure = { err -> _uiState.value = _uiState.value.copy(error = err.message) }
            )
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onOtherProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState      = viewModel.uiState.collectAsState().value
    var messageText  by remember { mutableStateOf("") }
    val listState    = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()
    LaunchedEffect(uiState.error) {
        uiState.error?.takeIf { it.isNotBlank() }?.let { snackbarHostState.showSnackbar(it) }
    }
    var showChatMenu by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }
    var deletingMessage by remember { mutableStateOf<Message?>(null) }
    val reversedMessages = remember(uiState.messages) { uiState.messages.asReversed() }
    LaunchedEffect(uiState.messages.size) {

        if (uiState.messages.isNotEmpty()) {

            listState.animateScrollToItem(0)
        }
    }

    val myId         = viewModel.myUserId
    val avatarColor  = incognitoColor(uiState.otherAvatarSeed)
    val avatarEmoji  = incognitoEmoji(uiState.otherAvatarSeed)

    // Auto-scroll to bottom on new messages


    val sendEnabled  = messageText.isNotBlank()
    val sendBg by animateColorAsState(
        if (sendEnabled) OrangePrimary else SurfaceAlt,
        tween(200),
        label = "send_bg"
    )

    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Edit message") },
            text = { OutlinedTextField(value = editText, onValueChange = { editText = it }, modifier = Modifier.fillMaxWidth(), maxLines = 5) },
            confirmButton = { TextButton(onClick = { editingMessage?.let { viewModel.editMessage(it.id, editText) }; editingMessage = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Cancel") } }
        )
    }
    if (deletingMessage != null) {
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Delete message for both?") },
            text = { Text("This replaces the message content for both participants.") },
            confirmButton = { TextButton(onClick = { deletingMessage?.let { viewModel.deleteMessageForBoth(it.id) }; deletingMessage = null }) { Text("Delete for both", color = RedAlert) } },
            dismissButton = { TextButton(onClick = { deletingMessage = null }) { Text("Cancel") } }
        )
    }
    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Surface, Background),
                            0f,
                            120f
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryText)
                }

                // Avatar
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(avatarColor.copy(0.5f), avatarColor.copy(0.2f))))
                        .border(1.5.dp, avatarColor.copy(0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(avatarEmoji, fontSize = 18.sp) }

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
                    Text(
                        text = if (uiState.isOtherOnline) "Online" else "View profile",
                        color = if (uiState.isOtherOnline) Color(0xFF22C55E) else SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Chat options", tint = SecondaryText)
                    }
                    DropdownMenu(
                        expanded = showChatMenu,
                        onDismissRequest = { showChatMenu = false },
                        containerColor = Color(0xFF171318),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 8.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("View profile") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            enabled = uiState.otherUserId.isNotBlank(),
                            onClick = {
                                showChatMenu = false
                                uiState.otherUserId.takeIf { it.isNotBlank() }?.let(onOtherProfileClick)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy conversation ID") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                            onClick = {
                                showChatMenu = false
                                clipboardManager.setText(AnnotatedString(conversationId))
                                screenScope.launch { snackbarHostState.showSnackbar("Conversation ID copied") }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = messageText,
                    onValueChange = { messageText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Type a message…", color = TertiaryText, fontSize = 14.sp) },
                    shape         = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = OrangePrimary,
                        unfocusedBorderColor    = Divider,
                        cursorColor             = OrangePrimary,

                        focusedTextColor        = PrimaryText,
                        unfocusedTextColor      = PrimaryText,

                        focusedContainerColor   = SurfaceAlt,
                        unfocusedContainerColor = SurfaceAlt,

                        focusedLabelColor       = OrangePrimary,
                        unfocusedLabelColor     = SecondaryText
                    ),
                    singleLine = false,
                    maxLines   = 4
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(sendBg)
                        .pressScale {
                            if (sendEnabled && !uiState.isSending) {

                                viewModel.sendMessage(messageText)
                                messageText = ""

                                screenScope.launch {
                                    listState.animateScrollToItem(0)
                                }


                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, null,
                            tint = if (sendEnabled) Color.White else TertiaryText,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(Modifier.fillMaxSize().padding(padding).padding(
    DensityManager.cardPadding.dp
), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            return@Scaffold
        }

        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(CircleShape)
                            .background(OrangePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Forum, contentDescription = null,
                            tint = OrangePrimary, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Your conversation starts here", color = PrimaryText,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text("Send a message to start a private conversation.",
                        color = SecondaryText, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else LazyColumn(
            state          = listState,
            reverseLayout = true,
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(reversedMessages, key = { _, msg -> msg.id }) { idx, msg ->
                val isMe = msg.senderId == myId
                // Show date separator when day changes
                val showDate = idx == 0 ||
                    !sameDay(reversedMessages[idx - 1].createdAt, msg.createdAt)
                if (showDate) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), Alignment.Center) {
                        Text(formatMsgDate(msg.createdAt), color = TertiaryText, fontSize = 11.sp,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceAlt).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                MessageBubble(
                    msg      = msg,
                    isMe     = isMe,
                    onEdit = { if (isMe) { editingMessage = msg; editText = msg.content } },
                    onDeleteForMe = { viewModel.deleteMessageForMe(msg.id) },
                    onDeleteForBoth = { if (isMe) deletingMessage = msg },
                    onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                        screenScope.launch { snackbarHostState.showSnackbar("Message copied") }
                    }
                )
            }
        }
    }
}

// ── Message bubble ────────────────────────────────────────────────────────────
@Composable
private fun MessageBubble(
    msg: Message,
    isMe: Boolean,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForBoth: () -> Unit,
    onCopy: (String) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    val bubbleBg = if (isMe)
        Brush.linearGradient(listOf(OrangePrimary, OrangeLight))
    else
        Brush.linearGradient(listOf(SurfaceAlt, SurfaceAlt))
    val textColor = if (isMe) Color.White else PrimaryText

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(120)) + androidx.compose.animation.scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(120, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart    = 18.dp,
                            topEnd      = 18.dp,
                            bottomStart = if (isMe) 18.dp else 4.dp,
                            bottomEnd   = if (isMe) 4.dp else 18.dp
                        )
                    )
                    .background(bubbleBg)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showOptions = true }
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(msg.content, color = textColor, fontSize = 15.sp, lineHeight = 21.sp)
            }

            // Timestamp
            Text(
                formatMsgTime(msg.createdAt),
                color    = TertiaryText,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
            )

            // Keep message actions in a predictable centered popup.
            if (showOptions) {
                AlertDialog(
                    onDismissRequest = { showOptions = false },
                    containerColor = Color(0xFF171318),
                    titleContentColor = Color(0xFFF7F3F5),
                    textContentColor = Color(0xFFC7C0CA),
                    shape = RoundedCornerShape(28.dp),
                    title = { Text("Message options") },
                    text = {
                        Column {
                            if (isMe) {
                                TextButton(onClick = { showOptions = false; onEdit() }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(12.dp)); Text("Edit message")
                                }
                            }
                            TextButton(onClick = { showOptions = false; onDeleteForMe() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.DeleteOutline, null, tint = RedAlert); Spacer(Modifier.width(12.dp)); Text("Delete for me", color = RedAlert)
                            }
                            if (isMe) {
                                TextButton(onClick = { showOptions = false; onDeleteForBoth() }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Filled.DeleteForever, null, tint = RedAlert); Spacer(Modifier.width(12.dp)); Text("Delete for both", color = RedAlert)
                                }
                            }
                            TextButton(onClick = { showOptions = false; onCopy(msg.content) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(12.dp)); Text("Copy")
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showOptions = false }) { Text("Close") } }
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatMsgTime(ts: String): String = try {
    val dt = java.time.ZonedDateTime.ofInstant(java.time.Instant.parse(ts), java.time.ZoneId.systemDefault())
    "${dt.hour.toString().padStart(2,'0')}:${dt.minute.toString().padStart(2,'0')}"
} catch (_: Exception) { "" }

private fun formatMsgDate(ts: String): String = try {
    val dt   = java.time.ZonedDateTime.ofInstant(java.time.Instant.parse(ts), java.time.ZoneId.systemDefault())
    val now  = java.time.ZonedDateTime.now()
    when {
        dt.toLocalDate() == now.toLocalDate()             -> "Today"
        dt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
        else -> "${dt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}, ${dt.dayOfMonth} ${dt.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}"
    }
} catch (_: Exception) { "" }

private fun sameDay(ts1: String, ts2: String): Boolean = try {
    val d1 = java.time.Instant.parse(ts1).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val d2 = java.time.Instant.parse(ts2).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    d1 == d2
} catch (_: Exception) { false }
