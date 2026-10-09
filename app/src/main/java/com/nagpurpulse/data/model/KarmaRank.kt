package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * "Top X%" karma position shown on the profile impact card.
 *
 * Returned by the get_my_karma_rank() RPC (supabase/migrations/20261009120000_profile_karma_rank.sql).
 * It only contains aggregate percentages for the calling user, never other users' data.
 */
@Serializable
data class KarmaRank(
    @SerialName("rank_percent")      val rankPercent: Int = 100,
    @SerialName("total_users")       val totalUsers: Int = 0,
    val area: String? = null,
    @SerialName("area_rank_percent") val areaRankPercent: Int? = null,
    @SerialName("area_users")        val areaUsers: Int? = null
)
