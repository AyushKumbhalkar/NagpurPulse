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

// ── Quick replies ─────────────────────────────────────────────────────────────

/** One-tap answers offered when the other person spoke last. */
val QuickReplies = listOf("👍", "Thanks!", "Sounds good")

// ── Reactions ─────────────────────────────────────────────────────────────────

/**
 * One reaction per person per message: removes [me] from every emoji, then adds [me] to [emoji]
 * unless they were toggling the same emoji off. Mirrors the server's toggle_message_reaction().
 */
fun applyReaction(
    reactions: Map<String, List<String>>,
    me: String,
    emoji: String
): Map<String, List<String>> {
    val hadSame = reactions[emoji]?.contains(me) == true
    val cleaned = reactions
        .mapValues { (_, users) -> users.filterNot { it == me } }
        .filterValues { it.isNotEmpty() }
    return if (hadSame) cleaned else cleaned + (emoji to ((cleaned[emoji] ?: emptyList()) + me))
}

// ── Links ─────────────────────────────────────────────────────────────────────

data class LinkSpan(val start: Int, val end: Int, val url: String)

private val UrlRegex = Regex("""(?i)(?:https?://|www\.)[^\s<>]+""")
private const val TRAILING_PUNCTUATION = ".,;:!?)]}'\""

fun normalizeUrl(raw: String): String =
    if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw

/** Host without "www." for display ("Open example.com?"). Falls back to the raw text. */
fun urlHost(url: String): String =
    runCatching { java.net.URI(url).host }.getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
        ?: url

/** Finds http(s):// and www. links, trimming trailing punctuation like the "." in "see x.com." */
fun findLinks(text: String): List<LinkSpan> =
    UrlRegex.findAll(text).mapNotNull { match ->
        val start = match.range.first
        var end = match.range.last + 1
        while (end > start && text[end - 1] in TRAILING_PUNCTUATION) end--
        val raw = text.substring(start, end)
        val url = normalizeUrl(raw)
        // Needs a real host with a dot, e.g. "https://x" or "www." are ignored.
        val host = runCatching { java.net.URI(url).host }.getOrNull()
        if (host.isNullOrBlank() || !host.contains('.')) null else LinkSpan(start, end, url)
    }.toList()

// ── Emoji-only messages ───────────────────────────────────────────────────────

/**
 * If [text] contains nothing but emoji (and spaces), returns how many emoji it holds
 * (flags, skin tones and joined families count as one). Returns 0 for anything else.
 * Used to show 1–3 emoji big and bubble-less.
 */
fun emojiOnlyCount(text: String): Int {
    val t = text.trim()
    if (t.isEmpty() || t.length > 32) return 0

    var clusters = 0
    var afterJoiner = false
    var halfFlag = false
    var i = 0
    while (i < t.length) {
        val cp = t.codePointAt(i)
        i += Character.charCount(cp)
        when {
            cp == 0x200D -> afterJoiner = true                       // zero-width joiner
            cp == 0xFE0F || cp == 0x20E3 || cp in 0x1F3FB..0x1F3FF -> Unit // variation selector, keycap, skin tone
            Character.isWhitespace(cp) -> { afterJoiner = false; halfFlag = false }
            cp in 0x1F1E6..0x1F1FF -> {                              // regional indicators pair into a flag
                if (halfFlag) halfFlag = false else { halfFlag = true; clusters++ }
                afterJoiner = false
            }
            cp >= 0x2190 && Character.getType(cp) == Character.OTHER_SYMBOL.toInt() -> {
                if (!afterJoiner) clusters++
                afterJoiner = false
                halfFlag = false
            }
            else -> return 0
        }
    }
    return clusters
}
