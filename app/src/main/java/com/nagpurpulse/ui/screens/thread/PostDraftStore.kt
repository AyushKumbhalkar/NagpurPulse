//java/com/nagpurpulse/ui/screens/thread/PostDraftStore.kt

package com.nagpurpulse.ui.screens.thread

import android.content.Context
import java.time.LocalDate

/**
 * Tiny SharedPreferences-backed store for:
 *  - the unfinished post draft (so closing the screen never loses work)
 *  - the local posting streak (consecutive days with a post from this device)
 *
 * Drafts are tagged with the user id so a different account never sees someone else's draft.
 */
class PostDraftStore(context: Context) {

    data class Draft(
        val title: String,
        val body: String,
        val category: String,
        val area: String,
        val isAnonymous: Boolean
    ) {
        val hasContent: Boolean get() = title.isNotBlank() || body.isNotBlank()
    }

    private val prefs = context.applicationContext
        .getSharedPreferences("create_post_draft", Context.MODE_PRIVATE)

    fun load(userId: String?): Draft? {
        if (userId == null || prefs.getString(KEY_USER, null) != userId) return null
        val draft = Draft(
            title = prefs.getString(KEY_TITLE, "") ?: "",
            body = prefs.getString(KEY_BODY, "") ?: "",
            category = prefs.getString(KEY_CATEGORY, "community") ?: "community",
            area = prefs.getString(KEY_AREA, "Nagpur") ?: "Nagpur",
            isAnonymous = prefs.getBoolean(KEY_ANON, false)
        )
        return draft.takeIf { it.hasContent }
    }

    fun save(userId: String?, draft: Draft) {
        if (userId == null) return
        if (!draft.hasContent) {
            clear()
            return
        }
        prefs.edit()
            .putString(KEY_USER, userId)
            .putString(KEY_TITLE, draft.title)
            .putString(KEY_BODY, draft.body)
            .putString(KEY_CATEGORY, draft.category)
            .putString(KEY_AREA, draft.area)
            .putBoolean(KEY_ANON, draft.isAnonymous)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_USER).remove(KEY_TITLE).remove(KEY_BODY)
            .remove(KEY_CATEGORY).remove(KEY_AREA).remove(KEY_ANON)
            .apply()
    }

    /** Call once after a successful new post. Returns the updated streak (>= 1). */
    fun recordPost(): Int {
        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong(KEY_LAST_POST_DAY, -1L)
        val current = prefs.getInt(KEY_STREAK, 0)
        val streak = when {
            last == today     -> current.coerceAtLeast(1)
            last == today - 1 -> current + 1
            else              -> 1
        }
        prefs.edit()
            .putLong(KEY_LAST_POST_DAY, today)
            .putInt(KEY_STREAK, streak)
            .apply()
        return streak
    }

    private companion object {
        const val KEY_USER = "user_id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_CATEGORY = "category"
        const val KEY_AREA = "area"
        const val KEY_ANON = "anonymous"
        const val KEY_LAST_POST_DAY = "last_post_day"
        const val KEY_STREAK = "streak"
    }
}
