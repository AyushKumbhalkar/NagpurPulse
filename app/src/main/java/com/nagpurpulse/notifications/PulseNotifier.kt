// notifications/PulseNotifier.kt
// Builds every notification the user sees in the tray. Replaces the old ad-hoc
// builders in NagpurPulseFcmService and ScheduledPushManager.
//
// What it does for each push:
//   - monochrome status icon + per-type accent colour + sender avatar / glyph tile
//   - collapses upvote / like storms into ONE live-updating notification
//     ("Aarav, Meera and 4 others upvoted your post") instead of N alerts
//   - chat messages use MessagingStyle (conversation thread + inline Reply)
//   - comments / replies / mentions get an inline Reply action
//   - trending posts show their cover image (BigPicture)
//   - stacks into a tidy group with an inbox-style summary
//   - private lock-screen version for messages
package com.nagpurpulse.notifications

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.nagpurpulse.MainActivity
import com.nagpurpulse.R

/** Everything the tray needs to know about one incoming notification. */
internal data class PulsePayload(
    val type: String,
    val title: String,
    val body: String,
    val notificationId: String? = null,
    val postId: String? = null,
    val commentId: String? = null,
    val conversationId: String? = null,
    val senderUsername: String? = null,
    val senderAvatarUrl: String? = null,
    val imageUrl: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/** A notification generated on-device by a scheduled worker (no server row). */
internal data class LocalPulse(
    val id: Int,
    val type: String,
    val title: String,
    val body: String,
    val lines: List<String> = emptyList(),
    val postId: String? = null
)

internal object PulseActions {
    const val ACTION_MARK_READ = "com.nagpurpulse.action.MARK_READ"
    const val ACTION_REPLY = "com.nagpurpulse.action.REPLY"

    const val EXTRA_NOTIF_ID = "pulse_notif_id"
    const val EXTRA_IDS = "pulse_ids"
    const val EXTRA_KIND = "pulse_kind"
    const val EXTRA_POST_ID = "pulse_post_id"
    const val EXTRA_COMMENT_ID = "pulse_comment_id"
    const val EXTRA_CONVERSATION_ID = "pulse_conversation_id"
    const val KEY_REPLY_TEXT = "pulse_reply_text"

    const val KIND_MESSAGE = "message"
    const val KIND_COMMENT = "comment"
}

internal object PulseNotifier {

    private const val EXTRA_NAMES = "pulse_names"
    private const val EXTRA_AGG_IDS = "pulse_agg_ids"

    private val REACTION_TYPES = setOf("upvote", "like", "comment_like")
    private val REPLYABLE_TYPES = setOf("comment", "reply", "mention")

    // ── Public API ────────────────────────────────────────────────────────────

    /** Show (or update) a notification that originated from a server row / FCM push. */
    @SuppressLint("MissingPermission")
    fun show(ctx: Context, p: PulsePayload) {
        val app = ctx.applicationContext
        createNotificationChannels(app)
        val nm = NotificationManagerCompat.from(app)
        if (!nm.areNotificationsEnabled()) return
        val sys = app.getSystemService(NotificationManager::class.java)

        val sender = p.senderUsername?.takeIf { it.isNotBlank() }
        // An upvote row without a sender is the "your post reached N upvotes" milestone.
        val isMilestone = p.type.equals("upvote", ignoreCase = true) && sender == null
        val effectiveType = if (isMilestone) "milestone" else p.type.lowercase()
        val spec = PulseStyle.specFor(effectiveType)

        val cleanTitle = PulseStyle.stripEmoji(p.title).ifBlank { spec.label }
        val body = p.body.trim()

        val isReaction = effectiveType in REACTION_TYPES && sender != null
        val isMessage = effectiveType == "message" && !p.conversationId.isNullOrBlank()

        val key = when {
            isReaction -> "react|$effectiveType|${p.postId}|${p.commentId}"
            isMessage -> "conv|${p.conversationId}"
            else -> p.notificationId
                ?: listOf(effectiveType, p.postId.orEmpty(), p.commentId.orEmpty(), p.conversationId.orEmpty(), cleanTitle, body)
                    .joinToString("|")
        }
        val id = notificationIdFor(key)

        val existing: Notification? = findActive(sys, id)
        val updating = existing != null

        // Names (newest first) and server ids accumulated across updates of one tray entry.
        val prevNames = existing?.extras?.getStringArray(EXTRA_NAMES)?.toList().orEmpty()
        val prevIds = existing?.extras?.getStringArray(EXTRA_AGG_IDS)?.toList().orEmpty()
        val allIds = (prevIds + listOfNotNull(p.notificationId)).distinct().takeLast(50)
        val names = if (sender != null) (listOf(sender) + prevNames).distinct().take(20) else prevNames

        var shownTitle = cleanTitle
        if (isReaction) {
            shownTitle = "${PulseStyle.whoLabel(names)} ${PulseStyle.reactionAction(effectiveType)}"
        } else if (isMessage && sender != null) {
            shownTitle = sender
        }

        // ── Large icon ────────────────────────────────────────────────────────
        val avatar: Bitmap? = if (sender != null && (!isReaction || names.size == 1)) {
            PulseIcons.fetchBitmap(p.senderAvatarUrl, 192)
        } else null
        val largeIcon = if (avatar != null) {
            PulseIcons.avatarWithBadge(app, avatar, spec.glyph, spec.accent)
        } else {
            PulseIcons.glyphTile(app, spec.glyph, spec.accent)
        }

        // ── Builder ───────────────────────────────────────────────────────────
        val extras = Bundle().apply {
            putStringArray(EXTRA_NAMES, names.toTypedArray())
            putStringArray(EXTRA_AGG_IDS, allIds.toTypedArray())
        }

        val builder = NotificationCompat.Builder(app, spec.channelId)
            .setSmallIcon(R.drawable.ic_stat_nagpurpulse)
            .setColor(spec.accent)
            .setLargeIcon(largeIcon)
            .setContentTitle(shownTitle)
            .setContentText(body.ifBlank { spec.label })
            .setSubText(spec.label)
            .setCategory(spec.category)
            .setWhen(p.createdAtMillis)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setPriority(if (spec.highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion(app, spec, if (isMessage) (sender ?: "New message") else shownTitle))
            .setGroup(spec.groupKey)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            // Reaction storms update silently; chat messages and new items always alert.
            .setOnlyAlertOnce(updating && isReaction)
            .setContentIntent(contentIntent(app, id, p, effectiveType, allIds))
            .addExtras(extras)
        if (spec.timeoutMs > 0) builder.setTimeoutAfter(spec.timeoutMs)

        // ── Style ─────────────────────────────────────────────────────────────
        when {
            isMessage -> {
                val me = Person.Builder().setName("You").build()
                val style = existing
                    ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
                    ?: NotificationCompat.MessagingStyle(me)
                val person = Person.Builder()
                    .setName(sender ?: "Someone")
                    .setKey("user:${sender ?: "unknown"}")
                    .apply {
                        avatar?.let { setIcon(IconCompat.createWithBitmap(PulseIcons.circleCrop(it))) }
                    }
                    .build()
                style.addMessage(body.ifBlank { "New message" }, p.createdAtMillis, person)
                builder.setStyle(style)
            }
            !p.imageUrl.isNullOrBlank() && (effectiveType == "trending" || effectiveType == "milestone") -> {
                val picture = PulseIcons.fetchBitmap(p.imageUrl, 720)
                if (picture != null) {
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(picture)
                            .setBigContentTitle(shownTitle)
                            .setSummaryText(body.ifBlank { spec.label })
                    )
                } else {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(body.ifBlank { spec.label }))
                }
            }
            else -> builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(shownTitle)
                    .bigText(body.ifBlank { spec.label })
            )
        }

        // ── Actions ───────────────────────────────────────────────────────────
        val canReply = (isMessage) ||
            (effectiveType in REPLYABLE_TYPES && !p.postId.isNullOrBlank() && sender != null)
        if (canReply) builder.addAction(replyAction(app, id, p, sender, isMessage, allIds))
        if (allIds.isNotEmpty()) builder.addAction(markReadAction(app, id, allIds))

        nm.notify(id, builder.build())
        refreshSummary(app, sys, nm, spec, id, shownTitle)
    }

    /** Show a scheduled / on-device notification (digests, trending, nudges). */
    @SuppressLint("MissingPermission")
    fun showLocal(ctx: Context, l: LocalPulse, glyphOverride: Int? = null) {
        val app = ctx.applicationContext
        if (NotifPrefsHelper.isSilencedNow(app)) return
        createNotificationChannels(app)
        val nm = NotificationManagerCompat.from(app)
        if (!nm.areNotificationsEnabled()) return

        val spec = PulseStyle.specFor(l.type)
        val glyph = glyphOverride ?: spec.glyph
        val title = PulseStyle.stripEmoji(l.title).ifBlank { spec.label }

        val intent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            l.postId?.takeIf { it.isNotBlank() }?.let { putExtra("post_id", it) }
            putExtra("notification_type", l.type)
        }
        val pi = PendingIntent.getActivity(
            app, l.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(app, spec.channelId)
            .setSmallIcon(R.drawable.ic_stat_nagpurpulse)
            .setColor(spec.accent)
            .setLargeIcon(PulseIcons.glyphTile(app, glyph, spec.accent))
            .setContentTitle(title)
            .setContentText(l.body)
            .setSubText(spec.label)
            .setCategory(spec.category)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
        if (spec.timeoutMs > 0) builder.setTimeoutAfter(spec.timeoutMs)

        if (l.lines.size >= 2) {
            val inbox = NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText(spec.label)
            l.lines.take(5).forEach { inbox.addLine(it) }
            builder.setStyle(inbox)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(l.body))
        }
        nm.notify(l.id, builder.build())
    }

    /**
     * Applies the outcome of an inline reply to the live notification. Required by Android:
     * after a RemoteInput the notification must be re-posted or the reply spinner never clears.
     */
    @SuppressLint("MissingPermission")
    fun applyReplyResult(ctx: Context, notifId: Int, kind: String, text: String, success: Boolean) {
        val app = ctx.applicationContext
        val sys = app.getSystemService(NotificationManager::class.java)
        val existing = findActive(sys, notifId) ?: return
        val nm = NotificationManagerCompat.from(app)
        val b = NotificationCompat.Builder.recoverBuilder(app, existing).setOnlyAlertOnce(true)

        if (success && kind == PulseActions.KIND_MESSAGE) {
            val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existing)
            if (style != null) {
                style.addMessage(text, System.currentTimeMillis(), null as Person?)
                b.setStyle(style)
            }
        } else if (success) {
            b.clearActions()
                .setContentTitle("Reply sent")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setTimeoutAfter(4_000L)
        } else {
            b.clearActions()
                .setContentTitle("Reply not sent")
                .setContentText("Tap to open Nagpur Pulse and try again.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Tap to open Nagpur Pulse and try again."))
        }
        nm.notify(notifId, b.build())
    }

    fun notificationIdFor(key: String): Int = (key.hashCode() and 0x7fffffff) or 0x10000

    /** Posts a handful of sample notifications so users can preview the new look. */
    fun showPreview(ctx: Context) {
        val now = System.currentTimeMillis()
        show(ctx, PulsePayload(
            type = "comment", title = "Aarav replied to your comment",
            body = "Sitabuldi traffic is much better after the new signal timing.",
            senderUsername = "Aarav", createdAtMillis = now
        ))
        show(ctx, PulsePayload(
            type = "upvote", title = "Aarav upvoted your post",
            body = "Best chai spots near Dharampeth", senderUsername = "Aarav",
            createdAtMillis = now
        ))
        show(ctx, PulsePayload(
            type = "upvote", title = "Meera upvoted your post",
            body = "Best chai spots near Dharampeth", senderUsername = "Meera",
            createdAtMillis = now
        ))
        show(ctx, PulsePayload(
            type = "trending", title = "Trending now in Nagpur",
            body = "Orange City Marathon registrations are open", createdAtMillis = now
        ))
        show(ctx, PulsePayload(
            type = "upvote", title = "Your post reached 50 upvotes",
            body = "Best chai spots near Dharampeth", createdAtMillis = now
        ))
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private fun findActive(sys: NotificationManager, id: Int): Notification? =
        runCatching { sys.activeNotifications.firstOrNull { it.id == id }?.notification }.getOrNull()

    private fun publicVersion(ctx: Context, spec: PulseSpec, headline: String): Notification =
        NotificationCompat.Builder(ctx, spec.channelId)
            .setSmallIcon(R.drawable.ic_stat_nagpurpulse)
            .setColor(spec.accent)
            .setContentTitle(headline)
            .setContentText(spec.label)
            .build()

    private fun contentIntent(
        ctx: Context, id: Int, p: PulsePayload, type: String, ids: List<String>
    ): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            p.postId?.takeIf { it.isNotBlank() }?.let { putExtra("post_id", it) }
            p.commentId?.takeIf { it.isNotBlank() }?.let { putExtra("comment_id", it) }
            p.conversationId?.takeIf { it.isNotBlank() }?.let { putExtra("conversation_id", it) }
            putExtra("notification_type", type)
            p.notificationId?.let { putExtra("notification_id", it) }
            if (ids.isNotEmpty()) putExtra("notification_ids", ids.toTypedArray())
        }
        return PendingIntent.getActivity(
            ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun markReadAction(ctx: Context, notifId: Int, ids: List<String>): NotificationCompat.Action {
        val intent = Intent(ctx, NotificationActionReceiver::class.java)
            .setAction(PulseActions.ACTION_MARK_READ)
            .putExtra(PulseActions.EXTRA_NOTIF_ID, notifId)
            .putExtra(PulseActions.EXTRA_IDS, ids.toTypedArray())
        val pi = PendingIntent.getBroadcast(
            ctx, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_stat_nagpurpulse, "Mark as read", pi)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }

    private fun replyAction(
        ctx: Context, notifId: Int, p: PulsePayload, sender: String?, isMessage: Boolean, ids: List<String>
    ): NotificationCompat.Action {
        val label = if (isMessage) "Message ${sender ?: ""}".trim() else "Reply to ${sender ?: "comment"}"
        val remoteInput = RemoteInput.Builder(PulseActions.KEY_REPLY_TEXT).setLabel(label).build()
        val intent = Intent(ctx, NotificationActionReceiver::class.java)
            .setAction(PulseActions.ACTION_REPLY)
            .putExtra(PulseActions.EXTRA_NOTIF_ID, notifId)
            .putExtra(PulseActions.EXTRA_KIND, if (isMessage) PulseActions.KIND_MESSAGE else PulseActions.KIND_COMMENT)
            .putExtra(PulseActions.EXTRA_POST_ID, p.postId)
            .putExtra(PulseActions.EXTRA_COMMENT_ID, p.commentId)
            .putExtra(PulseActions.EXTRA_CONVERSATION_ID, p.conversationId)
            .putExtra(PulseActions.EXTRA_IDS, ids.toTypedArray())
        // RemoteInput results are written into the PendingIntent, so it must be mutable on S+.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pi = PendingIntent.getBroadcast(ctx, notifId, intent, flags)
        return NotificationCompat.Action.Builder(R.drawable.ic_stat_nagpurpulse, "Reply", pi)
            .addRemoteInput(remoteInput)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .setAllowGeneratedReplies(true)
            .build()
    }

    /** Keeps the tray tidy: 2+ items in a group collapse under one inbox-style summary. */
    @SuppressLint("MissingPermission")
    private fun refreshSummary(
        ctx: Context,
        sys: NotificationManager,
        nm: NotificationManagerCompat,
        spec: PulseSpec,
        currentId: Int,
        currentTitle: String
    ) {
        val lines = LinkedHashMap<Int, String>()
        runCatching {
            sys.activeNotifications.forEach { sbn ->
                val n = sbn.notification
                val isSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                if (!isSummary && n.group == spec.groupKey) {
                    lines[sbn.id] = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                }
            }
        }
        lines[currentId] = currentTitle
        if (lines.size < 2) return

        val inbox = NotificationCompat.InboxStyle()
            .setBigContentTitle("${lines.size} new updates")
            .setSummaryText("Nagpur Pulse")
        lines.values.toList().asReversed().filter { it.isNotBlank() }.take(5).forEach { inbox.addLine(it) }

        val open = PendingIntent.getActivity(
            ctx, PulseStyle.summaryIdFor(spec.groupKey),
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summary = NotificationCompat.Builder(ctx, spec.channelId)
            .setSmallIcon(R.drawable.ic_stat_nagpurpulse)
            .setColor(spec.accent)
            .setContentTitle("Nagpur Pulse")
            .setContentText("${lines.size} new updates")
            .setStyle(inbox)
            .setGroup(spec.groupKey)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        nm.notify(PulseStyle.summaryIdFor(spec.groupKey), summary)
    }
}
