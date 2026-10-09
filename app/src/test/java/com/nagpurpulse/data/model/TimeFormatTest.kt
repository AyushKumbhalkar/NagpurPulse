package com.nagpurpulse.data.model

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeFormatTest {
    private val now = Instant.parse("2026-10-09T12:00:00Z")
    private fun label(s: String?) = relativeTimeLabel(s, now, ZoneOffset.UTC)

    @Test fun recent() {
        assertEquals("just now", label("2026-10-09T11:59:40Z"))
        assertEquals("just now", label("2026-10-09T12:00:30Z")) // clock skew
        assertEquals("5m ago", label("2026-10-09T11:55:00Z"))
    }

    @Test fun acceptsEverySupabaseTimestampShape() {
        assertEquals("5m ago", label("2026-10-09T11:54:59.123456+00:00"))
        assertEquals("5m ago", label("2026-10-09T11:55:00"))
    }

    @Test fun hoursDaysWeeks() {
        assertEquals("3h ago", label("2026-10-09T09:00:00Z"))
        assertEquals("2d ago", label("2026-10-07T12:00:00Z"))
        assertEquals("3w ago", label("2026-09-18T12:00:00Z"))
    }

    @Test fun oldPostsShowADate() {
        assertEquals("12 Aug", label("2026-08-12T12:00:00Z"))
        assertEquals("12 Sep 2025", label("2025-09-12T12:00:00Z"))
    }

    @Test fun garbageFallsBack() {
        assertEquals("recently", label("nope"))
        assertEquals("recently", label(null))
        assertEquals("recently", label(""))
    }

    @Test fun ageMinutes() {
        assertEquals(30L, ageMinutesOrNull("2026-10-09T11:30:00+00:00", now))
        assertNull(ageMinutesOrNull("x", now))
    }

    @Test fun trendingNeedsScoreAndRecency() {
        assertTrue(isPostTrending(15, 3, 100))
        assertFalse(isPostTrending(5, 2, 10))
        assertFalse(isPostTrending(100, 50, 49 * 60L))
        assertTrue(isPostTrending(20, 0, null))
    }

    @Test fun sharePreview() {
        assertNull(sharePreview(null))
        assertNull(sharePreview("   "))
        assertEquals("hello", sharePreview("  hello "))
        val cut = sharePreview("a".repeat(400))!!
        assertEquals(280, cut.length)
        assertTrue(cut.endsWith("…"))
        val emoji = sharePreview("a".repeat(278) + "😀😀😀")!!
        assertFalse(Character.isHighSurrogate(emoji[emoji.length - 2]))
    }
}
