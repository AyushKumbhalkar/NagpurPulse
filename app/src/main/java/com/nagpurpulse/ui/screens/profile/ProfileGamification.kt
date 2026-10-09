// java/com/nagpurpulse/ui/screens/profile/ProfileGamification.kt
//
// Pure Kotlin (no Compose / Android imports) so it can be unit tested on the JVM.
// Everything here is derived from data the app already has: karma, the user's
// posts and the user's comments. No new backend is required.

package com.nagpurpulse.ui.screens.profile

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// ── Levels ────────────────────────────────────────────────────────────────────

data class ProfileLevel(
    val number: Int,
    val title: String,
    val emoji: String,
    val minKarma: Int,
    /** What reaching this level unlocks (shown in the levels dialog). */
    val unlock: String,
    /** ARGB colours; converted to Compose Color in the UI layer. */
    val colorStart: Long,
    val colorEnd: Long
)

data class LevelProgress(
    val level: ProfileLevel,
    val next: ProfileLevel?,
    /** 0f..1f progress toward [next]; 1f when the user is at the top level. */
    val fraction: Float,
    /** Karma still needed for [next]; 0 at the top level. */
    val remaining: Int
)

object ProfileLevels {
    val all: List<ProfileLevel> = listOf(
        ProfileLevel(1, "Newcomer", "🌱", 0, "Starter banner & ring", 0xFF8E8E93, 0xFFC7C7CC),
        ProfileLevel(2, "Local", "📍", 50, "Sunset banner & orange ring", 0xFFFF6B00, 0xFFFF8A30),
        ProfileLevel(3, "Insider", "🔥", 250, "Flame banner & ring", 0xFFFF3D00, 0xFFFFB300),
        ProfileLevel(4, "Trusted Voice", "💎", 750, "Aurora banner & ring", 0xFF0A84FF, 0xFF5AC8FA),
        ProfileLevel(5, "City Legend", "👑", 2000, "Royal gold banner & ring", 0xFFFFB300, 0xFF7C3AED)
    )

    fun forKarma(karma: Int): ProfileLevel {
        val k = karma.coerceAtLeast(0)
        return all.last { k >= it.minKarma }
    }

    fun progress(karma: Int): LevelProgress {
        val k = karma.coerceAtLeast(0)
        val level = forKarma(k)
        val next = all.getOrNull(level.number) // numbers are 1-based, so index == next level
        if (next == null) return LevelProgress(level, null, 1f, 0)
        val span = (next.minKarma - level.minKarma).coerceAtLeast(1)
        val fraction = ((k - level.minKarma).toFloat() / span).coerceIn(0f, 1f)
        return LevelProgress(level, next, fraction, (next.minKarma - k).coerceAtLeast(0))
    }
}

// ── Dates / activity ──────────────────────────────────────────────────────────

