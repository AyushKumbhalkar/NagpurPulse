package com.nagpurpulse.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Premium haptic feedback utility.
 * Call from within composables to provide tactile response on key interactions.
 *
 * Usage:
 *   val haptic = rememberHaptic()
 *   haptic.upvote()    // on upvote click
 *   haptic.post()      // on post submit
 *   haptic.alert()     // on alert interaction
 *   haptic.tap()       // generic tap
 */
class HapticController(
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    /** Satisfying confirmation on upvote */
    fun upvote()     = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    /** Light tap for likes / saves */
    fun like()       = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    /** Strong pulse on post creation */
    fun post()       = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    /** Urgent tap for alert interactions */
    fun alert()      = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    /** Generic light interaction */
    fun tap()        = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    /** Error / rejection */
    fun error()      = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    /** Success confirmation */
    fun success()    = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberHaptic(): HapticController {
    val haptic = LocalHapticFeedback.current
    return HapticController(haptic)
}
