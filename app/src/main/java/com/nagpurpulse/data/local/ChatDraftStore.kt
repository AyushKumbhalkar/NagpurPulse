// java/com/nagpurpulse/data/local/ChatDraftStore.kt

package com.nagpurpulse.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers unsent message text per conversation, so leaving a chat never loses what you were
 * typing and the inbox can show "Draft: …".
 *
 * Drafts are keyed by user id (a second account on the same device never sees them) and live in
 * app-private storage (android:allowBackup is false, so they are not backed up either).
 */
@Singleton
class ChatDraftStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val all = MutableStateFlow(load())

    private fun key(userId: String, conversationId: String) = "$PREFIX$userId:$conversationId"

    private fun load(): Map<String, String> =
        prefs.all.entries.mapNotNull { entry ->
            val key = entry.key
            val value = entry.value
            if (key.startsWith(PREFIX) && value is String && value.isNotBlank()) {
                key to value
            } else {
                null
            }
        }.toMap()

    fun get(userId: String, conversationId: String): String =
        all.value[key(userId, conversationId)].orEmpty()

    /** Blank text clears the draft. */
    fun set(userId: String, conversationId: String, text: String) {
        if (userId.isBlank() || conversationId.isBlank()) return
        val k = key(userId, conversationId)
        val current = all.value
        if (text.isBlank()) {
            if (k !in current) return
            all.value = current - k
            prefs.edit().remove(k).apply()
        } else {
            if (current[k] == text) return
            all.value = current + (k to text)
            prefs.edit().putString(k, text).apply()
        }
    }

    /** Live map of conversationId -> draft for one user (used by the inbox). */
    fun draftsFor(userId: String): Flow<Map<String, String>> {
        val prefix = "$PREFIX$userId:"
        return all.map { map ->
            map.filterKeys { it.startsWith(prefix) }.mapKeys { it.key.removePrefix(prefix) }
        }
    }

    private companion object {
        const val PREFS = "nagpurpulse_chat_drafts"
        const val PREFIX = "draft_"
    }
}
