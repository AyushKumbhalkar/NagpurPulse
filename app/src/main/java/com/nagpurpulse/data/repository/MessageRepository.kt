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
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class HiddenMessageRow(val message_id: String = "")

@Serializable
private data class HiddenConversationRow(val conversation_id: String = "")

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
            // Check both orderings (p1,p2) and (p2,p1)
            val existing = client.postgrest["conversations"].select {
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
            }.decodeList<Conversation>().firstOrNull()

            if (existing != null) {
                return Result.success(enrichConversation(existing, myId))
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
            Result.success(rows.filterNot { it.id in hiddenIds }.map { enrichConversation(it, myId) })
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
            Result.success(row?.let { enrichConversation(it, myId) })
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

    // ── Send a message ────────────────────────────────────────────────────────
    suspend fun sendMessage(conversationId: String, content: String): Result<Message> {
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

            Result.success(enrichMessage(msg))
        } catch (e: Exception) {
            Result.failure(e)
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
        client.postgrest["messages"].delete { filter { eq("conversation_id", conversationId) } }
        client.postgrest["conversation_hidden_for_users"].delete { filter { eq("conversation_id", conversationId) } }
        client.postgrest["conversations"].delete { filter { eq("id", conversationId) } }
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
        client.postgrest["messages"].update(buildJsonObject { put("content", "This message was deleted") }) { filter { eq("id", messageId) } }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun editMessage(messageId: String, content: String): Result<Unit> = try {
        client.postgrest["messages"].update(buildJsonObject { put("content", content) }) { filter { eq("id", messageId) } }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
    // ── Realtime: subscribe to new messages in a conversation ─────────────────
    fun subscribeToMessages(conversationId: String): Flow<Message> = kotlinx.coroutines.flow.flow {

        val channel = client.realtime.channel("messages_$conversationId")

        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(
            schema = "public"
        ) {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }

        android.util.Log.d(
            "CHAT_RT",
            "Before subscribe"
        )

        channel.subscribe(
            blockUntilSubscribed = true
        )

        android.util.Log.d(
            "CHAT_RT",
            "After subscribe"
        )

        changes.collect { action ->

            android.util.Log.d(
                "CHAT_RT",
                "Realtime received: ${action.record}"
            )

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

        // Register both event flows before subscribing, so newly created chats
        // and message/unread updates can refresh the inbox in real time.
        channel.subscribe(blockUntilSubscribed = true)
        merge(inserts, updates).collect { conv ->
            emit(enrichConversation(conv, myId))
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
    private suspend fun enrichConversation(conv: Conversation, myId: String): Conversation {
        val otherId = if (conv.participantOne == myId) conv.participantTwo else conv.participantOne
        val unread  = if (conv.participantOne == myId) conv.unreadCountOne else conv.unreadCountTwo
        return try {
            val profile = client.postgrest["profiles"].select {
                filter { eq("id", otherId) }
            }.decodeList<com.nagpurpulse.data.model.Profile>().firstOrNull()
            conv.copy(
                otherUsername = profile?.username,
                otherAvatarSeed = profile?.username ?: otherId,
                otherUserId   = otherId,
                myUnreadCount = unread
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
