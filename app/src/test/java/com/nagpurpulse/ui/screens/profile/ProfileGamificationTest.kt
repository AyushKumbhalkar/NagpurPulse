package com.nagpurpulse.ui.screens.profile

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileGamificationTest {

    private val today = LocalDate.parse("2026-10-09")
    private fun d(s: String) = LocalDate.parse(s)
    private fun streak(vararg days: String) = computeStreak(days.map(::d).toSet(), today)

    // ── Levels ───────────────────────────────────────────────────────────────

    @Test fun levelBoundaries() {
        assertEquals(1, ProfileLevels.forKarma(0).number)
        assertEquals(1, ProfileLevels.forKarma(49).number)
        assertEquals(2, ProfileLevels.forKarma(50).number)
        assertEquals(1, ProfileLevels.forKarma(-30).number)
        assertEquals(5, ProfileLevels.forKarma(99_999).number)
    }

    @Test fun levelProgress() {
        val p = ProfileLevels.progress(150)
        assertEquals(3, p.next?.number)
        assertEquals(100, p.remaining)
        assertEquals(0.5f, p.fraction, 0.001f)
        val top = ProfileLevels.progress(5000)
        assertNull(top.next)
        assertEquals(1f, top.fraction, 0f)
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    @Test fun emptyStreak() = assertEquals(0, streak().days)

    @Test fun consecutiveDaysIncludingToday() {
        assertEquals(3, streak("2026-10-09", "2026-10-08", "2026-10-07").days)
    }

    @Test fun streakAtRiskWhenTodayMissing() {
        val s = streak("2026-10-08", "2026-10-07", "2026-10-06")
        assertEquals(3, s.days)
        assertTrue(s.atRisk)
        assertFalse(s.activeToday)
    }

    @Test fun oneRestDayIsForgiven() {
        assertEquals(3, streak("2026-10-09", "2026-10-07", "2026-10-06").days)
    }

    @Test fun twoGapsInsideSevenDaysBreakTheStreak() {
        assertEquals(2, streak("2026-10-09", "2026-10-07", "2026-10-05").days)
    }

    @Test fun twoMissedDaysInARowBreakTheStreak() {
        assertEquals(0, streak("2026-10-06", "2026-10-05").days)
    }

    @Test fun longestRun() {
        assertEquals(3, streak("2026-10-09", "2026-10-08", "2026-10-01", "2026-09-30", "2026-09-29").longest)
    }

    // ── Activity / heatmap ───────────────────────────────────────────────────

    @Test fun activityParsesSupabaseTimestamps() {
        val a = activityByDay(
            listOf("2026-10-08T10:00:00.123456+00:00", "2026-10-08T12:00:00Z", "2026-10-07T01:00:00", "bad", ""),
            ZoneOffset.UTC
        )
        assertEquals(2, a[d("2026-10-08")])
        assertEquals(1, a[d("2026-10-07")])
        assertEquals(2, a.size)
    }

    @Test fun heatmapShape() {
        val h = buildHeatmap(mapOf(today to 2), today, 16)
        assertEquals(16, h.size)
        assertTrue(h.all { it.size == 7 })
        assertEquals(2, h.last()[today.dayOfWeek.value - 1])
        assertTrue(h.last().drop(today.dayOfWeek.value).all { it == -1 })
    }

    // ── Completeness / impact / milestones ───────────────────────────────────

    @Test fun completeness() {
        val c = computeCompleteness(true, false, true, false, true)
        assertEquals(3, c.doneCount)
        assertEquals(60, c.percent)
        assertEquals("tagline", c.next?.key)
        assertFalse(c.isComplete)
    }

    @Test fun impactSplitsWeekAndTotal() {
        val now = Instant.parse("2026-10-09T12:00:00Z").toEpochMilli()
        val i = computeImpact(
            listOf(
                PostStat("2026-10-08T10:00:00+00:00", 5, 2, 100),
                PostStat("2026-09-01T10:00:00+00:00", 10, 3, 50),
                PostStat("bad", -4, 0, 1)
            ),
            now
        )
        assertEquals(1, i.weekThreads)
        assertEquals(5, i.weekUpvotes)
        assertEquals(100, i.weekViews)
        assertEquals(2, i.weekReplies)
        assertEquals(15, i.totalUpvotes)
        assertEquals(151, i.totalViews)
        assertEquals(5, i.totalReplies)
    }

    @Test fun milestones() {
        val ms = buildMilestones(MilestoneInput(3, 0, 20, 60, 2, false))
        assertEquals(11, ms.size)
        assertTrue(ms.first { it.id == "first_thread" }.earned)
        assertEquals("Post 7 more threads", ms.first { it.id == "storyteller" }.hint)
        assertEquals("Be active 1 more day in a row", ms.first { it.id == "streak_3" }.hint)
        val sorted = sortMilestones(ms)
        assertTrue(sorted.first().earned)
        assertFalse(sorted.last().earned)
    }

    @Test fun nextAction() {
        assertEquals(NextAction.FIRST_THREAD, pickNextAction(0, 0, streak()))
        assertEquals(NextAction.SAVE_STREAK, pickNextAction(2, 1, streak("2026-10-08", "2026-10-07", "2026-10-06")))
        assertEquals(NextAction.FIRST_COMMENT, pickNextAction(2, 0, streak("2026-10-09")))
        assertEquals(NextAction.JOIN_TRENDING, pickNextAction(2, 3, streak("2026-10-09")))
    }
}
