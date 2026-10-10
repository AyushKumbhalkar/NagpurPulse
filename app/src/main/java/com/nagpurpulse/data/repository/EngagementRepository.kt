// data/repository/EngagementRepository.kt
// One cheap RPC that returns the numbers used to personalise scheduled notifications.
// Only aggregates for the signed-in user; see supabase migration
// 20261011120000_push_engagement_and_device_tokens.sql.
package com.nagpurpulse.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class EngagementSnapshot(
    val username: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("my_new_upvotes") val myNewUpvotes: Int = 0,
    @SerialName("my_new_comments") val myNewComments: Int = 0,
    @SerialName("new_posts") val newPosts: Int = 0,
    @SerialName("active_alerts") val activeAlerts: Int = 0,
    @SerialName("top_post_id") val topPostId: String? = null,
    @SerialName("top_post_title") val topPostTitle: String? = null,
    @SerialName("top_post_upvotes") val topPostUpvotes: Int = 0,
    @SerialName("top_post_comments") val topPostComments: Int = 0
)

@Singleton
class EngagementRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository
) {
    /**
     * @param since start of the window for "new since" counts (upvotes/comments on the user's
     * posts, new posts in the city). Fails if signed out or the RPC is not deployed yet;
     * callers fall back to generic copy.
     */
    suspend fun snapshot(since: Instant): Result<EngagementSnapshot> = runCatching {
        // After a cold start the stored session is restored asynchronously.
        var tries = 0
        while (authRepository.currentUserId == null && tries < 6) {
            delay(500)
            tries++
        }
        if (authRepository.currentUserId == null) throw IllegalStateException("Not signed in")
        client.postgrest.rpc(
            "get_engagement_snapshot",
            buildJsonObject { put("p_since", since.toString()) }
        ).decodeAs<EngagementSnapshot>()
    }
}
