// notifications/PulseStyle.kt
// Single source of truth for how each notification type LOOKS and BEHAVES in the
// system tray: vector glyph (no emoji), accent colour, channel, category and group.
package com.nagpurpulse.notifications

import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import com.nagpurpulse.R

internal data class PulseSpec(
    val channelId: String,
    @DrawableRes val glyph: Int,
    val accent: Int,
    val label: String,
    val category: String,
    val groupKey: String,
    val highPriority: Boolean,
    /** Auto-dismiss stale, time-sensitive updates (0 = never). */
    val timeoutMs: Long = 0L
)

internal object PulseStyle {

    const val GROUP_SOCIAL = "com.nagpurpulse.group.SOCIAL"
    const val GROUP_MESSAGES = "com.nagpurpulse.group.MESSAGES"
    const val GROUP_ALERTS = "com.nagpurpulse.group.ALERTS"
    const val GROUP_DISCOVER = "com.nagpurpulse.group.DISCOVER"

    /** Fixed ids for the per-group summary notifications. Child ids are always >= 0x10000. */
    fun summaryIdFor(groupKey: String): Int = when (groupKey) {
        GROUP_SOCIAL -> 9001
        GROUP_MESSAGES -> 9002
        GROUP_ALERTS -> 9003
        else -> 9004
    }

    private const val ORANGE = 0xFFFF6B00.toInt()
    private const val BLUE = 0xFF0A84FF.toInt()
    private const val GREEN = 0xFF1FA64A.toInt()
    private const val PINK = 0xFFFF375F.toInt()
    private const val RED = 0xFFE5322B.toInt()
    private const val GOLD = 0xFFE6A100.toInt()
    private const val PURPLE = 0xFF7C3AED.toInt()
    private const val AMBER = 0xFFF59E0B.toInt()

    private const val HOURS_12 = 12L * 60 * 60 * 1000
    private const val HOURS_36 = 36L * 60 * 60 * 1000

    fun specFor(type: String): PulseSpec = when (type.lowercase()) {
        "comment" -> PulseSpec(
            CHANNEL_SOCIAL, R.drawable.ic_notif_comment, ORANGE, "New comment",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_SOCIAL, true
        )
        "reply" -> PulseSpec(
            CHANNEL_SOCIAL, R.drawable.ic_notif_comment, ORANGE, "New reply",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_SOCIAL, true
        )
        "mention" -> PulseSpec(
            CHANNEL_SOCIAL, R.drawable.ic_notif_mention, BLUE, "Mention",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_SOCIAL, true
        )
        "upvote", "like" -> PulseSpec(
            CHANNEL_SOCIAL, R.drawable.ic_notif_upvote, GREEN, "Upvote",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_SOCIAL, true
        )
        "comment_like" -> PulseSpec(
            CHANNEL_SOCIAL, R.drawable.ic_notif_like, PINK, "Comment like",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_SOCIAL, true
        )
        "message" -> PulseSpec(
            CHANNEL_MESSAGES, R.drawable.ic_notif_message, BLUE, "Message",
            NotificationCompat.CATEGORY_MESSAGE, GROUP_MESSAGES, true
        )
        "alert", "emergency", "alerts_summary" -> PulseSpec(
            CHANNEL_ALERTS, R.drawable.ic_notif_alert, RED, "City alert",
            NotificationCompat.CATEGORY_EVENT, GROUP_ALERTS, true
        )
        "trending" -> PulseSpec(
            CHANNEL_TRENDING, R.drawable.ic_notif_trending, ORANGE, "Trending",
            NotificationCompat.CATEGORY_RECOMMENDATION, GROUP_DISCOVER, false, HOURS_36
        )
        "milestone", "badge" -> PulseSpec(
            CHANNEL_REWARDS, R.drawable.ic_notif_trophy, GOLD, "Milestone",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_SOCIAL, true
        )
        "streak" -> PulseSpec(
            CHANNEL_REWARDS, R.drawable.ic_notif_streak, ORANGE, "Streak",
            NotificationCompat.CATEGORY_RECOMMENDATION, GROUP_DISCOVER, false, HOURS_12
        )
        "community" -> PulseSpec(
            CHANNEL_COMMUNITY, R.drawable.ic_notif_community, PURPLE, "Community",
            NotificationCompat.CATEGORY_RECOMMENDATION, GROUP_DISCOVER, false, HOURS_12
        )
        "digest" -> PulseSpec(
            CHANNEL_DIGEST, R.drawable.ic_notif_morning, AMBER, "Daily digest",
            NotificationCompat.CATEGORY_RECOMMENDATION, GROUP_DISCOVER, false, HOURS_12
        )
        "nudge" -> PulseSpec(
            CHANNEL_FOR_YOU, R.drawable.ic_notif_trending, ORANGE, "For you",
            NotificationCompat.CATEGORY_RECOMMENDATION, GROUP_DISCOVER, false, HOURS_36
        )
        "admin_warning" -> PulseSpec(
            CHANNEL_MAIN, R.drawable.ic_notif_alert, AMBER, "Account notice",
            NotificationCompat.CATEGORY_STATUS, GROUP_ALERTS, true
        )
        "admin_suspension", "admin_ban" -> PulseSpec(
            CHANNEL_MAIN, R.drawable.ic_notif_block, RED, "Account notice",
            NotificationCompat.CATEGORY_STATUS, GROUP_ALERTS, true
        )
        else -> PulseSpec(
            CHANNEL_MAIN, R.drawable.ic_notif_bell, ORANGE, "Nagpur Pulse",
            NotificationCompat.CATEGORY_SOCIAL, GROUP_DISCOVER, true
        )
    }

    // ── Text hygiene ──────────────────────────────────────────────────────────

    // Pictographs, dingbats, misc symbols, regional-indicator flags, variation
    // selectors, ZWJ and keycap marks. Deliberately narrower than \p{So} so that
    // (c), (r), degree signs and other ordinary symbols survive.
    private val EMOJI = Regex(
        "[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{2300}-\\x{23FF}" +
            "\\x{FE0E}\\x{FE0F}\\x{200D}\\x{20E3}]"
    )
    private val SPACES = Regex("\\s{2,}")

    /** Removes emoji from SYSTEM-written copy (titles). Never use on user content. */
    fun stripEmoji(text: String?): String =
        (text ?: "").replace(EMOJI, "").replace(SPACES, " ").trim()

    /** Joins names as "A", "A and B", "A, B and 3 others". */
    fun whoLabel(names: List<String>): String {
        val shown = names.take(2)
        val extra = names.size - shown.size
        return when {
            shown.isEmpty() -> "Someone"
            extra <= 0 -> shown.joinToString(" and ")
            else -> shown.joinToString(", ") + " and $extra ${if (extra == 1) "other" else "others"}"
        }
    }

    fun reactionAction(type: String): String = when (type.lowercase()) {
        "comment_like" -> "liked your comment"
        else -> "upvoted your post"
    }
}
