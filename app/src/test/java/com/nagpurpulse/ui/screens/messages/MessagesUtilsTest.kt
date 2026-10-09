package com.nagpurpulse.ui.screens.messages

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesUtilsTest {

    // ── Links ────────────────────────────────────────────────────────────────

    @Test fun findsWwwLinkAndTrimsTrailingPeriod() {
        val text = "see www.example.com."
        val links = findLinks(text)
        assertEquals(1, links.size)
        assertEquals("https://www.example.com", links[0].url)
        assertEquals("www.example.com", text.substring(links[0].start, links[0].end))
    }

    @Test fun findsHttpsLinkAndTrimsComma() {
        val links = findLinks("Visit https://nagpurpulse.app/post/1, ok")
        assertEquals(1, links.size)
        assertEquals("https://nagpurpulse.app/post/1", links[0].url)
    }

    @Test fun findsMultipleLinks() {
        assertEquals(2, findLinks("a https://one.com b http://two.org/x").size)
    }

    @Test fun ignoresTextWithoutRealLinks() {
        assertTrue(findLinks("no links here").isEmpty())
        assertTrue(findLinks("https://x").isEmpty())      // host has no dot
        assertTrue(findLinks("see www. later").isEmpty())  // nothing after www.
    }

    @Test fun hostIsShownWithoutWww() {
        assertEquals("example.com", urlHost("https://www.example.com/a/b"))
        assertEquals("sub.example.org", urlHost("http://sub.example.org"))
    }

    // ── Emoji-only messages ──────────────────────────────────────────────────

    @Test fun countsSimpleEmoji() {
        assertEquals(1, emojiOnlyCount("😂"))
        assertEquals(3, emojiOnlyCount("😂😂😂"))
        assertEquals(4, emojiOnlyCount("😂😂😂😂"))
        assertEquals(2, emojiOnlyCount("😂 😂"))
    }

    @Test fun treatsJoinedAndModifiedEmojiAsOne() {
        assertEquals(1, emojiOnlyCount("\u2764\uFE0F"))                                   // red heart + variation selector
        assertEquals(1, emojiOnlyCount("\uD83D\uDC4D\uD83C\uDFFD"))                       // thumbs up + skin tone
        assertEquals(1, emojiOnlyCount("\uD83C\uDDEE\uD83C\uDDF3"))                       // flag: India
        assertEquals(1, emojiOnlyCount("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67")) // family (ZWJ sequence)
    }

    @Test fun rejectsAnythingThatIsNotOnlyEmoji() {
        assertEquals(0, emojiOnlyCount("hi 😂"))
        assertEquals(0, emojiOnlyCount("123"))
        assertEquals(0, emojiOnlyCount(""))
        assertEquals(0, emojiOnlyCount("   "))
    }

    // ── Reactions ────────────────────────────────────────────────────────────

    @Test fun reactionAddsThenRemovesOnSecondTap() {
        val added = applyReaction(emptyMap(), "u1", "❤️")
        assertEquals(mapOf("❤️" to listOf("u1")), added)
        assertEquals(emptyMap<String, List<String>>(), applyReaction(added, "u1", "❤️"))
    }

    @Test fun reactionMovesWhenSwitchingEmoji() {
        val start = mapOf("❤️" to listOf("u1", "u2"))
        val result = applyReaction(start, "u1", "👍")
        assertEquals(listOf("u2"), result["❤️"])
        assertEquals(listOf("u1"), result["👍"])
    }

    @Test fun reactionDropsEmptyEmojiEntries() {
        val result = applyReaction(mapOf("❤️" to listOf("u1")), "u1", "👍")
        assertFalse(result.containsKey("❤️"))
    }

    // ── Time helpers ─────────────────────────────────────────────────────────

    @Test fun sameDayAndWithinMinutes() {
        val a = "2026-10-09T12:00:00Z"
        assertTrue(sameDay(a, "2026-10-09T12:05:00Z"))
        assertFalse(sameDay(a, "2026-10-12T12:00:00Z"))
        assertTrue(withinMinutes(a, "2026-10-09T12:04:00Z", 5))
        assertFalse(withinMinutes(a, "2026-10-09T12:06:00Z", 5))
        assertFalse(sameDay("garbage", a))
    }

    @Test fun convTimeIsRelative() {
        assertEquals("now", formatConvTime(Instant.now().minusSeconds(20).toString()))
        assertEquals("5m", formatConvTime(Instant.now().minusSeconds(5 * 60 + 10L).toString()))
        assertEquals("", formatConvTime("not a time"))
    }

    @Test fun minutesSinceHandlesBadInput() {
        assertNull(minutesSince("nope"))
        assertTrue((minutesSince(Instant.now().minusSeconds(125).toString()) ?: -1L) >= 2)
    }

    @Test fun detectsDeletedPlaceholder() {
        assertTrue(isDeletedMessage("This message was deleted"))
        assertTrue(isDeletedMessage("  this MESSAGE was deleted "))
        assertFalse(isDeletedMessage("hello"))
    }
}
