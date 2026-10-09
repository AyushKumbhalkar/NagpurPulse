// java/com/nagpurpulse/data/model/PostRules.kt
//
// Small pure rules used by the post card. No Android / Compose imports.

package com.nagpurpulse.data.model

const val TRENDING_SCORE_THRESHOLD = 20
const val TRENDING_MAX_AGE_MINUTES = 48 * 60L

/**
 * Trending = enough engagement (comments count double) AND still recent.
 * Without the age limit a post would stay "Trending" forever.
 */
fun isPostTrending(upvotes: Int, commentCount: Int, ageMinutes: Long?): Boolean {
    val score = upvotes + commentCount * 2
    if (score < TRENDING_SCORE_THRESHOLD) return false
    return ageMinutes == null || ageMinutes <= TRENDING_MAX_AGE_MINUTES
}

/** Trimmed body for sharing, cut at [maxChars] without splitting an emoji. Null when empty. */
fun sharePreview(body: String?, maxChars: Int = 280): String? {
    val text = body?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (text.length <= maxChars) return text
    var end = (maxChars - 1).coerceAtLeast(1)
    if (end < text.length && end > 0 &&
        Character.isLowSurrogate(text[end]) && Character.isHighSurrogate(text[end - 1])
    ) end--
    return text.substring(0, end).trimEnd() + "…"
}
