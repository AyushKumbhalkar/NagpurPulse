// java/com/nagpurpulse/ui/components/CountFormat.kt
//
// Pure Kotlin (no Compose imports) so it can be unit tested on the JVM.

package com.nagpurpulse.ui.components

import java.util.Locale

/**
 * Compact counts for feed UI: 0..999 as-is, then 1K, 1.2K, 12K, 1.5M.
 *
 * Locale-independent (always a "." decimal) and never produces "1.0K" or "1000.0K".
 */
fun formatCount(count: Int): String {
    if (count < 1_000) return count.toString()
    val (value, suffix) =
        if (count < 1_000_000) (count / 1_000.0 to "K") else (count / 1_000_000.0 to "M")
    var text = String.format(Locale.US, "%.1f", value)
    if (text.endsWith(".0")) text = text.dropLast(2)
    // 999_950..999_999 round up to "1000" -> that is really 1M.
    if (suffix == "K" && text == "1000") return "1M"
    return text + suffix
}
