// data/repository/NotificationRepository.kt  — REPLACE entirely
package com.nagpurpulse.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.nagpurpulse.data.model.Notification
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

        // Preferred path: a SECURITY DEFINER RPC that also re-homes a token that still
        // belongs to a previous account on this phone (RLS hides that row from a plain upsert).
        val viaRpc = runCatching {
            client.postgrest.rpc(
                "register_device_token",
                buildJsonObject { put("p_token", fcmToken) }
            )
        }
        if (viaRpc.isFailure) {
            client.postgrest["device_tokens"].upsert(
                mapOf(
                    "user_id" to userId,
                    "fcm_token" to fcmToken,
                    "updated_at" to java.time.Instant.now().toString()
                )
            ) {
                onConflict = "fcm_token"
            }
        }
    }
    suspend fun deleteFcmToken(): Result<Unit> = runCatching {
        val userId = authRepository.currentUserId ?: return@runCatching
        val fcmToken = FirebaseMessaging.getInstance().token.await()
        // Delete only this installation's token; preserve push delivery on other devices.
        client.postgrest["device_tokens"].delete {
            filter {
                eq("user_id", userId)
                eq("fcm_token", fcmToken)
            }
        }
        FirebaseMessaging.getInstance().deleteToken().await()
    }

    /**
     * Increment privacy-preserving daily aggregate analytics. The database stores
     * event/type counters only; no account, notification, post, or device ID is sent.
     * Analytics are best-effort and must never block core notification behavior.
     */
    suspend fun trackAnalytics(eventType: String, notificationType: String = "all") {
        runCatching {
            if (authRepository.currentUserId == null) return@runCatching
            client.postgrest.rpc(
                "record_notification_analytics",
                parameters = buildJsonObject {
                    put("p_event_type", eventType)
                    put("p_notification_type", notificationType.lowercase())
                }
            )
        }.onFailure { error ->
            android.util.Log.d("NotificationAnalytics", "Aggregate event skipped: ${error.message}")
        }
    }

    // ── Read notifications ────────────────────────────────────────────────────

    suspend fun getNotifications(userId: String, limit: Int = 60): Result<List<Notification>> = runCatching {
        client.postgrest["notifications"].select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
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

    suspend fun markOneRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        client.postgrest["notifications"].update(mapOf("is_read" to true)) {
            filter {
                eq("user_id", userId)
                eq("id", notificationId)
            }
        }
    }

    /** Batch read/unread update — one request for a whole grouped notification. */
    suspend fun setManyRead(userId: String, ids: List<String>, read: Boolean): Result<Unit> = runCatching {
        if (ids.isEmpty()) return@runCatching
        client.postgrest["notifications"].update(mapOf("is_read" to read)) {
            filter {
                eq("user_id", userId)
                isIn("id", ids)
            }
        }
    }

    /** Batch delete used by swipe-to-delete (committed after the undo window). */
    suspend fun deleteMany(userId: String, ids: List<String>): Result<Unit> = runCatching {
        if (ids.isEmpty()) return@runCatching
        client.postgrest["notifications"].delete {
            filter {
                eq("user_id", userId)
                isIn("id", ids)
            }
        }
    }

    /**
     * Permanently removes the signed-in user's notifications.
     * This is intentionally separate from markAllRead so the UI cannot
     * accidentally describe a read operation as a clear operation.
     */
    suspend fun deleteAll(userId: String): Result<Unit> = runCatching {
        client.postgrest["notifications"].delete {
            filter { eq("user_id", userId) }
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
