// notifications/NagpurPulseFcmService.kt  — REPLACE entirely
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

        val title    = message.notification?.title ?: message.data["title"]  ?: "Nagpur Pulse"
        val body     = message.notification?.body  ?: message.data["body"]   ?: ""
        val postId   = message.data["post_id"]
        val type     = message.data["type"] ?: "general"
        val senderId = message.data["sender_id"]

        // ── Check user preference before showing OS notification ──────────────
        if (!NotifPrefsHelper.shouldShowType(applicationContext, type)) {
            // Still write to in-app DB so bell count stays accurate
            CoroutineScope(Dispatchers.IO).launch {
                notificationRepository.writeInAppNotification(title, body, type, postId)
            }
            return
        }

        val channelId = when (type) {
            "alert", "emergency"   -> CHANNEL_ALERTS
            "trending"             -> CHANNEL_TRENDING
            "community"            -> CHANNEL_COMMUNITY
            else                   -> CHANNEL_DIGEST
        }

        // Deep link intent → MainActivity reads post_id and navigates
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            postId?.let { putExtra("post_id", it) }
            putExtra("notification_type", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use the app's custom small icon; fall back to system icon if not present
        val smallIcon = try {
           // R.drawable.ic_notification
            R.mipmap.ic_launcher
        } catch (_: Exception) {
            android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(
                if (type in listOf("alert", "emergency"))
                    NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .setColor(android.graphics.Color.parseColor("#FF6B00"))
            .build()

        NotificationManagerCompat.from(this)
            .notify((System.currentTimeMillis() % 10_000).toInt(), notification)

        // Write to in-app DB with sender info
        CoroutineScope(Dispatchers.IO).launch {
            notificationRepository.writeInAppNotification(title, body, type, postId)
        }
    }
}
