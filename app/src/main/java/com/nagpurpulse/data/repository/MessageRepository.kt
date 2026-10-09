// This is the MessageRepository.kt file

// java/com/nagpurpulse/data/repository/MessageRepository.kt

package com.nagpurpulse.data.repository

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import com.nagpurpulse.data.model.Conversation
import com.nagpurpulse.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class HiddenMessageRow(val message_id: String = "")

@Serializable
private data class HiddenConversationRow(val conversation_id: String = "")

@Serializable
private data class BlockRow(@SerialName("blocked_id") val blockedId: String = "")

@Serializable
private data class ConversationPrefRow(
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("is_pinned") val isPinned: Boolean = false
)

/** One page of a conversation's messages, oldest first. */
data class MessagesPage(
    val messages: List<Message>,
    /** How many rows the server returned (before per-user hidden filtering); used as the next offset. */
    val serverCount: Int,
    val hasMore: Boolean
)

/** Ephemeral "is typing" payload exchanged over a per-conversation presence channel. */
@Serializable
data class TypingPayload(
    val userId: String = "",
    val typing: Boolean = false
)

/**
 * Handle for the per-conversation typing channel. Uses realtime *presence*, so nothing is
 * written to the database. Always call [close] when leaving the chat.
 */
class TypingSession internal constructor(
    private val client: SupabaseClient,
    private val channel: RealtimeChannel,
    private val myId: String,
    val othersTyping: StateFlow<Set<String>>
) {
    suspend fun setTyping(typing: Boolean) {
        try {
            channel.track(
                buildJsonObject {
                    put("userId", myId)
                    put("typing", typing)
                }
            )
        } catch (_: Exception) {
            // Typing is best-effort; never surface failures to the user.
        }
    }

    suspend fun close() {
        try {
            channel.untrack()
            client.realtime.removeChannel(channel)
        } catch (_: Exception) {
        }
    }
}

