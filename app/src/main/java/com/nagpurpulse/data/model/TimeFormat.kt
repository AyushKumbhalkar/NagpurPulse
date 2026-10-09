// java/com/nagpurpulse/data/model/TimeFormat.kt
//
// Pure Kotlin (no Android / Compose imports) so it can be unit tested on the JVM.

package com.nagpurpulse.data.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses the timestamp formats Supabase/PostgREST can return:
 * `...Z`, `...+00:00` and offset-less values (treated as UTC). Returns null if unparseable.
 */
fun parsePostInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: Exception) {
        try {
            Instant.parse(value)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** Minutes since [createdAt], or null if it can't be parsed. Negative if the clock is skewed. */
fun ageMinutesOrNull(createdAt: String?, now: Instant = Instant.now()): Long? {
    val created = parsePostInstant(createdAt) ?: return null
    return Duration.between(created, now).toMinutes()
}

private val SameYearFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val OtherYearFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/**
 * "just now", "5m ago", "3h ago", "2d ago", "3w ago", then a real date ("12 Sep", "12 Sep 2025")
 * once a post is a month old. Falls back to "recently" when the timestamp can't be parsed.
 */
fun relativeTimeLabel(
    createdAt: String?,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val created = parsePostInstant(createdAt) ?: return "recently"
    val diff = Duration.between(created, now)
    val minutes = diff.toMinutes()
    return when {
        minutes < 1 -> "just now"            // also covers small clock skew (negative)
        minutes < 60 -> "${minutes}m ago"
        diff.toHours() < 24 -> "${diff.toHours()}h ago"
        diff.toDays() < 7 -> "${diff.toDays()}d ago"
        diff.toDays() < 30 -> "${diff.toDays() / 7}w ago"
        else -> {
            val date = created.atZone(zone).toLocalDate()
            val today = now.atZone(zone).toLocalDate()
            if (date.year == today.year) SameYearFormat.format(date) else OtherYearFormat.format(date)
        }
    }
}
