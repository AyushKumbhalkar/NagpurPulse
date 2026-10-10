// notifications/EngagementTracker.kt
// Device-local record of when the user last opened the app, their daily-visit streak,
// and how many "come back" nudges have gone unanswered. Nothing here leaves the device.
//
// Respect rules baked in (so the nudges stay welcome instead of getting muted):
//   - never nudge someone who already opened the app today
//   - at most 3 nudges per absence, spaced 1 day -> 3 days -> 7 days apart
//   - opening the app resets the count
package com.nagpurpulse.notifications

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

object EngagementTracker {

    private const val PREFS = "nagpur_engagement"
    private const val KEY_LAST_OPEN = "last_open_ms"
    private const val KEY_LAST_DAY = "last_open_epoch_day"
    private const val KEY_STREAK = "streak"
    private const val KEY_NUDGES = "nudges_since_open"
    private const val KEY_LAST_NUDGE = "last_nudge_ms"

    private const val HOUR_MS = 60L * 60 * 1000
    const val MAX_NUDGES_PER_ABSENCE = 3

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Call whenever the app comes to the foreground. */
    fun markOpened(ctx: Context) {
        val p = prefs(ctx)
        val today = LocalDate.now().toEpochDay()
        val lastDay = p.getLong(KEY_LAST_DAY, -1L)
        val streak = p.getInt(KEY_STREAK, 0)
        val newStreak = when (lastDay) {
            today -> streak.coerceAtLeast(1)
            today - 1 -> streak + 1
            else -> 1
        }
        p.edit()
            .putLong(KEY_LAST_OPEN, System.currentTimeMillis())
            .putLong(KEY_LAST_DAY, today)
            .putInt(KEY_STREAK, newStreak)
            .putInt(KEY_NUDGES, 0)
            .apply()
    }

    /** False for installs that have not opened this build yet: nothing to compare against. */
    fun hasHistory(ctx: Context): Boolean = prefs(ctx).getLong(KEY_LAST_DAY, -1L) >= 0L

    fun lastOpenMillis(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST_OPEN, 0L)

    fun openedToday(ctx: Context): Boolean =
        prefs(ctx).getLong(KEY_LAST_DAY, -1L) == LocalDate.now().toEpochDay()

    /** Whole calendar days since the app was last opened (0 = today). */
    fun daysAway(ctx: Context): Int {
        val lastDay = prefs(ctx).getLong(KEY_LAST_DAY, -1L)
        if (lastDay < 0) return 0
        return (LocalDate.now().toEpochDay() - lastDay).toInt().coerceAtLeast(0)
    }

    /** The streak that can still be saved tonight (last open was yesterday), else 0. */
    fun savableStreak(ctx: Context): Int {
        val p = prefs(ctx)
        return if (p.getLong(KEY_LAST_DAY, -1L) == LocalDate.now().toEpochDay() - 1) p.getInt(KEY_STREAK, 0) else 0
    }

    fun nudgesSinceOpen(ctx: Context): Int = prefs(ctx).getInt(KEY_NUDGES, 0)

    /** True when another nudge is allowed right now. */
    fun canNudge(ctx: Context, now: Long = System.currentTimeMillis()): Boolean {
        val sent = nudgesSinceOpen(ctx)
        if (sent >= MAX_NUDGES_PER_ABSENCE) return false
        val requiredGapHours = when (sent) {
            0 -> 20L
            1 -> 72L
            else -> 168L
        }
        return now - prefs(ctx).getLong(KEY_LAST_NUDGE, 0L) >= requiredGapHours * HOUR_MS
    }

    fun recordNudge(ctx: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(ctx)
        p.edit()
            .putInt(KEY_NUDGES, p.getInt(KEY_NUDGES, 0) + 1)
            .putLong(KEY_LAST_NUDGE, now)
            .apply()
    }
}
