// notifications/NotificationActionReceiver.kt
// Handles the buttons on a notification: "Mark as read" and inline "Reply".
package com.nagpurpulse.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.PostRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var messageRepository: MessageRepository
    @Inject lateinit var postRepository: PostRepository

    override fun onReceive(context: Context, intent: Intent) {
        // Hilt injects the fields in the generated super implementation.
        super.onReceive(context, intent)
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    PulseActions.ACTION_MARK_READ -> handleMarkRead(app, intent)
                    PulseActions.ACTION_REPLY -> handleReply(app, intent)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleMarkRead(ctx: Context, intent: Intent) {
        val notifId = intent.getIntExtra(PulseActions.EXTRA_NOTIF_ID, 0)
        val ids = intent.getStringArrayExtra(PulseActions.EXTRA_IDS)?.toList().orEmpty()
        // Dismiss immediately; syncing read state is best-effort.
        if (notifId != 0) ctx.getSystemService(NotificationManager::class.java).cancel(notifId)
        val uid = awaitUserId() ?: return
        if (ids.isNotEmpty()) notificationRepository.setManyRead(uid, ids, true)
    }

    private suspend fun handleReply(ctx: Context, intent: Intent) {
        val notifId = intent.getIntExtra(PulseActions.EXTRA_NOTIF_ID, 0)
        val kind = intent.getStringExtra(PulseActions.EXTRA_KIND) ?: PulseActions.KIND_COMMENT
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(PulseActions.KEY_REPLY_TEXT)?.toString()?.trim().orEmpty()
        if (notifId == 0) return
        if (text.isEmpty()) {
            PulseNotifier.applyReplyResult(ctx, notifId, kind, text, success = false)
            return
        }

        val success = try {
            awaitUserId() ?: throw IllegalStateException("Not signed in")
            if (kind == PulseActions.KIND_MESSAGE) {
                val conversationId = intent.getStringExtra(PulseActions.EXTRA_CONVERSATION_ID)
                    ?: throw IllegalArgumentException("Missing conversation")
                messageRepository.sendMessage(conversationId, text).isSuccess
            } else {
                val postId = intent.getStringExtra(PulseActions.EXTRA_POST_ID)
                    ?: throw IllegalArgumentException("Missing post")
                postRepository.addComment(
                    postId = postId,
                    body = text,
                    isAnonymous = false,
                    parentId = intent.getStringExtra(PulseActions.EXTRA_COMMENT_ID)?.takeIf { it.isNotBlank() }
                ).isSuccess
            }
        } catch (_: Exception) {
            false
        }

        PulseNotifier.applyReplyResult(ctx, notifId, kind, text, success)

        if (success) {
            val uid = authRepository.currentUserId
            val ids = intent.getStringArrayExtra(PulseActions.EXTRA_IDS)?.toList().orEmpty()
            if (uid != null && ids.isNotEmpty()) notificationRepository.setManyRead(uid, ids, true)
        }
    }

    /** The Supabase session restores asynchronously after a cold process start. */
    private suspend fun awaitUserId(): String? {
        repeat(10) {
            authRepository.currentUserId?.let { return it }
            delay(400)
        }
        return null
    }
}
