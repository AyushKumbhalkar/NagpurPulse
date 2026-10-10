// notifications/ScheduledPushManager.kt
// Channels + the on-device scheduled updates (morning digest, trending, evening
// community, night summary) and the streak / comeback nudge.
//
// Copy rules: no emoji (icons come from PulseStyle), no invented numbers. Every figure
// shown comes from real data; if the data is unavailable the notification is either
// generic-but-honest or skipped.
package com.nagpurpulse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nagpurpulse.R
import com.nagpurpulse.data.repository.EngagementRepository
import com.nagpurpulse.data.repository.PostRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ── Channel IDs (existing ids are unchanged so users keep their per-channel settings) ─
const val CHANNEL_TRENDING  = "nagpur_trending"
const val CHANNEL_ALERTS    = "nagpur_alerts"
const val CHANNEL_COMMUNITY = "nagpur_community"
const val CHANNEL_DIGEST    = "nagpur_digest"
const val CHANNEL_MESSAGES  = "nagpur_messages"
const val CHANNEL_SOCIAL    = "nagpur_social_v2"
const val CHANNEL_MAIN      = "nagpur_pulse_main"
const val CHANNEL_REWARDS   = "nagpur_rewards"
const val CHANNEL_FOR_YOU   = "nagpur_for_you"

private data class ChannelDef(val id: String, val name: String, val description: String, val importance: Int)

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(NotificationManager::class.java)
    val orange = android.graphics.Color.parseColor("#FF6B00")
    listOf(
        ChannelDef(CHANNEL_SOCIAL, "Comments and upvotes",
            "Replies, mentions and upvotes on your posts and comments.", NotificationManager.IMPORTANCE_HIGH),
        ChannelDef(CHANNEL_MESSAGES, "Direct messages",
            "New messages from people in Nagpur.", NotificationManager.IMPORTANCE_HIGH),
        ChannelDef(CHANNEL_ALERTS, "Live city alerts",
            "Safety, traffic and weather alerts near you, plus the nightly summary.", NotificationManager.IMPORTANCE_HIGH),
        ChannelDef(CHANNEL_TRENDING, "Trending in Nagpur",
            "The posts the whole city is talking about.", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(CHANNEL_COMMUNITY, "Community updates",
            "Evening community pulse and nearby activity.", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(CHANNEL_DIGEST, "Daily digest",
            "Your morning briefing of Nagpur's top stories.", NotificationManager.IMPORTANCE_LOW),
        ChannelDef(CHANNEL_REWARDS, "Milestones and streaks",
            "Celebrate when your posts take off and keep your daily streak alive.", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(CHANNEL_FOR_YOU, "For you",
            "Occasional reminders about activity you missed. Never more than a few per absence.", NotificationManager.IMPORTANCE_DEFAULT),
        ChannelDef(CHANNEL_MAIN, "Account and general",
            "Account notices and everything else from Nagpur Pulse.", NotificationManager.IMPORTANCE_HIGH)
    ).forEach { def ->
        // Re-creating an existing channel only refreshes its name/description; the user's
        // sound, importance and other choices are preserved by Android.
        nm.createNotificationChannel(
            NotificationChannel(def.id, def.name, def.importance).apply {
                description = def.description
                enableLights(true)
                lightColor = orange
                if (def.importance >= NotificationManager.IMPORTANCE_DEFAULT) {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 40, 70, 40)
                }
            }
        )
    }
}

private const val MAX_ATTEMPTS = 3

private fun plural(n: Int, one: String, many: String) = if (n == 1) "$n $one" else "$n $many"

private fun trimTitle(title: String, max: Int) =
    title.trim().let { if (it.length <= max) it else it.take(max - 1).trimEnd() + "\u2026" }

// ── Workers — each checks SharedPreferences before doing any work ─────────────

@HiltWorker
class MorningDigestWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository,
    private val engagementRepository: EngagementRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isDigestEnabled(applicationContext)) return Result.success()
        return try {
            val posts = postRepository.getPosts(sortBy = "hot").getOrNull().orEmpty()
            val snap = engagementRepository.snapshot(Instant.now().minusSeconds(12 * 3600L)).getOrNull()
            val name = snap?.username?.takeIf { it.isNotBlank() }
            val top = posts.firstOrNull()

            val title = if (name != null) "Good morning, $name" else "Good morning, Nagpur"
            val earned = (snap?.myNewUpvotes ?: 0) + (snap?.myNewComments ?: 0)
            val body = when {
                snap != null && earned > 0 ->
                    "Overnight your posts picked up ${plural(snap.myNewUpvotes, "upvote", "upvotes")} " +
                        "and ${plural(snap.myNewComments, "comment", "comments")}."
                top != null -> "Top story: ${trimTitle(top.title, 80)}"
                else -> "See what Nagpur is talking about today."
            }
            val lines = posts.take(3).map {
                "${trimTitle(it.title, 56)} \u00B7 ${plural(it.upvotes, "upvote", "upvotes")}"
            }
            PulseNotifier.showLocal(
                applicationContext,
                LocalPulse(1001, "digest", title, body, lines, postId = top?.id?.takeIf { it.isNotBlank() }),
                glyphOverride = R.drawable.ic_notif_morning
            )
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }
}

