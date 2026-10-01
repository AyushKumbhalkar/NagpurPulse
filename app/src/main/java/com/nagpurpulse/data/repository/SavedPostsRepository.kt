package com.nagpurpulse.data.repository

import com.nagpurpulse.data.model.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class SavedPost(
    val id: String = "",
    val user_id: String = "",
    val post_id: String = "",
    val created_at: String = ""
)

class SavedPostsRepository @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun savePost(userId: String, postId: String): Result<Unit> {
        return try {
            client.postgrest["saved_posts"].insert(
                mapOf("user_id" to userId, "post_id" to postId)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsavePost(userId: String, postId: String): Result<Unit> {
        return try {
            client.postgrest["saved_posts"].delete {
                filter {
                    eq("user_id", userId)
                    eq("post_id", postId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedPostIds(userId: String): Result<List<String>> {
        return try {
            val saved = client.postgrest["saved_posts"].select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<SavedPost>()
            Result.success(saved.map { it.post_id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isPostSaved(userId: String, postId: String): Result<Boolean> {
        return try {
            val result = client.postgrest["saved_posts"].select {
                filter {
                    eq("user_id", userId)
                    eq("post_id", postId)
                }
            }.decodeList<SavedPost>()
            Result.success(result.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
