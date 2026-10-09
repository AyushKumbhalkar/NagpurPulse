// java/com/nagpurpulse/ui/components/Festival.kt
package com.nagpurpulse.ui.components

import androidx.annotation.StringRes
import com.nagpurpulse.R
import java.time.LocalDate
import java.time.MonthDay

data class FestivalGreeting(val emoji: String, @StringRes val messageRes: Int)

/**
 * A warm, one-line seasonal greeting for the Home screen.
 *
 * Fixed-date days work every year. Festivals that follow the lunar calendar are listed per
 * year in [movable] and MUST be refreshed every year (outside a listed year nothing is shown).
 * Please double-check these dates against the official calendar before each release.
 */
object Festivals {

    private data class Window(val start: LocalDate, val end: LocalDate, val greeting: FestivalGreeting)

    private val movable: List<Window> = listOf(
        // 2026 — verify against the official calendar
        Window(LocalDate.of(2026, 3, 19), LocalDate.of(2026, 3, 19), FestivalGreeting("🎏", R.string.festival_gudi_padwa)),
        Window(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 24), FestivalGreeting("🐘", R.string.festival_ganeshotsav)),
        Window(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 19), FestivalGreeting("🪔", R.string.festival_navratri)),
        Window(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 20), FestivalGreeting("🏹", R.string.festival_dussehra)),
        Window(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 11), FestivalGreeting("🪔", R.string.festival_diwali)),
    )

    private val fixed: Map<MonthDay, FestivalGreeting> = mapOf(
        MonthDay.of(1, 26) to FestivalGreeting("🇮🇳", R.string.festival_republic_day),
        MonthDay.of(5, 1) to FestivalGreeting("🌅", R.string.festival_maharashtra_day),
        MonthDay.of(8, 15) to FestivalGreeting("🇮🇳", R.string.festival_independence_day),
    )

    fun today(date: LocalDate = LocalDate.now()): FestivalGreeting? {
        movable.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }?.let { return it.greeting }
        return fixed[MonthDay.from(date)]
    }
}
