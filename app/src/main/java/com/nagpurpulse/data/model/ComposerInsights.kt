//java/com/nagpurpulse/data/model/ComposerInsights.kt

package com.nagpurpulse.data.model

/**
 * Lightweight, best-effort numbers shown on the Create Post screen.
 * Every field is nullable / empty on failure so the UI can simply hide the related element.
 */
data class ComposerInsights(
    /** Number of posts created today (device local day). */
    val postsToday: Int? = null,
    /** Approximate number of people who follow the selected area. */
    val reachCount: Int? = null,
    /** Most active categories today, most active first. */
    val trendingCategories: List<String> = emptyList()
)
