// notifications/ScheduledPushManager.kt  — REPLACE entirely
package com.nagpurpulse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nagpurpulse.MainActivity
import com.nagpurpulse.R
import com.nagpurpulse.data.repository.PostRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ── Channel IDs (keep same as original) ──────────────────────────────────────
const val CHANNEL_TRENDING  = "nagpur_trending"
const val CHANNEL_ALERTS    = "nagpur_alerts"
const val CHANNEL_COMMUNITY = "nagpur_community"
const val CHANNEL_DIGEST    = "nagpur_digest"

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(NotificationManager::class.java)
    listOf(
        Triple(CHANNEL_TRENDING,  "Trending in Nagpur",  NotificationManager.IMPORTANCE_DEFAULT),
        Triple(CHANNEL_ALERTS,    "Live Alerts",         NotificationManager.IMPORTANCE_HIGH),
        Triple(CHANNEL_COMMUNITY, "Community Updates",   NotificationManager.IMPORTANCE_DEFAULT),
        Triple(CHANNEL_DIGEST,    "Daily Digest",        NotificationManager.IMPORTANCE_LOW)
    ).forEach { (id, name, importance) ->
        nm.createNotificationChannel(
            NotificationChannel(id, name, importance).apply {
                description  = "NagpurPulse $name"
                enableLights(true)
                lightColor   = android.graphics.Color.parseColor("#FF6B00")
            }
        )
    }
}

// Safe icon helper — uses app icon if available, system fallback otherwise
private fun safeNotifIcon(): Int = try {
  //  R.drawable.ic_notification

    R.mipmap.ic_launcher
} catch (_: Exception) {
    android.R.drawable.ic_dialog_info
}

fun sendLocalNotification(
    context: Context,
    channelId: String,
    notifId: Int,
    title: String,
    body: String
) {
    val pi = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(safeNotifIcon())
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pi)
        .setColor(android.graphics.Color.parseColor("#FF6B00"))
        .build()
    context.getSystemService(NotificationManager::class.java).notify(notifId, notification)
}

// ── Workers — each checks SharedPreferences before doing any work ─────────────

@HiltWorker
class MorningDigestWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isDigestEnabled(applicationContext)) return Result.success()
        return try {
            val posts = postRepository.getPosts(sortBy = "hot").getOrNull() ?: emptyList()
            val top   = posts.take(3).joinToString(" · ") { it.title.take(28) }
            val body  = if (top.isNotBlank()) "Trending: $top" else "See what Nagpur is talking about today!"
            sendLocalNotification(applicationContext, CHANNEL_DIGEST, 1001, "🌅 Good Morning, Nagpur!", body)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

@HiltWorker
class AfternoonTrendingWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isTrendingEnabled(applicationContext)) return Result.success()
        return try {
            val hot  = postRepository.getPosts(sortBy = "top").getOrNull()?.firstOrNull()
            val body = if (hot != null) "\"${hot.title.take(60)}\" — ${hot.upvotes} upvotes"
                       else "Something hot is trending in Nagpur!"
            sendLocalNotification(applicationContext, CHANNEL_TRENDING, 1002, "🔥 Trending in Nagpur", body)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

@HiltWorker
class EveningCommunityWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isCommunityEnabled(applicationContext)) return Result.success()
        val messages = listOf(
            "🌆 Evening, Nagpur! Check what's happening around the city.",
            "💬 New discussions are live. Jump in and share your take!",
            "🏙️ Your Nagpur community is active! Don't miss today's conversations.",
            "⭐ Stay connected with your city every evening."
        )
        sendLocalNotification(applicationContext, CHANNEL_COMMUNITY, 1003, "Evening Update 🌆", messages.random())
        return Result.success()
    }
}

@HiltWorker
class NightAlertsSummaryWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isAlertsSummaryEnabled(applicationContext)) return Result.success()
        return try {
            val alerts = postRepository.getPosts(category = "alerts", sortBy = "new")
                .getOrNull()?.filter { it.isAlert } ?: emptyList()
            val body = if (alerts.isNotEmpty())
                "${alerts.size} active alert${if (alerts.size > 1) "s" else ""} in Nagpur. Stay safe!"
            else "All clear in Nagpur tonight. Stay safe! 🌙"
            sendLocalNotification(applicationContext, CHANNEL_ALERTS, 1004, "🌙 Nagpur Night Summary", body)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}

// ── Scheduler ─────────────────────────────────────────────────────────────────

object ScheduledPushManager {

    private val WORKER_NAMES = listOf(
        "morning_digest", "afternoon_trend", "evening_comm", "night_summary"
    )

    fun schedule(context: Context) {
        createNotificationChannels(context)
        if (!NotifPrefsHelper.isPushEnabled(context)) return
        val wm = WorkManager.getInstance(context)
        enqueue<MorningDigestWorker>(wm,        "morning_digest",  8,  0)
        enqueue<AfternoonTrendingWorker>(wm,     "afternoon_trend", 13, 0)
        enqueue<EveningCommunityWorker>(wm,      "evening_comm",    18, 0)
        enqueue<NightAlertsSummaryWorker>(wm,    "night_summary",   22, 0)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).let { wm ->
            WORKER_NAMES.forEach { wm.cancelUniqueWork(it) }
        }
    }

    /** Call after any preference change — cancels then re-schedules based on current prefs */
    fun reschedule(context: Context) {
        cancelAll(context)
        schedule(context)
    }

    private inline fun <reified W : ListenableWorker> enqueue(
        wm: WorkManager, name: String, hour: Int, minute: Int
    ) {
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE,      minute)
            set(Calendar.SECOND,      0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        wm.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<W>(24, TimeUnit.HOURS)
                .setInitialDelay(target.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(name)
                .build()
        )
    }
}
