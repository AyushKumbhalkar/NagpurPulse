package com.nagpurpulse.ui.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language choice (English / Hindi / Marathi).
 *
 * - Android 13+ (API 33): uses the system per-app language API, so the choice also
 *   shows up in system Settings and the system recreates the activity for us.
 * - Older phones: the choice is stored in SharedPreferences and applied by wrapping
 *   the base context in [wrap] (called from MainActivity and the Application class).
 */
object AppLocale {
    data class Option(val tag: String, val nativeName: String)

    val options = listOf(
        Option("en", "English"),
        Option("hi", "हिन्दी"),
        Option("mr", "मराठी")
    )

    private const val PREFS = "app_locale_prefs"
    private const val KEY_TAG = "language_tag"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Language tag currently in use ("en", "hi" or "mr"); falls back to English. */
    fun currentTag(context: Context): String {
        val active = context.resources.configuration.locales[0].language
        return options.firstOrNull { it.tag == active }?.tag ?: "en"
    }

    /** Applies a saved choice on phones older than Android 13. No-op on Android 13+. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY_TAG, "").orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** Switches the app language and restarts the current screen to show it. */
    fun apply(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(tag)
            return
        }
        prefs(activity).edit().putString(KEY_TAG, tag).apply()
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        // Keep strings read through the application context (ViewModel messages) in sync.
        val appContext = activity.applicationContext
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        activity.recreate()
    }
}