@HiltWorker
class AfternoonTrendingWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository,
    private val engagementRepository: EngagementRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isTrendingEnabled(applicationContext)) return Result.success()
        return try {
            // Prefer the best post of the last 24h; fall back to the all-round top post.
            val snap = engagementRepository.snapshot(Instant.now().minusSeconds(24 * 3600L)).getOrNull()
            var postId = snap?.topPostId
            var title = snap?.topPostTitle
            var upvotes = snap?.topPostUpvotes ?: 0
            var comments = snap?.topPostComments ?: 0
            if (title.isNullOrBlank()) {
                val hot = postRepository.getPosts(sortBy = "top").getOrNull()?.firstOrNull()
                postId = hot?.id
                title = hot?.title
                upvotes = hot?.upvotes ?: 0
                comments = hot?.commentCount ?: 0
            }
            // Nothing genuinely trending: stay quiet rather than send filler.
            if (title.isNullOrBlank()) return Result.success()

            val stats = buildList {
                if (upvotes > 0) add(plural(upvotes, "upvote", "upvotes"))
                if (comments > 0) add(plural(comments, "comment", "comments"))
            }.joinToString(", ")
            val body = trimTitle(title, 90) + if (stats.isNotEmpty()) "\n$stats so far" else ""
            PulseNotifier.showLocal(
                applicationContext,
                LocalPulse(1002, "trending", "Trending in Nagpur", body, postId = postId?.takeIf { it.isNotBlank() })
            )
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }
}

@HiltWorker
class EveningCommunityWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val engagementRepository: EngagementRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        if (!NotifPrefsHelper.isPushEnabled(applicationContext) ||
            !NotifPrefsHelper.isCommunityEnabled(applicationContext)) return Result.success()

        val snap = engagementRepository.snapshot(Instant.now().minusSeconds(6 * 3600L)).getOrNull()
        val fresh = snap?.newPosts ?: 0
        val (title, body) = if (fresh >= 3) {
            "Nagpur is talking" to "${plural(fresh, "new post", "new posts")} since this afternoon. Catch up on what your neighbours are saying."
        } else {
            "Seen something worth sharing?" to "Nagpur reads what you post. Add today's moment to the feed."
        }
        PulseNotifier.showLocal(applicationContext, LocalPulse(1003, "community", title, body))
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
                .getOrNull()?.filter { it.isAlert }.orEmpty()
            val body = if (alerts.isNotEmpty()) {
                "${plural(alerts.size, "active alert", "active alerts")} across the city. Tap to stay informed."
            } else {
                "All clear across Nagpur tonight. Rest easy."
            }
            PulseNotifier.showLocal(
                applicationContext,
                LocalPulse(1004, "alerts_summary", "Nagpur tonight", body),
                glyphOverride = if (alerts.isEmpty()) R.drawable.ic_notif_night else null
            )
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }
}

/**
 * Evening streak-saver / comeback nudge. See [EngagementTracker] for the respect rules.
 * Sends nothing unless there is something real to say.
 */
