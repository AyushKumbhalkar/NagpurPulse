// data/repository/NotificationRepository.kt  — REPLACE entirely
package com.nagpurpulse.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.nagpurpulse.data.model.Notification
import com.nagpurpulse.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository
) {

    // ── FCM token ─────────────────────────────────────────────────────────────

/*
    suspend fun saveFcmToken(token: String? = null): Result<Unit> = runCatching {
        val userId   = authRepository.currentUserId ?: return@runCatching
        val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()
        client.postgrest["device_tokens"].upsert(
            mapOf(
                "user_id"    to userId,
                "fcm_token"  to fcmToken,
                "updated_at" to java.time.Instant.now().toString()
            )
        )
    }


 */

    suspend fun saveFcmToken(token: String? = null): Result<Unit> = runCatching {

        android.util.Log.d("FCM_DEBUG", "saveFcmToken() called")

        val userId = authRepository.currentUserId
        android.util.Log.d("FCM_DEBUG", "User ID = $userId")

        if (userId == null) return@runCatching

        val fcmToken = token ?: FirebaseMessaging.getInstance().token.await()

        android.util.Log.d("FCM_DEBUG", "FCM Token = $fcmToken")

        try {

            client.postgrest["device_tokens"].upsert(
                mapOf(
                    "user_id" to userId,
                    "fcm_token" to fcmToken,
                    "updated_at" to java.time.Instant.now().toString()
                )
            ) {
                onConflict = "user_id"
            }

            android.util.Log.d("FCM_DEBUG", "Token saved successfully")

        } catch (e: Exception) {

            android.util.Log.e("FCM_DEBUG", "UPSERT FAILED", e)

            throw e
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

    // ── Create notification with sender info ──────────────────────────────────

    /**
     * Creates a notification for [targetUserId].
     * [senderUserId] is used to look up the sender's username + avatar so the
     * notification row can show a real avatar instead of the generic placeholder.
     */
    suspend fun createNotification(
        targetUserId:   String,
        type:           String,
        title:          String,
        body:           String?,
        relatedPostId:  String?  = null,
        senderUserId:   String?  = null
    ): Result<Unit> = runCatching {
        // Fetch sender profile for avatar display
        val sender: Profile? = senderUserId?.takeIf { it.isNotBlank() }?.let { uid ->
            try {
                client.postgrest["profiles"]
                    .select { filter { eq("id", uid) } }
                    .decodeSingle()
            } catch (_: Exception) { null }
        }

        client.postgrest["notifications"].insert(
            mapOf(
                "user_id"           to targetUserId,
                "type"              to type,
                "title"             to title,
                "body"              to body,
                "is_read"           to false,
                "related_post_id"   to relatedPostId,
                "sender_username"   to sender?.username,
                "sender_avatar_url" to sender?.avatarUrl
            )
        )
    }

    /**
     * Called by FCM service when a push arrives while the app is foregrounded.
     * Writes to the in-app DB so the bell icon stays accurate.
     */
    suspend fun writeInAppNotification(
        title:         String,
        body:          String?,
        type:          String,
        relatedPostId: String? = null
    ): Result<Unit> = runCatching {
        val userId = authRepository.currentUserId ?: return@runCatching
        createNotification(userId, type, title, body, relatedPostId)
    }

    // ── Preference check (used by PostRepository before creating notif) ───────

    suspend fun targetUserWantsNotif(targetUserId: String, field: String): Boolean {
        return try {
            val prefs = client.postgrest["user_preferences"]
                .select { filter { eq("user_id", targetUserId) } }
                .decodeList<com.nagpurpulse.data.model.UserPreferences>()
                .firstOrNull() ?: return true   // default true if no prefs row
            when (field) {
                "notif_replies"  -> prefs.notifReplies
                "notif_upvotes"  -> prefs.notifUpvotes
                "notif_mentions" -> prefs.notifMentions
                "notif_messages" -> prefs.notifMessages
                else             -> true
            }
        } catch (_: Exception) { true }
    }
}
