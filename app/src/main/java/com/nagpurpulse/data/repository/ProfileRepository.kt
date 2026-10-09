package com.nagpurpulse.data.repository

import com.nagpurpulse.data.model.Badge
import com.nagpurpulse.data.model.KarmaRank
import com.nagpurpulse.data.model.Notification
import com.nagpurpulse.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import kotlinx.serialization.json.put

class ProfileRepository @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun getProfile(userId: String): Result<Profile> {
        return try {
            val profile = client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        tagline: String? = null,
        avatarUrl: String? = null,
        areas: List<String>? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            tagline?.let { updates["tagline"] = it }
            avatarUrl?.let { updates["avatar_url"] = it }
            areas?.let { updates["areas"] = it }
            client.postgrest["profiles"].update(updates) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAreas(userId: String, areas: List<String>): Result<Unit> {
        return try {
            client.postgrest["profiles"].update(
                mapOf("areas" to areas)
            ) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBadges(userId: String): Result<List<Badge>> {
        return try {
            val badges = client.postgrest["badges"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<Badge>()
            Result.success(badges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotifications(userId: String): Result<List<Notification>> {
        return try {
            val notifications = client.postgrest["notifications"].select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<Notification>()
            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(userId: String): Result<Unit> {
        return try {
            client.postgrest["notifications"].update(
                mapOf("is_read" to true)
            ) {
                filter { eq("user_id", userId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentCount(userId: String): Result<Int> {
        return try {
            // Server-side count: other people's anonymous comments are never counted.
            val count = client.postgrest.rpc(
                "get_user_comment_count",
                kotlinx.serialization.json.buildJsonObject { put("p_user_id", userId) }
            ).decodeAs<Long>().toInt()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * "Top X%" position by karma, overall and inside the user's first area.
     * Returns null (never throws) if the RPC is not deployed yet or the user has no rank,
     * so the UI can simply hide the rank row.
     */
    suspend fun getKarmaRank(): KarmaRank? {
        return try {
            client.postgrest
                .rpc("get_my_karma_rank", kotlinx.serialization.json.buildJsonObject { })
                .decodeList<KarmaRank>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
