package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alert(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val body: String? = null,
    val category: String = "",
    @SerialName("area_tag") val areaTag: String? = null,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("alert_severity") val alertSeverity: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val username: String? = null
)

fun Alert.timeAgo(): String {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(created, now)
        when {
            diff.toMinutes() < 1 -> "just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()} min ago"
            diff.toHours() < 24 -> "${diff.toHours()} hour${if (diff.toHours() > 1) "s" else ""} ago"
            else -> "${diff.toDays()}d ago"
        }
    } catch (e: Exception) {
        "recently"
    }
}

fun Alert.alertEmoji(): String = when (category) {
    "traffic" -> "🚨"
    "alerts" -> "⚡"
    "weather" -> "🌧"
    "police" -> "🚔"
    "emergency" -> "🆘"
    else -> "⚠️"
}
