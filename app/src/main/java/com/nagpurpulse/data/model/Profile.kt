//java/com/nagpurpulse/data/model/Profile.kt
package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val username: String = "",

    @SerialName("display_name")
    val displayName: String? = null,

    val tagline: String? = null,
    @SerialName("avatar_url")  val avatarUrl: String? = null,
    @SerialName("cover_url")   val coverUrl: String? = null,
    @SerialName("gender")
    val gender: String? = null,
    val areas: List<String> = emptyList(),
    val karma: Int = 0,

    @SerialName("is_verified")
    val isVerified: Boolean = false,

    val location: String? = null,
    val website: String? = null,
    @SerialName("hide_comments")      val hideComments: Boolean = false,
    @SerialName("hide_posts")         val hidePosts: Boolean = false,
    @SerialName("hide_profile")       val hideProfile: Boolean = false,
    @SerialName("allow_dms")          val allowDms: Boolean = true,
    @SerialName("show_online_status") val showOnlineStatus: Boolean = true,
    @SerialName("incognito_mode")     val incognitoMode: Boolean = false,
    @SerialName("hide_from_search")   val hideFromSearch: Boolean = false,
    @SerialName("is_deactivated")     val isDeactivated: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

fun Profile.memberSince(): String {
    return try {
        val instant = java.time.Instant.parse(createdAt)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val month = date.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH)
        "Member since $month ${date.year}"
    } catch (e: Exception) {
        "Member since 2024"
    }
}

// ── User privacy/settings stored in profiles table ─────────────────────────
@Serializable
data class UserPrivacySettings(
    @SerialName("hide_comments")        val hideComments: Boolean = false,
    @SerialName("hide_posts")           val hidePosts: Boolean = false,
    @SerialName("hide_profile")         val hideProfile: Boolean = false,
    @SerialName("allow_dms")            val allowDms: Boolean = true,
    @SerialName("show_online_status")   val showOnlineStatus: Boolean = true,
    @SerialName("incognito_mode")       val incognitoMode: Boolean = false,
    @SerialName("hide_from_search")     val hideFromSearch: Boolean = false
)

@Serializable
data class UserNotifSettings(
    @SerialName("push_enabled")         val pushEnabled: Boolean = true,
    @SerialName("comment_replies")      val commentReplies: Boolean = true,
    @SerialName("mention_alerts")       val mentionAlerts: Boolean = true,
    @SerialName("message_notifs")       val messageNotifs: Boolean = true,
    @SerialName("upvote_notifs")        val upvoteNotifs: Boolean = true
)
