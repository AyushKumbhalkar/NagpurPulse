// data/model/UserPreferences.kt  — REPLACE your existing file entirely
package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(

    @SerialName("user_id")          val userId: String,
    @SerialName("language")         val language: String  = "en",
    @SerialName("text_size")        val textSize: String  = "medium",
    @SerialName("display_density")  val displayDensity: String = "comfortable",
    @SerialName("feed_style")       val feedStyle: String = "expanded",
    @SerialName("amoled_mode")      val amoledMode: Boolean = false,
    @SerialName("reduce_animations")val reduceAnimations: Boolean = false,
    @SerialName("large_media")      val largeMedia: Boolean = true,

    // ── Notification preferences ──────────────────────────────────────
    // Master push toggle — disabling this cancels all scheduled workers
    @SerialName("notif_push")            val notifPush: Boolean = true,
    // FCM / in-app per-type toggles
    @SerialName("notif_replies")         val notifReplies: Boolean = true,
    @SerialName("notif_mentions")        val notifMentions: Boolean = true,
    @SerialName("notif_messages")        val notifMessages: Boolean = true,
    @SerialName("notif_upvotes")         val notifUpvotes: Boolean = true,
    // Scheduled worker toggles
    @SerialName("notif_digest")          val notifDigest: Boolean = true,
    @SerialName("notif_trending")        val notifTrending: Boolean = true,
    @SerialName("notif_community")       val notifCommunity: Boolean = true,
    @SerialName("notif_alerts_summary")  val notifAlertsSummary: Boolean = true,
    // UI state — persisted so banner never reappears after dismiss
    @SerialName("notif_banner_dismissed")val notifBannerDismissed: Boolean = false
)
