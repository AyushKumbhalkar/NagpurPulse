// data/repository/NotificationRepository.kt  — REPLACE entirely
package com.nagpurpulse.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.nagpurpulse.data.model.Notification
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.tasks.await
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository
) {

    // ── FCM token ─────────────────────────────────────────────────────────────

    suspend fun saveFcmToken(token: String? = null): Result<Unit> = runCatching {
        val userId = authRepository.currentUserId ?: return@runCatching
        val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()

        client.postgrest["device_tokens"].upsert(
            mapOf(
                "user_id" to userId,
                "fcm_token" to fcmToken,
                "updated_at" to java.time.Instant.now().toString()
            )
        ) {
            onConflict = "user_id"
        }
    }
    suspend fun deleteFcmToken(): Result<Unit> = runCatching {
        val userId = authRepository.currentUserId ?: return@runCatching
        client.postgrest["device_tokens"].delete { filter { eq("user_id", userId) } }
        FirebaseMessaging.getInstance().deleteToken().await()
    }

    // ── Read notifications ────────────────────────────────────────────────────

    suspend fun getNotifications(userId: String): Result<List<Notification>> = runCatching {
        client.postgrest["notifications"].select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(60)
        }.decodeList()
    }

    suspend fun getUnreadCount(userId: String): Result<Int> = runCatching {
        client.postgrest["notifications"].select {
            filter { eq("user_id", userId); eq("is_read", false) }
        }.decodeList<Notification>().size
    }

    // ── Mark read ─────────────────────────────────────────────────────────────

    suspend fun markAllRead(userId: String): Result<Unit> = runCatching {
        client.postgrest["notifications"].update(mapOf("is_read" to true)) {
            filter { eq("user_id", userId) }
        }
    }

    suspend fun markOneRead(notificationId: String): Result<Unit> = runCatching {
        client.postgrest["notifications"].update(mapOf("is_read" to true)) {
            filter { eq("id", notificationId) }
        }
    }

    /**
     * Emits when a notification for this user is inserted in Supabase.
     * The ViewModel reloads the inbox to keep new items and unread counts fresh.
     */
    fun subscribeToNotifications(userId: String): Flow<Unit> = flow {
        val channel = client.realtime.channel("notifications_$userId")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "notifications"
            filter("user_id", FilterOperator.EQ, userId)
        }
        channel.subscribe(blockUntilSubscribed = true)
        inserts.collect { emit(Unit) }
    }

}
