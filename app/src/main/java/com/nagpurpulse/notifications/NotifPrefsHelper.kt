// notifications/NotifPrefsHelper.kt
// SharedPreferences mirror of notification settings.
// The FCM service and background workers use this (no Supabase access in background).
// The NotifSettingsViewModel writes here whenever a preference changes.
package com.nagpurpulse.notifications

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "nagpur_notif_prefs"

object NotifPrefsHelper {

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Write (called by NotifSettingsViewModel on every toggle) ──────────────
    fun saveAll(ctx: Context, prefs: Map<String, Boolean>) {
        this.prefs(ctx).edit().apply {
            prefs.forEach { (k, v) -> putBoolean(k, v) }
            apply()
        }
    }

    fun save(ctx: Context, key: String, value: Boolean) {
        prefs(ctx).edit().putBoolean(key, value).apply()
    }

    // ── Read (called by FCM service / workers) ────────────────────────────────
    fun isPushEnabled(ctx: Context)          = prefs(ctx).getBoolean("notif_push",            true)
    fun isRepliesEnabled(ctx: Context)       = prefs(ctx).getBoolean("notif_replies",         true)
    fun isMentionsEnabled(ctx: Context)      = prefs(ctx).getBoolean("notif_mentions",        true)
    fun isMessagesEnabled(ctx: Context)      = prefs(ctx).getBoolean("notif_messages",        true)
    fun isUpvotesEnabled(ctx: Context)       = prefs(ctx).getBoolean("notif_upvotes",         true)
    fun isDigestEnabled(ctx: Context)        = prefs(ctx).getBoolean("notif_digest",          true)
    fun isTrendingEnabled(ctx: Context)      = prefs(ctx).getBoolean("notif_trending",        true)
    fun isCommunityEnabled(ctx: Context)     = prefs(ctx).getBoolean("notif_community",       true)
    fun isAlertsSummaryEnabled(ctx: Context) = prefs(ctx).getBoolean("notif_alerts_summary",  true)
    fun isBannerDismissed(ctx: Context)      = prefs(ctx).getBoolean("notif_banner_dismissed", false)

    fun shouldShowType(ctx: Context, type: String): Boolean {
        if (!isPushEnabled(ctx)) return false
        return when (type) {
            "comment", "reply"          -> isRepliesEnabled(ctx)
            "mention"                   -> isMentionsEnabled(ctx)
            "message"                   -> isMessagesEnabled(ctx)
            "upvote", "like"            -> isUpvotesEnabled(ctx)
            "alert", "emergency"        -> true  // alerts always shown
            "trending"                  -> isTrendingEnabled(ctx)
            "community"                 -> isCommunityEnabled(ctx)
            "digest"                    -> isDigestEnabled(ctx)
            else                        -> true
        }
    }

    // Sync from Supabase UserPreferences into SharedPreferences (on login/settings load)
    fun syncFromUserPreferences(ctx: Context, prefs: com.nagpurpulse.data.model.UserPreferences) {
        saveAll(ctx, mapOf(
            "notif_push"             to prefs.notifPush,
            "notif_replies"          to prefs.notifReplies,
            "notif_mentions"         to prefs.notifMentions,
            "notif_messages"         to prefs.notifMessages,
            "notif_upvotes"          to prefs.notifUpvotes,
            "notif_digest"           to prefs.notifDigest,
            "notif_trending"         to prefs.notifTrending,
            "notif_community"        to prefs.notifCommunity,
            "notif_alerts_summary"   to prefs.notifAlertsSummary,
            "notif_banner_dismissed" to prefs.notifBannerDismissed
        ))
    }
}
