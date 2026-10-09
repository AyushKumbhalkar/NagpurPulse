// ui/preferences/WellbeingManager.kt
//
// Optional, opt-in "take a break" nudge. While the app is in the foreground it shows one
// gentle message every REMINDER_MINUTES. Off by default; stored only on this device.
package com.nagpurpulse.ui.preferences

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object WellbeingManager {

    private const val PREFS = "wellbeing_prefs"
    private const val KEY_ENABLED = "break_reminder_enabled"

    const val REMINDER_MINUTES = 30L

    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) start(ctx) else stop()
    }

    /** Call from the activity's onStart. */
    fun start(ctx: Context) {
        stop()
        if (!isEnabled(ctx)) return
        val appContext = ctx.applicationContext
        val intervalMs = REMINDER_MINUTES * 60_000L
        val task = object : Runnable {
            override fun run() {
                Toast.makeText(
                    appContext,
                    "You've been here for a while \uD83C\uDF3F A short break does wonders.",
                    Toast.LENGTH_LONG
                ).show()
                handler.postDelayed(this, intervalMs)
            }
        }
        pending = task
        handler.postDelayed(task, intervalMs)
    }

    /** Call from the activity's onStop. */
    fun stop() {
        pending?.let { handler.removeCallbacks(it) }
        pending = null
    }
}
