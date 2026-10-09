//java/com/nagpurpulse/data/repository/AlertRepository.kt

package com.nagpurpulse.data.repository

import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class AlertRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository
) {

    /**
     * Loads the latest alerts. The category filter is optional: the Alerts screen now loads
     * everything once and filters on the client, so chip switches are instant and every chip
     * can show a live count.
     */
    suspend fun getAlerts(category: String? = null): Result<List<Post>> {
        return try {
            val posts = client.postgrest["posts"].select {
                filter {
                    eq("is_alert", true)
                    if (category != null && category != "all") {
                        eq("category", category)
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<Post>()

            Result.success(enrichWithAuthors(posts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Alert ids (from [postIds]) that the signed-in user has already confirmed. */
    suspend fun getMyConfirmedIds(postIds: List<String>): Set<String> {
        val uid = authRepository.currentUserId ?: return emptySet()
        if (postIds.isEmpty()) return emptySet()
        return try {
            client.postgrest["alert_confirmations"]
                .select(Columns.list("post_id")) {
                    filter {
                        eq("user_id", uid)
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<JsonObject>()
                .mapNotNull { it["post_id"]?.jsonPrimitive?.content }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /** "I can see this too" - one confirmation per user, enforced server-side. */
    suspend fun confirmAlert(postId: String): Result<Unit> {
        return try {
            client.postgrest.rpc(
                "confirm_alert",
                parameters = buildJsonObject { put("p_post_id", postId) }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AlertRepository", "confirm_alert failed for $postId", e)
            Result.failure(e)
        }
    }

    /** Author marks their own alert as resolved ("all clear"). */
    suspend fun resolveAlert(postId: String): Result<Unit> {
        return try {
            client.postgrest.rpc(
                "resolve_alert",
                parameters = buildJsonObject { put("p_post_id", postId) }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AlertRepository", "resolve_alert failed for $postId", e)
            Result.failure(e)
        }
    }

    fun subscribeToAlerts(): Flow<Post> = flow {
        try {
            val channel = client.realtime.channel("alerts-channel")

            val changes = channel.postgresChangeFlow<PostgresAction.Insert>(
                schema = "public"
            ) {
                table = "posts"
            }

            channel.subscribe()

            changes.collect { action ->
                try {
                    val post = Json { ignoreUnknownKeys = true }.decodeFromJsonElement(
                        Post.serializer(),
                        action.record
                    )
                    if (post.isAlert) {
                        emit(enrichWithAuthors(listOf(post)).first())
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
        } catch (e: Exception) {
            // Ignore realtime errors
        }
    }

    // Alerts used to show "u/unknown" because usernames were never joined in.
    private suspend fun enrichWithAuthors(posts: List<Post>): List<Post> {
        val userIds = posts.filter { !it.isAnonymous }.map { it.userId }.distinct()
        if (userIds.isEmpty()) return posts

        val profiles: Map<String, Profile> = try {
            client.postgrest["profiles"].select {
                filter { isIn("id", userIds) }
            }.decodeList<Profile>().associateBy { it.id }
        } catch (_: Exception) {
            emptyMap()
        }

        return posts.map { post ->
            if (post.isAnonymous) {
                post.copy(username = null, isVerified = false, authorAvatarUrl = null)
            } else {
                val profile = profiles[post.userId]
                post.copy(
                    username = profile?.username ?: post.username,
                    isVerified = profile?.isVerified ?: false,
                    authorAvatarUrl = profile?.avatarUrl
                )
            }
        }
    }
}
