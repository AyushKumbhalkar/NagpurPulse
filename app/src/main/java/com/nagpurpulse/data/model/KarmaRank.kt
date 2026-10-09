package com.nagpurpulse.data.model

/**
 * Ranking summary displayed on the profile impact card.
 *
 * Percentages are expected to be calculated by the backend/repository; this model
 * only represents the returned values and does not invent ranking data.
 */
data class KarmaRank(
    val rankPercent: Int,
    val areaRankPercent: Int? = null,
    val area: String? = null,
    val areaUsers: Int? = null
)
