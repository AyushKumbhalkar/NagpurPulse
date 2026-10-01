//this is the AlertRepository.kt file

//java/com/nagpurpulse/data/repository/AlertRepository.kt

package com.nagpurpulse.data.repository

import com.nagpurpulse.data.model.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AlertRepository @Inject constructor(
    private val client: SupabaseClient
) {

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

            Result.success(posts)

        } catch (e: Exception) {

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

                    val post = Json.decodeFromJsonElement(
                        Post.serializer(),
                        action.record
                    )

                    if (post.isAlert) {
                        emit(post)
                    }

                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }

        } catch (e: Exception) {
            // Ignore realtime errors
        }
    }
}