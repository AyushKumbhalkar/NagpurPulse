// data/repository/UserPreferencesRepository.kt  — REPLACE entirely
package com.nagpurpulse.data.repository

import android.content.Context
import com.nagpurpulse.data.model.UserPreferences
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.preferences.PreferenceManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository
) {

    // ── Core fetch ────────────────────────────────────────────────────────────

    suspend fun getPreferences(): Result<UserPreferences> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("Not logged in"))
            val prefs = client.postgrest["user_preferences"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingle<UserPreferences>()
            Result.success(prefs)
        } catch (_: Exception) {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("Not logged in"))
            Result.success(UserPreferences(userId = userId))
        }
    }

    // ── Display preferences ───────────────────────────────────────────────────

    suspend fun saveLanguage(language: String)     = updateField("language",        language)
    suspend fun saveTextSize(textSize: String)     = updateField("text_size",       textSize)
    suspend fun saveAmoledMode(enabled: Boolean)   = updateField("amoled_mode",     enabled)

    suspend fun getAmoledMode(): Boolean = try {
        getPreferences().getOrNull()?.amoledMode ?: true
    } catch (_: Exception) {
        true
    }

    fun getSavedTheme(context: Context): Boolean {
        return context
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .getBoolean("amoled_mode", true)
    }

    fun saveThemeLocally(
        context: Context,
        enabled: Boolean
    ) {
        context
            .getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("amoled_mode", enabled)
            .apply()
    }

    suspend fun saveTextSizeAndApply(size: String): Result<Unit> {
        PreferenceManager.updateTextSize(size)
        return saveTextSize(size)
    }

    suspend fun saveDisplayDensity(density: String): Result<Unit> {
        DensityManager.density = density
        return updateField("display_density", density)
    }

    suspend fun saveFeedStyle(style: String): Result<Unit> {
        FeedLayoutManager.feedStyle = style
        return updateField("feed_style", style)
    }

    suspend fun getTextSize(): String = try {
        getPreferences().getOrNull()?.textSize ?: "medium"
    } catch (_: Exception) { "medium" }

    suspend fun getDisplayDensity(): String = try {
        getPreferences().getOrNull()?.displayDensity ?: "comfortable"
    } catch (_: Exception) { "comfortable" }

    suspend fun getFeedStyle(): String = try {
        // Keep the default consistent with FeedLayoutManager and the intended
        // initial home-feed layout. A missing preference must not switch the
        // post text to compact mode when the Settings ViewModel loads.
        getPreferences().getOrNull()?.feedStyle
            ?.takeIf { it == "compact" || it == "expanded" }
            ?: "expanded"
    } catch (_: Exception) {
        "expanded"
    }

    // ── Notification preferences ──────────────────────────────────────────────

    /** Save a single boolean notif preference to Supabase + sync SharedPreferences */
    suspend fun saveNotifPref(ctx: Context, field: String, value: Boolean): Result<Unit> {
        NotifPrefsHelper.save(ctx, field, value)
        return updateField(field, value)
    }

    /** Load all notif prefs from Supabase and sync to SharedPreferences */
    suspend fun loadAndSyncNotifPrefs(ctx: Context): UserPreferences? {
        return try {
            val prefs = getPreferences().getOrNull() ?: return null
            NotifPrefsHelper.syncFromUserPreferences(ctx, prefs)
            prefs
        } catch (_: Exception) { null }
    }

    /** Mark the push-enable banner as permanently dismissed */
    suspend fun dismissNotifBanner(ctx: Context): Result<Unit> {
        NotifPrefsHelper.save(ctx, "notif_banner_dismissed", true)
        return updateField("notif_banner_dismissed", true)
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun updateField(field: String, value: Any): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("Not logged in"))
            val body = buildJsonObject {
                put("user_id", userId)
                when (value) {
                    is String  -> put(field, value)
                    is Boolean -> put(field, value)
                }
            }
            client.postgrest["user_preferences"].upsert(body)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



}
