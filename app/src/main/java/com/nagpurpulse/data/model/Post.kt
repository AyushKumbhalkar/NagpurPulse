// Post.kt file

// java/com/nagpurpulse/data/model/Post.kt

package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Post(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",

    @SerialName("post_type")
    val postType: String = "normal",

    val body: String? = null,
    val category: String = "",
    @SerialName("area_tag") val areaTag: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    val upvotes: Int = 0,
    val downvotes: Int = 0,

    val userVote: String? = null,

    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_alert") val isAlert: Boolean = false,
    @SerialName("alert_severity") val alertSeverity: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val username: String? = null,

    @SerialName("is_verified")
    val isVerified: Boolean = false,

    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_locked") val isLocked: Boolean = false,

    // Filled client-side by PostRepository.enrichPostsWithUsernames(); never sent to
    // or read from the posts table.
    @Transient val authorAvatarUrl: String? = null
)

fun Post.timeAgo(): String {
    // Simple time ago calculation
    return try {
        val created = java.time.Instant.parse(createdAt)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(created, now)
        when {
            diff.toMinutes() < 1 -> "just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24 -> "${diff.toHours()}h ago"
            diff.toDays() < 7 -> "${diff.toDays()}d ago"
            else -> "${diff.toDays() / 7}w ago"
        }
    } catch (e: Exception) {
        "recently"
    }
}

fun Post.categoryEmoji(): String = when (category) {
    "food" -> "🍜"
    "nightlife" -> "🌙"
    "jobs" -> "💼"
    "college" -> "🎓"
    "rants" -> "😤"
    "neighborhoods" -> "🏘"
    "lost_found" -> "🔍"
    "events" -> "🎉"
    "traffic" -> "🚗"
    "alerts" -> "🚨"
    else -> "💬"
}

fun Post.categoryDisplay(): String = when (category) {
    "food" -> "Food"
    "nightlife" -> "Nightlife"
    "jobs" -> "Jobs"
    "college" -> "College Life"
    "rants" -> "Rants"
    "neighborhoods" -> "Neighborhoods"
    "lost_found" -> "Lost & Found"
    "events" -> "Events"
    "traffic" -> "Traffic"
    "alerts" -> "Alerts"
    else -> category.replaceFirstChar { it.uppercase() }
}