@Singleton
class MessageRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    // ── Get or create a conversation between two users ────────────────────────
    suspend fun getOrCreateConversation(otherUserId: String): Result<Conversation> {
        val myId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            // Check both orderings and skip conversations hidden by this user.
            // A hidden conversation must not be reused for this user.
            val hiddenIds = client.postgrest["conversation_hidden_for_users"].select {
                filter { eq("user_id", myId) }
            }.decodeList<HiddenConversationRow>().map { it.conversation_id }.toSet()
            val visibleExisting = client.postgrest["conversations"].select {
                filter {
                    or {
                        and {
                            eq("participant_one", myId)
                            eq("participant_two", otherUserId)
                        }
                        and {
                            eq("participant_one", otherUserId)
                            eq("participant_two", myId)
                        }
                    }
                }
            }.decodeList<Conversation>().firstOrNull { it.id !in hiddenIds }

            if (visibleExisting != null) {
                return Result.success(enrichConversation(visibleExisting, myId))
            }

            // Respect the recipient's direct-message preference before creating a
            // new conversation. Database-side enforcement is still needed because
            // clients can bypass this repository check.
            val recipientProfile = profileRepository.getProfile(otherUserId).getOrElse {
                return Result.failure(it)
            }
            if (!recipientProfile.allowDms) {
                return Result.failure(IllegalStateException("This user isn't accepting new direct messages."))
            }

            // Create new
            val created = client.postgrest["conversations"].insert(
                buildJsonObject {
                    put("participant_one", myId)
                    put("participant_two", otherUserId)
                    put("last_message", "")
                    put("last_message_at", java.time.Instant.now().toString())
                    put("unread_count_one", 0)
                    put("unread_count_two", 0)
                }
            ) { select() }.decodeSingle<Conversation>()

            Result.success(enrichConversation(created, myId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Fetch all conversations for current user ──────────────────────────────
    suspend fun getConversations(): Result<List<Conversation>> {
        val myId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            val rows = client.postgrest["conversations"].select {
                filter {
                    or {
                        eq("participant_one", myId)
                        eq("participant_two", myId)
                    }
                }
                order("last_message_at", Order.DESCENDING)
            }.decodeList<Conversation>()
            val hiddenIds = client.postgrest["conversation_hidden_for_users"].select {
                filter { eq("user_id", myId) }
            }.decodeList<HiddenConversationRow>().map { it.conversation_id }.toSet()
            val visibleRows = rows.filterNot { it.id in hiddenIds || it.lastMessage.isNullOrBlank() }
            Result.success(enrichConversations(visibleRows, myId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversationById(conversationId: String): Result<Conversation?> {
        val myId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            val row = client.postgrest["conversations"].select {
                filter { eq("id", conversationId) }
                limit(1)
            }.decodeList<Conversation>().firstOrNull()
            Result.success(row?.let { enrichConversation(it, myId, withPrefs = true) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Fetch messages in a conversation ──────────────────────────────────────
    suspend fun getMessages(conversationId: String): Result<List<Message>> {
        return try {
            val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
            val hiddenIds = client.postgrest["message_hidden_for_users"].select {
                filter { eq("user_id", myId) }
            }.decodeList<HiddenMessageRow>().map { it.message_id }.toSet()
            val msgs = client.postgrest["messages"].select {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<Message>().filterNot { it.id in hiddenIds }
            Result.success(msgs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Newest-first paging: [offset] = how many server rows were already loaded. The returned
     * messages are in ascending (oldest first) order, ready to prepend to the visible list.
     */
    suspend fun getMessagesPage(
        conversationId: String,
        offset: Int,
        limit: Int = 30
    ): Result<MessagesPage> {
        val myId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            val hiddenIds = client.postgrest["message_hidden_for_users"].select {
                filter { eq("user_id", myId) }
            }.decodeList<HiddenMessageRow>().map { it.message_id }.toSet()

            val rows = client.postgrest["messages"].select {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<Message>()

            Result.success(
                MessagesPage(
                    messages = rows.filterNot { it.id in hiddenIds }.reversed(),
                    serverCount = rows.size,
                    hasMore = rows.size >= limit
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Send a message ────────────────────────────────────────────────────────
    /**
     * Sends through the atomic send_message() RPC (one round-trip, race-free unread counts,
     * block enforcement). Falls back to the legacy three-step path if the migration has not
     * been applied yet, so the app keeps working during rollout.
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        replyToId: String? = null
    ): Result<Message> {
        authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Message is empty"))

        return try {
            val msg = client.postgrest.rpc(
                "send_message",
                buildJsonObject {
                    put("p_conversation_id", conversationId)
                    put("p_content", trimmed)
                    if (replyToId != null) put("p_reply_to", replyToId)
                }
            ).decodeAs<Message>()
            Result.success(enrichMessage(msg))
        } catch (e: Exception) {
            when {
                isMissingRpc(e) -> sendMessageLegacy(conversationId, trimmed)
                else -> Result.failure(friendlySendError(e))
            }
        }
    }

    private fun isMissingRpc(e: Exception): Boolean {
        val m = e.message.orEmpty()
        return m.contains("PGRST202") ||
            m.contains("Could not find the function", ignoreCase = true) ||
            m.contains("schema cache", ignoreCase = true)
    }

    private fun friendlySendError(e: Exception): Exception {
        val m = e.message.orEmpty()
        return when {
            m.contains("message_blocked") ->
                IllegalStateException("You can't message this person.")
            m.contains("message_too_long") ->
                IllegalStateException("That message is too long.")
            else -> e
        }
    }

    /** Pre-migration path: three round-trips. Only used when send_message() isn't deployed yet. */
    private suspend fun sendMessageLegacy(conversationId: String, content: String): Result<Message> {
        val myId = authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in"))
        return try {
            val msg = client.postgrest["messages"].insert(
                buildJsonObject {
                    put("conversation_id", conversationId)
                    put("sender_id",       myId)
                    put("content",         content)
                    put("message_type",    "text")
                    put("is_read",         false)
                }
            ) { select() }.decodeSingle<Message>()

            // Update conversation last_message + increment other person's unread
            val conv = client.postgrest["conversations"].select {
                filter { eq("id", conversationId) }
            }.decodeSingle<Conversation>()

            val isParticipantOne = conv.participantOne == myId
            client.postgrest["conversations"].update(
                buildJsonObject {
                    put("last_message",    content)
                    put("last_message_at", java.time.Instant.now().toString())
                    // Increment unread for the OTHER person
                    if (isParticipantOne)
                        put("unread_count_two", conv.unreadCountTwo + 1)
                    else
                        put("unread_count_one", conv.unreadCountOne + 1)
                }
            ) { filter { eq("id", conversationId) } }
            // Notification rows are created by the database message trigger.
            Result.success(enrichMessage(msg))
        } catch (e: Exception) {
            Result.failure(friendlySendError(e))
        }
    }

    // ── Mark conversation as read ─────────────────────────────────────────────
    suspend fun markConversationRead(conversationId: String) {
        val myId = authRepository.currentUserId ?: return
        try {
            val conv = client.postgrest["conversations"].select {
                filter { eq("id", conversationId) }
            }.decodeSingle<Conversation>()

            val isOne = conv.participantOne == myId
            client.postgrest["conversations"].update(
                buildJsonObject {
                    if (isOne) put("unread_count_one", 0)
                    else       put("unread_count_two", 0)
                }
            ) { filter { eq("id", conversationId) } }

            // Mark individual messages read
            client.postgrest["messages"].update(
                buildJsonObject { put("is_read", true) }
            ) {
                filter {
                    eq("conversation_id", conversationId)
                    neq("sender_id", myId)
                    eq("is_read", false)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteConversationForMe(conversationId: String): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["conversation_hidden_for_users"].upsert(buildJsonObject { put("conversation_id", conversationId); put("user_id", myId) })
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteConversationForBoth(conversationId: String): Result<Unit> = try {
        client.postgrest.rpc(
            "delete_conversation_for_both",
            buildJsonObject { put("p_conversation_id", conversationId) }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteMessageForMe(messageId: String): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["message_hidden_for_users"].upsert(buildJsonObject { put("message_id", messageId); put("user_id", myId) })
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteMessageForBoth(messageId: String): Result<Unit> = try {
        client.postgrest.rpc(
            "delete_message_for_both",
            buildJsonObject { put("p_message_id", messageId) }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun editMessage(messageId: String, content: String): Result<Unit> = try {
        try {
            client.postgrest["messages"].update(
                buildJsonObject {
                    put("content", content)
                    put("edited_at", java.time.Instant.now().toString())
                }
            ) { filter { eq("id", messageId) } }
        } catch (_: Exception) {
            // Pre-migration databases have no edited_at column; retry with content only.
            client.postgrest["messages"].update(
                buildJsonObject { put("content", content) }
            ) { filter { eq("id", messageId) } }
        }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Reactions ─────────────────────────────────────────────────────────────
    /** Toggles [emoji] on a message (one reaction per user). Realtime updates both people. */
    suspend fun toggleReaction(messageId: String, emoji: String): Result<Unit> = try {
        client.postgrest.rpc(
            "toggle_message_reaction",
            buildJsonObject {
                put("p_message_id", messageId)
                put("p_emoji", emoji)
            }
        )
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Undo for "delete for me" ──────────────────────────────────────────────
    suspend fun restoreConversationForMe(conversationId: String): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["conversation_hidden_for_users"].delete {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", myId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Blocking & reporting ──────────────────────────────────────────────────
    suspend fun getBlockedUserIds(): Set<String> {
        val myId = authRepository.currentUserId ?: return emptySet()
        return try {
            client.postgrest["user_blocks"].select {
                filter { eq("blocker_id", myId) }
            }.decodeList<BlockRow>().map { it.blockedId }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun blockUser(otherUserId: String): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["user_blocks"].upsert(
                buildJsonObject {
                    put("blocker_id", myId)
                    put("blocked_id", otherUserId)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun unblockUser(otherUserId: String): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["user_blocks"].delete {
                filter {
                    eq("blocker_id", myId)
                    eq("blocked_id", otherUserId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun reportUser(
        conversationId: String,
        reportedUserId: String,
        reason: String,
        messageId: String? = null
    ): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["message_reports"].insert(
                buildJsonObject {
                    put("reporter_id", myId)
                    put("reported_user_id", reportedUserId)
                    put("conversation_id", conversationId)
                    if (messageId != null) put("message_id", messageId)
                    put("reason", reason)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Mute / pin ────────────────────────────────────────────────────────────
    suspend fun setConversationPrefs(
        conversationId: String,
        muted: Boolean,
        pinned: Boolean
    ): Result<Unit> {
        val myId = authRepository.currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            client.postgrest["conversation_prefs"].upsert(
                buildJsonObject {
                    put("conversation_id", conversationId)
                    put("user_id", myId)
                    put("is_muted", muted)
                    put("is_pinned", pinned)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Typing indicator (presence-based, nothing is stored) ──────────────────
    suspend fun openTypingSession(
        scope: CoroutineScope,
        conversationId: String
    ): TypingSession? {
        val myId = authRepository.currentUserId ?: return null
        return try {
            val channel = client.channel("typing_$conversationId")
            val others = MutableStateFlow<Set<String>>(emptySet())
            val presence = channel.presenceDataFlow<TypingPayload>()
            scope.launch {
                presence.collect { payloads ->
                    others.value = payloads
                        .filter { it.typing && it.userId.isNotBlank() && it.userId != myId }
                        .map { it.userId }
                        .toSet()
                }
            }
            channel.subscribe(blockUntilSubscribed = true)
            TypingSession(client, channel, myId, others)
        } catch (_: Exception) {
            null
        }
    }
    // ── Realtime: subscribe to new messages in a conversation ─────────────────
    fun subscribeToMessages(conversationId: String): Flow<Message> = kotlinx.coroutines.flow.flow {

        val channel = client.realtime.channel("messages_$conversationId")

        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(
            schema = "public"
        ) {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }

        val updates = channel.postgresChangeFlow<PostgresAction.Update>(
            schema = "public"
        ) {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }

        channel.subscribe(
            blockUntilSubscribed = true
        )

        merge(inserts, updates).collect { action ->

            val msg = action.decodeRecord<Message>()

            emit(enrichMessage(msg))
        }
    }

    // ── Realtime: subscribe to conversation list updates ──────────────────────
    fun subscribeToConversations(): Flow<Conversation> = flow {
        val myId = authRepository.currentUserId ?: return@flow
        val channel = client.realtime.channel("conversations_$myId")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "conversations"
        }.map { it.decodeRecord<Conversation>() }
        val updates = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "conversations"
        }.map { it.decodeRecord<Conversation>() }
        val deletes = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "conversations"
        }.map { action ->
            // Internal marker lets the inbox remove a deleted conversation immediately.
            Json.decodeFromJsonElement<Conversation>(action.oldRecord).copy(lastMessage = "__NAGPURPULSE_CONVERSATION_DELETED__")
        }

        // Register all event flows before subscribing, including deletions, so
        // both participants' inboxes update without requiring a manual refresh.
        channel.subscribe(blockUntilSubscribed = true)
        merge(inserts, updates, deletes).collect { conv ->
            if (conv.lastMessage == "__NAGPURPULSE_CONVERSATION_DELETED__") {
                emit(conv)
            } else {
                emit(enrichConversation(conv, myId))
            }
        }
    }

    // ── Search users by username ──────────────────────────────────────────────
    suspend fun searchUsers(query: String): Result<List<com.nagpurpulse.data.model.Profile>> {
        if (query.length < 2) return Result.success(emptyList())
        val myId = authRepository.currentUserId
        return try {
            val profiles = client.postgrest["profiles"].select {
                filter { ilike("username", "%$query%") }
                limit(20)
            }.decodeList<com.nagpurpulse.data.model.Profile>()
            // Exclude self from results
            Result.success(profiles.filter { it.id != myId })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /**
     * Enrich an inbox page with one profile query instead of one query per conversation.
     * This keeps inbox loading from scaling to N+1 network requests as chats grow.
     */
    private suspend fun enrichConversations(rows: List<Conversation>, myId: String): List<Conversation> {
        if (rows.isEmpty()) return emptyList()

        val otherIds = rows.map { conv ->
            if (conv.participantOne == myId) conv.participantTwo else conv.participantOne
        }.distinct()
        val profilesById = try {
            client.postgrest["profiles"].select {
                filter { isIn("id", otherIds) }
            }.decodeList<com.nagpurpulse.data.model.Profile>().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        // Per-user mute / pin. Missing table (migration not applied yet) just means "no prefs".
        val prefsById = try {
            client.postgrest["conversation_prefs"].select {
                filter {
                    eq("user_id", myId)
                    isIn("conversation_id", rows.map { it.id })
                }
            }.decodeList<ConversationPrefRow>().associateBy { it.conversationId }
        } catch (_: Exception) {
            emptyMap()
        }

        return rows.map { conv ->
            val otherId = if (conv.participantOne == myId) conv.participantTwo else conv.participantOne
            val unread = if (conv.participantOne == myId) conv.unreadCountOne else conv.unreadCountTwo
            val profile = profilesById[otherId]
            val prefs = prefsById[conv.id]
            conv.copy(
                otherUsername = profile?.username,
                otherAvatarSeed = profile?.username ?: otherId,
                otherUserId = otherId,
                myUnreadCount = unread,
                isPinned = prefs?.isPinned ?: false,
                isMuted = prefs?.isMuted ?: false
            )
        }
    }

    private suspend fun enrichConversation(
        conv: Conversation,
        myId: String,
        withPrefs: Boolean = false
    ): Conversation {
        val otherId = if (conv.participantOne == myId) conv.participantTwo else conv.participantOne
        val unread  = if (conv.participantOne == myId) conv.unreadCountOne else conv.unreadCountTwo
        return try {
            val profile = client.postgrest["profiles"].select {
                filter { eq("id", otherId) }
            }.decodeList<com.nagpurpulse.data.model.Profile>().firstOrNull()
            val prefs = if (withPrefs) {
                try {
                    client.postgrest["conversation_prefs"].select {
                        filter {
                            eq("user_id", myId)
                            eq("conversation_id", conv.id)
                        }
                    }.decodeList<ConversationPrefRow>().firstOrNull()
                } catch (_: Exception) {
                    null
                }
            } else null
            conv.copy(
                otherUsername = profile?.username,
                otherAvatarSeed = profile?.username ?: otherId,
                otherUserId   = otherId,
                myUnreadCount = unread,
                isPinned = prefs?.isPinned ?: false,
                isMuted = prefs?.isMuted ?: false
            )
        } catch (_: Exception) {
            conv.copy(otherUserId = otherId, myUnreadCount = unread)
        }
    }

    private suspend fun enrichMessage(msg: Message): Message {
        return try {
            val profile = client.postgrest["profiles"].select {
                filter { eq("id", msg.senderId) }
            }.decodeList<com.nagpurpulse.data.model.Profile>().firstOrNull()
            msg.copy(senderUsername = profile?.username)
        } catch (_: Exception) { msg }
    }
}
