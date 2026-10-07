package com.nagpurpulse.ui.screens.auth

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Tiny wrapper for signup-funnel events. Never put personal data (email, name,
 * password) in the parameters - only anonymous values like "true"/"false".
 */
object AuthAnalytics {
    fun log(context: Context, event: String, vararg params: Pair<String, String>) {
        runCatching {
            val bundle = Bundle().apply { params.forEach { (key, value) -> putString(key, value) } }
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(event, bundle)
        }
    }
}