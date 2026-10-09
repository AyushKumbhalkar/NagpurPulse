// java/com/nagpurpulse/ui/components/PostLinks.kt

package com.nagpurpulse.ui.components

import com.nagpurpulse.BuildConfig

/**
 * Shareable web link for a post. Disabled by default so the app never shares a link that 404s.
 *
 * Turn on with `POST_SHARE_LINKS=true` in local.properties once https://nagpurpulse.in/p/{id}
 * serves a landing page and assetlinks.json covers the host (see AUTH_LINKS_SETUP.md).
 */
object PostLinks {
    private const val BASE = "https://nagpurpulse.in/p/"

    fun shareUrl(postId: String): String? =
        if (BuildConfig.POST_SHARE_LINKS && postId.isNotBlank()) BASE + postId else null
}
