package com.nagpurpulse.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nagpurpulse.MainActivity
import com.nagpurpulse.R
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

        val title = message.notification?.title ?: message.data["title"] ?: "Nagpur Pulse"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val postId = message.data["post_id"]?.takeIf { it.isNotBlank() }
        val commentId = message.data["comment_id"]?.takeIf { it.isNotBlank() }
        val conversationId = message.data["conversation_id"]?.takeIf { it.isNotBlank() }
        val notificationId = message.data["notification_id"]?.takeIf { it.isNotBlank() }
        val type = message.data["type"] ?: "general"

        // Supabase notifications are the source of truth. Never insert another
        // row from FCM receipt: the database webhook would send it again.
        if (!NotifPrefsHelper.shouldShowType(applicationContext, type)) return

        val channelId = when (type) {
            "alert", "emergency", "alerts_summary" -> CHANNEL_ALERTS
            "trending" -> CHANNEL_TRENDING
            "message" -> CHANNEL_MESSAGES
            "community" -> CHANNEL_COMMUNITY
            else -> CHANNEL_DIGEST
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            postId?.let { putExtra("post_id", it) }
            commentId?.let { putExtra("comment_id", it) }
            conversationId?.let { putExtra("conversation_id", it) }
            putExtra("notification_type", type)
            notificationId?.let { putExtra("notification_id", it) }
        }

        val stableKey = notificationId ?: listOf(type, postId.orEmpty(), commentId.orEmpty(), title, body).joinToString("|")
        val notificationCode = stableKey.hashCode() and 0x7fffffff
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(
                if (type in listOf("alert", "emergency", "alerts_summary")) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setColor(android.graphics.Color.parseColor("#FF6B00"))
            .build()

        NotificationManagerCompat.from(this).notify(notificationCode, notification)
    }
}
