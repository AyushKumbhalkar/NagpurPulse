package com.nagpurpulse.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Premium haptic feedback: every kind of interaction gets its own, distinct feel.
 *
 * Uses the platform's richer haptic constants (tick, confirm, reject, toggle) where the
 * device supports them and falls back to Compose's basic haptics otherwise. Nothing
 * happens when the user has touch feedback turned off in system settings.
 *
 *   val haptic = rememberHaptic()
 *   haptic.tap()        // light tick: selections, chips, small taps
 *   haptic.upvote()     // medium click: likes / upvotes
 *   haptic.toggle(on)   // switch-like controls
 *   haptic.threshold()  // a swipe just crossed its "commit" point
 *   haptic.success()    // something worked (post sent, saved, reported)
 *   haptic.error()      // something was rejected
 *   haptic.alert()      // urgent / destructive
 */
class HapticController(
    private val view: View,
    private val fallback: HapticFeedback
) {
    private fun perform(constant: Int, fallbackType: HapticFeedbackType) {
        val handled = runCatching { view.performHapticFeedback(constant) }.getOrDefault(false)
        if (!handled) runCatching { fallback.performHapticFeedback(fallbackType) }
    }

    /** Light tick for taps and selections. */
    fun tap() = perform(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackType.TextHandleMove)
    /** Same light tick, for likes / saves. */
    fun like() = tap()
    /** Light tick for picking an option. */
    fun selection() = tap()
    /** Medium click for upvotes. */
    fun upvote() = perform(HapticFeedbackConstants.CONTEXT_CLICK, HapticFeedbackType.TextHandleMove)

    /** Switch on / off. */
    fun toggle(on: Boolean) {
        if (Build.VERSION.SDK_INT >= 34) {
            perform(
                if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF,
                HapticFeedbackType.TextHandleMove
            )
        } else tap()
    }

    /** A swipe just crossed the point where releasing will commit the action. */
    fun threshold() {
        if (Build.VERSION.SDK_INT >= 34) {
            perform(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, HapticFeedbackType.TextHandleMove)
        } else upvote()
    }

    /** Confirmation: the action worked. */
    fun success() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
        HapticFeedbackType.LongPress
    )
    /** Strong pulse on post creation. */
    fun post() = success()

    /** Rejection: the action failed or was refused. */
    fun error() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
        HapticFeedbackType.LongPress
    )

    /** Urgent tap for alert interactions and destructive actions. */
    fun alert() = perform(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptic(): HapticController {
    val view = LocalView.current
    val fallback = LocalHapticFeedback.current
    return remember(view, fallback) { HapticController(view, fallback) }
}
