package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stores a persistent anonymous alias for a user within a specific post's comments.
 * e.g. user ABC in post XYZ always appears as "Anon_Tiger_4" in that thread.
 * Different post → different random alias.
 */
@Serializable
data class AnonAlias(
    val id: String = "",
    @SerialName("post_id")   val postId: String = "",
    @SerialName("user_id")   val userId: String = "",
    @SerialName("alias")     val alias: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

// ── Deterministic alias generator ─────────────────────────────────────────────
// Combines post_id + user_id hash → picks animal + adjective + number
// Same inputs always produce the same alias; no DB needed if you prefer pure-client.

private val ADJECTIVES = listOf(
    "Brave","Swift","Silent","Wild","Calm","Mystic","Clever","Bold",
    "Fierce","Gentle","Sneaky","Happy","Dark","Cosmic","Chill","Sharp",
    "Fuzzy","Lucky","Quirky","Sunny","Shadow","Nimble","Ancient","Cozy"
)
private val ANIMALS = listOf(
    "Tiger","Wolf","Eagle","Fox","Bear","Panda","Hawk","Lion","Owl","Deer",
    "Lynx","Crow","Seal","Raven","Mink","Viper","Cobra","Gecko","Otter","Bison",
    "Moose","Crane","Finch","Manta","Dingo","Hyena","Lemur","Tapir","Quail","Ibis"
)

fun generateAnonAlias(postId: String, userId: String): String {
    // Stable hash of postId + userId
    val seed = (postId + userId).fold(17L) { acc, c -> acc * 31 + c.code }
    val adjIdx    = ((seed ushr 0) % ADJECTIVES.size).toInt().let { if (it < 0) it + ADJECTIVES.size else it }
    val animalIdx = ((seed ushr 8) % ANIMALS.size).toInt().let { if (it < 0) it + ANIMALS.size else it }
    val num       = ((seed ushr 16) % 99 + 1).toInt().let { if (it < 0) it + 99 else it }
    return "Anon_${ADJECTIVES[adjIdx]}_${ANIMALS[animalIdx]}_$num"
}
