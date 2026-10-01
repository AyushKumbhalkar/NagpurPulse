// data/model/Notification.kt  — REPLACE entirely
package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String = "",
    @SerialName("user_id")          val userId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String? = null,
    @SerialName("is_read")          val isRead: Boolean = false,
    @SerialName("related_post_id")  val relatedPostId: String? = null,
    @SerialName("sender_username")  val senderUsername: String? = null,
    @SerialName("sender_avatar_url")val senderAvatarUrl: String? = null,
    @SerialName("created_at")       val createdAt: String = ""
)

fun Notification.timeAgo(): String {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val diff    = java.time.Duration.between(created, java.time.Instant.now())
        when {
            diff.toMinutes() < 1  -> "just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()} min ago"
            diff.toHours()   < 24 -> "${diff.toHours()}h ago"
            diff.toDays()    < 2  -> "Yesterday"
            else                   -> "${diff.toDays()}d ago"
        }
    } catch (_: Exception) { "recently" }
}

// ── Type metadata ─────────────────────────────────────────────────────────────

fun Notification.emoji(): String = when (type) {
    "comment", "reply"            -> "💬"
    "mention"                     -> "📣"
    "upvote", "like"              -> "⬆️"
    "message"                     -> "✉️"
    "alert", "emergency"          -> "🚨"
    "badge"                       -> "🏆"
    "trending"                    -> "🔥"
    "community"                   -> "🌆"
    "digest"                      -> "📰"
    "admin_warning"               -> "⚠️"
    "admin_suspension"            -> "🚫"
    "admin_ban"                   -> "❌"
    else                          -> "🔔"
}

// ── Badge model ───────────────────────────────────────────────────────────────

@Serializable
data class Badge(
    val id: String = "",
    @SerialName("user_id")    val userId: String = "",
    @SerialName("badge_type") val badgeType: String = "",
    @SerialName("earned_at")  val earnedAt: String = ""
)

fun Badge.displayName(): String = badgeType
    .replace("_", " ")
    .split(" ")
    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

fun Badge.emoji(): String = when (badgeType) {
    "food_expert"     -> "🍜"
    "night_owl"       -> "🌙"
    "top_contributor" -> "🔥"
    "area_local"      -> "🏘"
    else              -> "🏆"
}
