// notifications/NotifDeepLink.kt  — NEW FILE, drop into notifications/ package
package com.nagpurpulse.notifications

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Single source of truth for pending deep-link post IDs arriving from
 * notification taps. Both cold-start (via onCreate intent) and warm-start
 * (via onNewIntent) write here; the NavGraph LaunchedEffect collects and navigates.
 */
object NotifDeepLink {
    val pendingPostId = MutableStateFlow<String?>(null)
    val pendingConversationId = MutableStateFlow<String?>(null)
}
