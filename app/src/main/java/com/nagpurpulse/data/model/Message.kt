//this is the Message.kt file
// java/com/nagpurpulse/data/model/Message.kt

package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id")       val senderId: String = "",
    val content: String = "",
    @SerialName("message_type")    val messageType: String = "text",   // text | image | voice
    @SerialName("is_read")         val isRead: Boolean = false,
    @SerialName("created_at")      val createdAt: String = "",
    // Enriched client-side only
    val senderUsername: String? = null,
    val senderAvatarUrl: String? = null
)

@Serializable
data class Conversation(
    val id: String = "",
    @SerialName("participant_one") val participantOne: String = "",
    @SerialName("participant_two") val participantTwo: String = "",
    @SerialName("last_message")    val lastMessage: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String = "",
    @SerialName("unread_count_one") val unreadCountOne: Int = 0,
    @SerialName("unread_count_two") val unreadCountTwo: Int = 0,
    @SerialName("created_at")      val createdAt: String = "",
    // Enriched client-side
    val otherUsername: String? = null,
    val otherAvatarSeed: String? = null,
    val otherUserId: String? = null,
    val myUnreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

fun Message.timeAgo(): String {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val now     = java.time.Instant.now()
        val diff    = java.time.Duration.between(created, now)
        when {
            diff.toMinutes() < 1  -> "just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24   -> "${diff.toHours()}h ago"
            diff.toDays() < 2     -> "Yesterday"
            else                  -> {
                val dt = java.time.ZonedDateTime.ofInstant(created, java.time.ZoneId.systemDefault())
                "${dt.hour.toString().padStart(2,'0')}:${dt.minute.toString().padStart(2,'0')}"
            }
        }
    } catch (_: Exception) { "" }
}

fun Message.shortTime(): String {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val now     = java.time.Instant.now()
        val diff    = java.time.Duration.between(created, now)
        when {
            diff.toMinutes() < 60  -> "${diff.toMinutes()}m"
            diff.toHours()   < 24  -> {
                val dt = java.time.ZonedDateTime.ofInstant(created, java.time.ZoneId.systemDefault())
                "${dt.hour.toString().padStart(2,'0')}:${dt.minute.toString().padStart(2,'0')}"
            }
            diff.toDays() < 2      -> "Yesterday"
            diff.toDays() < 7      -> {
                val dt = java.time.ZonedDateTime.ofInstant(created, java.time.ZoneId.systemDefault())
                dt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
            }
            else -> {
                val dt = java.time.ZonedDateTime.ofInstant(created, java.time.ZoneId.systemDefault())
                "${dt.dayOfMonth} ${dt.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)}"
            }
        }
    } catch (_: Exception) { "" }
}