@HiltWorker
class ReengagementWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val engagementRepository: EngagementRepository,
    private val postRepository: PostRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!NotifPrefsHelper.isPushEnabled(ctx) || !NotifPrefsHelper.isCommunityEnabled(ctx)) return Result.success()
        if (NotifPrefsHelper.isSilencedNow(ctx)) return Result.success()
        if (!EngagementTracker.hasHistory(ctx) || EngagementTracker.openedToday(ctx)) return Result.success()
        if (!EngagementTracker.canNudge(ctx)) return Result.success()

        val now = System.currentTimeMillis()
        val lastOpen = EngagementTracker.lastOpenMillis(ctx)
        val since = Instant.ofEpochMilli(maxOf(lastOpen, now - 72L * 3600_000L))
        val snap = engagementRepository.snapshot(since).getOrNull() ?: return Result.success()

        val streak = EngagementTracker.savableStreak(ctx)
        val earnedUp = snap.myNewUpvotes
        val earnedCm = snap.myNewComments
        val earned = earnedUp + earnedCm
        val topTitle = snap.topPostTitle?.takeIf { it.isNotBlank() }

        val pulse: LocalPulse? = when {
            streak >= 3 -> LocalPulse(
                1005, "streak",
                "Keep your $streak-day streak",
                if (earned > 0) {
                    "Your posts picked up ${plural(earnedUp, "upvote", "upvotes")} and " +
                        "${plural(earnedCm, "comment", "comments")}. Check in before midnight."
                } else {
                    "Check in before midnight to keep it going."
                }
            )
            earned > 0 -> LocalPulse(
                1005, "nudge",
                "Your posts are getting noticed",
                "${plural(earnedUp, "new upvote", "new upvotes")} and " +
                    "${plural(earnedCm, "comment", "comments")} since you last visited."
            )
            snap.unreadCount > 0 -> LocalPulse(
                1005, "nudge",
                plural(snap.unreadCount, "unread update", "unread updates"),
                topTitle?.let { "Also trending: ${trimTitle(it, 80)}" } ?: "Catch up on what you missed."
            )
            topTitle != null -> LocalPulse(
                1005, "nudge", "Trending in Nagpur", trimTitle(topTitle, 90), postId = snap.topPostId
            )
            else -> null
        } ?: serverNudgePulse()

        if (pulse != null) {
            PulseNotifier.showLocal(ctx, pulse)
            EngagementTracker.recordNudge(ctx, now)
        }
        return Result.success()
    }

    /**
     * Fallback when there is no personal activity to report: ask the server whether to invite the
     * user to post (first post, comeback, reply waiting) with a personalised idea. The server
     * already applies quiet hours and the community-notification preference.
     */
    private suspend fun serverNudgePulse(): LocalPulse? {
        val nudge = postRepository.getPostingNudge() ?: return null
        val title = nudge.title
        val body = nudge.body
        if (!nudge.shouldNudge || title.isNullOrBlank() || body.isNullOrBlank()) return null
        return LocalPulse(1005, "nudge", title, body)
    }
}

// ── Scheduler ─────────────────────────────────────────────────────────────────

object ScheduledPushManager {

    private const val REENGAGE_HOUR = 20
    private const val REENGAGE_MINUTE = 30

    private val WORKER_NAMES = listOf(
        "morning_digest", "afternoon_trend", "evening_comm", "night_summary", "reengage"
    )

    fun schedule(context: Context) {
        createNotificationChannels(context)
        if (!NotifPrefsHelper.isPushEnabled(context)) return
        val wm = WorkManager.getInstance(context)
        fun hm(slot: String) = NotifPrefsHelper.scheduledMinutes(context, slot).let { it / 60 to it % 60 }
        enqueue<MorningDigestWorker>(wm,        "morning_digest",  hm(NotifPrefsHelper.SLOT_MORNING).first,  hm(NotifPrefsHelper.SLOT_MORNING).second)
        enqueue<AfternoonTrendingWorker>(wm,     "afternoon_trend", hm(NotifPrefsHelper.SLOT_TRENDING).first, hm(NotifPrefsHelper.SLOT_TRENDING).second)
        enqueue<EveningCommunityWorker>(wm,      "evening_comm",    hm(NotifPrefsHelper.SLOT_EVENING).first,  hm(NotifPrefsHelper.SLOT_EVENING).second)
        enqueue<NightAlertsSummaryWorker>(wm,    "night_summary",   hm(NotifPrefsHelper.SLOT_NIGHT).first,    hm(NotifPrefsHelper.SLOT_NIGHT).second)
        enqueue<ReengagementWorker>(wm,          "reengage",        REENGAGE_HOUR,                            REENGAGE_MINUTE)
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
