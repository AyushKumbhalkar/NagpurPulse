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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.model.Message
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nagpurpulse.ui.preferences.DensityManager
// ── ViewModel ─────────────────────────────────────────────────────────────────
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val otherUsername: String = "Chat",
    val otherAvatarSeed: String = "anon",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val conversationId: String = savedStateHandle["conversationId"] ?: ""
    val myUserId get() = authRepository.currentUserId ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init { loadMessages(); subscribeRealtime() }

    private fun loadMessages() {
        viewModelScope.launch {
            messageRepository.getMessages(conversationId).fold(
                onSuccess = { msgs ->
                    val conv = messageRepository.getConversations().getOrNull()
                        ?.firstOrNull { it.id == conversationId }
                    _uiState.value = ChatUiState(
                        messages       = msgs,
                        otherUsername  = conv?.otherUsername ?: "Chat",
                        otherAvatarSeed = conv?.otherAvatarSeed ?: "anon",
                        isLoading      = false
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
            // Optimistic insert
            val optimistic = Message(
                id             = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId       = myUserId,
                content        = content,
                createdAt      = java.time.Instant.now().toString()
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + optimistic
            )
            // Real send
            messageRepository.sendMessage(conversationId, content).fold(
                onSuccess = { real ->
                    // Realtime may deliver the server message before this request
                    // returns. Remove the optimistic row and deduplicate by server ID.
                    val updated = _uiState.value.messages
                        .filterNot { it.id == optimistic.id }
                        .toMutableList()
                    if (updated.none { it.id == real.id }) updated.add(real)
                    _uiState.value = _uiState.value.copy(
                        messages = updated,
                        isSending = false
                    )
                },
                onFailure = { e ->
                    // Remove optimistic on failure
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.filter { it.id != optimistic.id },
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

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages.filter { it.id != messageId }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
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

                Column(Modifier.weight(1f)) {
                    Text(uiState.otherUsername, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(GreenSuccess))
                        Spacer(Modifier.width(4.dp))
                        Text("Private conversation", color = SecondaryText, fontSize = 11.sp)
                    }
                }

                Box {
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "Chat options", tint = SecondaryText)
                    }
                    DropdownMenu(
                        expanded = showChatMenu,
                        onDismissRequest = { showChatMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Refresh messages") },
                            leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                            onClick = {
                                showChatMenu = false
                                viewModel.refreshMessages()
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
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Send a message to start a private conversation.",
                        color = SecondaryText, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        } else LazyColumn(
            state          = listState,
            reverseLayout = true,
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    onDelete = { if (isMe) viewModel.deleteMessage(msg.id) },
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
    onDelete: () -> Unit,
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
    LaunchedEffect(Unit) { delay(30); visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(200)) + slideInHorizontally(
            initialOffsetX = { if (isMe) it / 3 else -it / 3 },
            animationSpec  = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
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
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Options dropdown (long-press)
            DropdownMenu(
                expanded          = showOptions,
                onDismissRequest  = { showOptions = false },
                modifier          = Modifier.background(SurfaceAlt)
            ) {
                if (isMe) {
                    DropdownMenuItem(
                        text    = { Text("Delete message", color = RedAlert) },
                        onClick = { showOptions = false; onDelete() }
                    )
                }
                DropdownMenuItem(
                    text    = { Text("Copy", color = PrimaryText) },
                    onClick = { showOptions = false; onCopy(msg.content) }
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
