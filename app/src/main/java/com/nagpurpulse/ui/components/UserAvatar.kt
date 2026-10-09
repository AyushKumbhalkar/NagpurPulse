// UserAvatar.kt
// java/com/nagpurpulse/ui/components/UserAvatar.kt

package com.nagpurpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

// Warm, brand-friendly palette. The colour is derived from the name so a person
// always gets the same avatar colour everywhere in the app.
private val AvatarPalette = listOf(
    Color(0xFFFF6B00), // brand orange
    Color(0xFF0A84FF),
    Color(0xFF34C759),
    Color(0xFFAF52DE),
    Color(0xFFFF375F),
    Color(0xFF5AC8FA),
    Color(0xFFFF9F0A)
)

private fun avatarColorFor(name: String): Color {
    if (name.isBlank()) return AvatarPalette[0]
    return AvatarPalette[Math.abs(name.lowercase().hashCode()) % AvatarPalette.size]
}

private fun initialsFor(name: String): String {
    val clean = name.trim().trimStart('@')
    if (clean.isBlank()) return "?"
    val parts = clean.split(' ', '_', '.', '-').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        else -> clean.take(1).uppercase()
    }
}

/**
 * Round avatar: photo when available, otherwise coloured initials.
 * [isAnonymous] shows a neutral person glyph so anonymous posts still feel human
 * without revealing who wrote them.
 */
@Composable
fun UserAvatar(
    name: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isAnonymous: Boolean = false
) {
    val displayName = name.orEmpty()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            isAnonymous -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Anonymous",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }

            !imageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "$displayName avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size)
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(avatarColorFor(displayName)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsFor(displayName),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.38f).sp
                    )
                }
            }
        }
    }
}
