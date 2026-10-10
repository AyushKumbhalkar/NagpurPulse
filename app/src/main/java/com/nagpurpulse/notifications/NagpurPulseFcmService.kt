package com.nagpurpulse.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nagpurpulse.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NagpurPulseFcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationRepository: NotificationRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]?.takeIf { it.isNotBlank() } ?: "general"

        // Supabase notifications are the source of truth. Never insert another
        // row from FCM receipt: the database webhook would send it again.
        if (!NotifPrefsHelper.shouldShowType(applicationContext, type)) return

        // The server sends data-only messages so this code styles EVERY push, whether the
        // app is foreground, background or killed. Older servers sent a `notification`
        // block; keep reading it as a fallback.
        val title = data["title"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.title
            ?: "Nagpur Pulse"
        val body = data["body"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.body
            ?: ""

        PulseNotifier.show(
            applicationContext,
            PulsePayload(
                type = type,
                title = title,
                body = body,
                notificationId = data["notification_id"]?.takeIf { it.isNotBlank() },
                postId = data["post_id"]?.takeIf { it.isNotBlank() },
                commentId = data["comment_id"]?.takeIf { it.isNotBlank() },
                conversationId = data["conversation_id"]?.takeIf { it.isNotBlank() },
                senderUsername = data["sender_username"]?.takeIf { it.isNotBlank() },
                senderAvatarUrl = data["sender_avatar_url"]?.takeIf { it.isNotBlank() },
                imageUrl = data["image_url"]?.takeIf { it.isNotBlank() },
                createdAtMillis = parseMillis(data["created_at"])
            )
        )
    }

    private fun parseMillis(value: String?): Long {
        if (value.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
