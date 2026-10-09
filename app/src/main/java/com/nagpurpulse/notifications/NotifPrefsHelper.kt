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

    /** Local timestamp of the last banner dismissal (enables one gentle re-show). */
    fun markBannerDismissedNow(ctx: Context) {
        prefs(ctx).edit().putLong("notif_banner_dismissed_at", System.currentTimeMillis()).apply()
    }

    /**
     * True when the push banner should be visible. A banner dismissed once is shown
     * exactly one more time after 7 days; legacy dismissals without a timestamp stay hidden.
     */
    fun consumeBannerVisibility(ctx: Context): Boolean {
        if (!isBannerDismissed(ctx)) return true
        val dismissedAt = prefs(ctx).getLong("notif_banner_dismissed_at", 0L)
        if (dismissedAt <= 0L || prefs(ctx).getBoolean("notif_banner_reshown", false)) return false
        if (System.currentTimeMillis() - dismissedAt < 7L * 24 * 60 * 60 * 1000) return false
        prefs(ctx).edit().putBoolean("notif_banner_reshown", true).apply()
        return true
    }

    // ── Pause & quiet hours (device-local; no backend column needed) ──────────
    const val SLOT_MORNING = "morning"
    const val SLOT_TRENDING = "trending"
    const val SLOT_EVENING = "evening"
    const val SLOT_NIGHT = "night"

    fun pausedUntil(ctx: Context): Long = prefs(ctx).getLong("notif_paused_until", 0L)

    fun isPaused(ctx: Context): Boolean = System.currentTimeMillis() < pausedUntil(ctx)

    /** Pause everything except emergency alerts for [hours]; pass 0 to resume. */
    fun pauseFor(ctx: Context, hours: Int) {
        val until = if (hours <= 0) 0L else System.currentTimeMillis() + hours * 3_600_000L
        prefs(ctx).edit().putLong("notif_paused_until", until).apply()
    }

    fun isQuietHoursEnabled(ctx: Context) = prefs(ctx).getBoolean("notif_quiet_enabled", false)
    fun quietStartMinutes(ctx: Context) = prefs(ctx).getInt("notif_quiet_start", 23 * 60)
    fun quietEndMinutes(ctx: Context) = prefs(ctx).getInt("notif_quiet_end", 7 * 60)

    fun setQuietHours(ctx: Context, enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        prefs(ctx).edit()
            .putBoolean("notif_quiet_enabled", enabled)
            .putInt("notif_quiet_start", startMinutes)
            .putInt("notif_quiet_end", endMinutes)
            .apply()
    }

    fun isInQuietHours(ctx: Context): Boolean {
        if (!isQuietHoursEnabled(ctx)) return false
        val cal = java.util.Calendar.getInstance()
        val now = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val start = quietStartMinutes(ctx)
        val end = quietEndMinutes(ctx)
        return when {
            start == end -> false
            start < end  -> now in start until end
            else         -> now >= start || now < end   // window wraps past midnight
        }
    }

    fun isSilencedNow(ctx: Context): Boolean = isPaused(ctx) || isInQuietHours(ctx)

    // ── Delivery times for the scheduled updates (minutes after midnight) ─────
    private fun defaultMinutes(slot: String) = when (slot) {
        SLOT_MORNING  -> 8 * 60
        SLOT_TRENDING -> 13 * 60
        SLOT_EVENING  -> 18 * 60
        SLOT_NIGHT    -> 22 * 60
        else          -> 8 * 60
    }

    fun scheduledMinutes(ctx: Context, slot: String): Int =
        prefs(ctx).getInt("notif_time_$slot", defaultMinutes(slot))

    fun setScheduledMinutes(ctx: Context, slot: String, minutes: Int) {
        prefs(ctx).edit().putInt("notif_time_$slot", minutes.coerceIn(0, 24 * 60 - 1)).apply()
    }

    fun shouldShowType(ctx: Context, type: String): Boolean {
        if (!isPushEnabled(ctx)) return false
        // Pause / quiet hours silence everything except emergency alerts.
        if (type != "alert" && type != "emergency" && isSilencedNow(ctx)) return false
        return when (type) {
            "comment", "reply"          -> isRepliesEnabled(ctx)
            "mention"                   -> isMentionsEnabled(ctx)
            "message"                   -> isMessagesEnabled(ctx)
            "upvote", "like", "comment_like" -> isUpvotesEnabled(ctx)
            "alert", "emergency"        -> true  // alerts always shown
            "trending"                  -> isTrendingEnabled(ctx)
            "community"                 -> isCommunityEnabled(ctx)
            "digest"                    -> isDigestEnabled(ctx)
            "alerts_summary"             -> isAlertsSummaryEnabled(ctx)
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