internal fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: Exception) {
        try {
            Instant.parse(value)
        } catch (_: Exception) {
            try {
                // Timestamp without an offset: Supabase stores UTC.
                LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** Number of contributions (threads + comments) per local calendar day. */
fun activityByDay(
    timestamps: List<String>,
    zone: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, Int> {
    val result = HashMap<LocalDate, Int>()
    for (ts in timestamps) {
        val day = parseInstant(ts)?.atZone(zone)?.toLocalDate() ?: continue
        result[day] = (result[day] ?: 0) + 1
    }
    return result
}

// ── Streak ────────────────────────────────────────────────────────────────────

data class StreakInfo(
    /** Current streak length in days (rest days are not counted). */
    val days: Int,
    val activeToday: Boolean,
    /** True when the streak is alive but today's contribution is still missing. */
    val atRisk: Boolean,
    /** True when a weekly rest day is currently keeping the streak alive. */
    val restDayUsed: Boolean,
    /** Longest strict run of consecutive active days ever. */
    val longest: Int
)

/**
 * A streak counts consecutive days with at least one thread or comment.
 * To keep it friendly, ONE missed day per rolling 7 days is forgiven (a "rest day"),
 * as long as the user was active the day before it.
 * Today is never counted as a miss because the day is not over yet.
 */
fun computeStreak(activeDays: Set<LocalDate>, today: LocalDate): StreakInfo {
    val longest = longestRun(activeDays)
    if (activeDays.isEmpty()) return StreakInfo(0, false, false, false, longest)

    val activeToday = today in activeDays
    var day = if (activeToday) today else today.minusDays(1)
    var streak = 0
    var lastRestDay: LocalDate? = null

    while (true) {
        if (day in activeDays) {
            streak++
            day = day.minusDays(1)
            continue
        }
        val previousActive = day.minusDays(1) in activeDays
        val restAvailable = lastRestDay == null ||
                ChronoUnit.DAYS.between(day, lastRestDay) >= 7
        if (previousActive && restAvailable) {
            lastRestDay = day
            day = day.minusDays(1)
            continue
        }
        break
    }

    val restKeepingAlive = lastRestDay != null &&
            ChronoUnit.DAYS.between(lastRestDay, today) <= 1
    return StreakInfo(
        days = streak,
        activeToday = activeToday,
        atRisk = !activeToday && streak > 0,
        restDayUsed = restKeepingAlive,
        longest = longest
    )
}

fun longestRun(activeDays: Set<LocalDate>): Int {
    if (activeDays.isEmpty()) return 0
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (d in activeDays.sorted()) {
        run = if (previous != null && previous.plusDays(1) == d) run + 1 else 1
        if (run > best) best = run
        previous = d
    }
    return best
}

// ── Heatmap ───────────────────────────────────────────────────────────────────

/**
 * Grid of [weeks] columns (Mon..Sun rows). Entry is the contribution count for the day,
 * or -1 for days in the future (so the UI can skip them).
 */
fun buildHeatmap(
    counts: Map<LocalDate, Int>,
    today: LocalDate,
    weeks: Int = 16
): List<List<Int>> {
    val start = today
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .minusWeeks((weeks - 1).toLong())
    return (0 until weeks).map { col ->
        (0 until 7).map { row ->
            val date = start.plusDays((col * 7 + row).toLong())
            if (date.isAfter(today)) -1 else (counts[date] ?: 0)
        }
    }
}

// ── Profile completeness ──────────────────────────────────────────────────────

data class CompletenessStep(val key: String, val label: String, val done: Boolean)

data class Completeness(val steps: List<CompletenessStep>) {
    val doneCount: Int get() = steps.count { it.done }
    val total: Int get() = steps.size
    val fraction: Float get() = if (total == 0) 1f else doneCount.toFloat() / total
    val percent: Int get() = (fraction * 100f).toInt()
    val isComplete: Boolean get() = doneCount == total
    val next: CompletenessStep? get() = steps.firstOrNull { !it.done }
}

fun computeCompleteness(
    hasAvatar: Boolean,
    hasTagline: Boolean,
    hasLocation: Boolean,
    hasAreas: Boolean,
    hasThread: Boolean
): Completeness = Completeness(
    listOf(
        CompletenessStep("avatar", "Add a profile photo", hasAvatar),
        CompletenessStep("tagline", "Write a one-line tagline", hasTagline),
        CompletenessStep("location", "Add your location", hasLocation),
        CompletenessStep("areas", "Pick your Nagpur areas", hasAreas),
        CompletenessStep("thread", "Post your first thread", hasThread)
    )
)

// ── Impact ────────────────────────────────────────────────────────────────────

data class PostStat(
    val createdAt: String,
    val upvotes: Int,
    val replies: Int,
    val views: Int
)

data class ImpactStats(
    val weekThreads: Int,
    val weekViews: Int,
    val weekUpvotes: Int,
    val weekReplies: Int,
    val totalViews: Int,
    val totalUpvotes: Int,
    val totalReplies: Int
) {
    val hasAnything: Boolean get() = totalViews > 0 || totalUpvotes > 0 || totalReplies > 0
}

/** "This week" = threads created in the last 7 days, with their lifetime counters. */
fun computeImpact(posts: List<PostStat>, nowMillis: Long = System.currentTimeMillis()): ImpactStats {
    val weekAgo = nowMillis - 7L * 24 * 60 * 60 * 1000
    var wT = 0; var wV = 0; var wU = 0; var wR = 0
    var tV = 0; var tU = 0; var tR = 0
    for (p in posts) {
        val up = p.upvotes.coerceAtLeast(0)
        tV += p.views; tU += up; tR += p.replies
        val created = parseInstant(p.createdAt)?.toEpochMilli() ?: continue
        if (created >= weekAgo) {
            wT++; wV += p.views; wU += up; wR += p.replies
        }
    }
    return ImpactStats(wT, wV, wU, wR, tV, tU, tR)
}

// ── Milestones (client-side badges with progress) ─────────────────────────────

data class Milestone(
    val id: String,
    val emoji: String,
    val title: String,
    /** What the user needs to do next, e.g. "Post 2 more threads". */
    val hint: String,
    val current: Int,
    val target: Int
) {
    val earned: Boolean get() = current >= target
    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

data class MilestoneInput(
    val threads: Int,
    val comments: Int,
    val upvotesReceived: Int,
    val karma: Int,
    val longestStreak: Int,
    val profileComplete: Boolean
)

private fun plural(n: Int, one: String, many: String = one + "s"): String =
    "$n ${if (n == 1) one else many}"

fun buildMilestones(i: MilestoneInput): List<Milestone> {
    fun left(target: Int, current: Int) = (target - current).coerceAtLeast(0)
    return listOf(
        Milestone("first_thread", "🧵", "First Thread", "Post your first thread", i.threads, 1),
        Milestone("first_comment", "💬", "Conversation Starter", "Write your first comment", i.comments, 1),
        Milestone(
            "streak_3", "🔥", "3-Day Streak",
            "Be active ${plural(left(3, i.longestStreak), "more day")} in a row", i.longestStreak, 3
        ),
        Milestone(
            "storyteller", "📖", "Storyteller",
            "Post ${plural(left(10, i.threads), "more thread")}", i.threads, 10
        ),
        Milestone(
            "crowd_pleaser", "👏", "Crowd Pleaser",
            "Earn ${left(50, i.upvotesReceived)} more upvotes", i.upvotesReceived, 50
        ),
        Milestone(
            "karma_100", "⭐", "Rising Star",
            "Earn ${left(100, i.karma)} more karma", i.karma, 100
        ),
        Milestone(
            "streak_7", "⚡", "Week Warrior",
            "Be active ${plural(left(7, i.longestStreak), "more day")} in a row", i.longestStreak, 7
        ),
        Milestone(
            "regular_voice", "🗣", "Regular Voice",
            "Write ${plural(left(25, i.comments), "more comment")}", i.comments, 25
        ),
        Milestone(
            "profile_pro", "✨", "Profile Pro",
            "Complete your profile", if (i.profileComplete) 1 else 0, 1
        ),
        Milestone(
            "karma_500", "🦸", "Local Hero",
            "Earn ${left(500, i.karma)} more karma", i.karma, 500
        ),
        Milestone(
            "streak_30", "🏆", "Unstoppable",
            "Be active ${plural(left(30, i.longestStreak), "more day")} in a row", i.longestStreak, 30
        )
    )
}

/** Earned first, then closest-to-done first. */
fun sortMilestones(list: List<Milestone>): List<Milestone> =
    list.sortedWith(compareByDescending<Milestone> { it.earned }.thenByDescending { it.fraction })

// ── Next best action ──────────────────────────────────────────────────────────

enum class NextAction { FIRST_THREAD, SAVE_STREAK, FIRST_COMMENT, JOIN_TRENDING }

fun pickNextAction(threads: Int, comments: Int, streak: StreakInfo): NextAction = when {
    threads == 0 -> NextAction.FIRST_THREAD
    streak.atRisk && streak.days >= 2 -> NextAction.SAVE_STREAK
    comments == 0 -> NextAction.FIRST_COMMENT
    else -> NextAction.JOIN_TRENDING
}
