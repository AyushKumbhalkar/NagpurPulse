// This is the MessageUtilis.kt file

// java/com/nagpurpulse/ui/screens/messages/MessagesUtils.kt

package com.nagpurpulse.ui.screens.messages

import androidx.compose.ui.graphics.Color
import com.nagpurpulse.ui.theme.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

fun incognitoColor(seed: String): Color {
    val hash = seed.fold(0L) { acc, c -> acc * 31 + c.code }
    val colors = listOf(OrangePrimary, PurpleNight, BlueInfo, GreenSuccess, PinkEvents, TealNeighborhood, RedAlert, Color(0xFFFFD60A))
    return colors[(hash.toInt().and(0x7FFFFFFF) % colors.size)]
}

fun incognitoEmoji(seed: String): String {
    val emojis = listOf("🦊","🐺","🦝","🐱","🦁","🐯","🦈","🦅","🦉","🐸","🦎","🐙","🦋","🦩","🦚","🐝","🦔","🦜","🦂","🦑")
    val hash = seed.fold(0L) { acc, c -> acc * 31 + c.code }
    return emojis[(hash.toInt().and(0x7FFFFFFF) % emojis.size)]
}

// ── Shared message constants ──────────────────────────────────────────────────

/** Text the app (and, best-effort, the server) uses for a message deleted for both people. */
const val DELETED_MESSAGE_TEXT = "This message was deleted"

fun isDeletedMessage(content: String): Boolean =
    content.trim().equals(DELETED_MESSAGE_TEXT, ignoreCase = true)

/** Quick reactions offered in the long-press menu. Keep in sync with nothing server-side: any short emoji is accepted. */
val QuickReactions = listOf("❤️", "😂", "👍", "😮", "😢", "🙏")

/** A conversation "needs a reply" when the other person spoke last and it has been this long. */
const val NEEDS_REPLY_AFTER_MINUTES = 180L

// ── Time formatting (shared by inbox + chat so both look the same) ────────────

private fun two(n: Int) = n.toString().padStart(2, '0')

/** Inbox timestamp: now · 5m · 14:05 · Yesterday · Mon · 12 Sep */
fun formatConvTime(ts: String): String = try {
    val inst = Instant.parse(ts)
    val diff = Duration.between(inst, Instant.now())
    val zoned = ZonedDateTime.ofInstant(inst, ZoneId.systemDefault())
    when {
        diff.toMinutes() < 1 -> "now"
        diff.toMinutes() < 60 -> "${diff.toMinutes()}m"
        diff.toHours() < 24 -> "${two(zoned.hour)}:${two(zoned.minute)}"
        diff.toDays() < 2 -> "Yesterday"
        diff.toDays() < 7 -> zoned.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        else -> "${zoned.dayOfMonth} ${zoned.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
    }
} catch (_: Exception) { "" }

/** Minutes since [ts], or null when it can't be parsed. */
fun minutesSince(ts: String): Long? = try {
    Duration.between(Instant.parse(ts), Instant.now()).toMinutes()
} catch (_: Exception) { null }

fun formatMsgTime(ts: String): String = try {
    val dt = ZonedDateTime.ofInstant(Instant.parse(ts), ZoneId.systemDefault())
    "${two(dt.hour)}:${two(dt.minute)}"
} catch (_: Exception) { "" }

fun formatMsgDate(ts: String): String = try {
    val dt = ZonedDateTime.ofInstant(Instant.parse(ts), ZoneId.systemDefault())
    val now = ZonedDateTime.now()
    when {
        dt.toLocalDate() == now.toLocalDate() -> "Today"
        dt.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
        else -> "${dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${dt.dayOfMonth} ${dt.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
    }
} catch (_: Exception) { "" }

fun sameDay(ts1: String, ts2: String): Boolean = try {
    val zone = ZoneId.systemDefault()
    Instant.parse(ts1).atZone(zone).toLocalDate() == Instant.parse(ts2).atZone(zone).toLocalDate()
} catch (_: Exception) { false }

/** True when two timestamps are within [minutes] of each other. */
fun withinMinutes(ts1: String, ts2: String, minutes: Long): Boolean = try {
    kotlin.math.abs(Duration.between(Instant.parse(ts1), Instant.parse(ts2)).toMinutes()) < minutes
} catch (_: Exception) { false }
