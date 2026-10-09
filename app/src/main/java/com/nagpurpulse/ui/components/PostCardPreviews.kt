// java/com/nagpurpulse/ui/components/PostCardPreviews.kt
//
// Android Studio previews for PostCard. Open this file and use "Split" / "Design" to review the
// card in light/dark, at a large font size, in Marathi, and read-only, without running the app.

package com.nagpurpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.ui.theme.NagpurPulseTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

private fun minutesAgo(minutes: Long): String =
    Instant.now().minus(minutes, ChronoUnit.MINUTES).toString()

private val freshTrending = Post(
    id = "p1",
    userId = "u1",
    title = "Best santra barfi near Sitabuldi? Looking for something fresh for Diwali",
    body = "Went to three shops yesterday and everything felt stale. Anyone know a place that makes it fresh daily? Budget is flexible.",
    category = "food",
    areaTag = "Sitabuldi",
    upvotes = 34,
    commentCount = 12,
    username = "rahul_n",
    isVerified = true,
    createdAt = minutesAgo(8)
)

private val pinnedLocked = Post(
    id = "p2",
    userId = "u2",
    title = "Community guidelines — please read before posting",
    body = "Be kind, no spam, keep it about Nagpur.",
    category = "alerts",
    areaTag = "Dharampeth",
    upvotes = 120,
    commentCount = 4,
    username = "nagpurpulse",
    isPinned = true,
    isLocked = true,
    createdAt = minutesAgo(60L * 24 * 40)
)

private val marathi = Post(
    id = "p3",
    userId = "u3",
    title = "नागपूरमध्ये सर्वोत्तम सावजी मटण कुठे मिळते? धरमपेठ आणि सिताबर्डी परिसरात सुचवा",
    body = "आम्ही या रविवारी कुटुंबासोबत जाण्याचा विचार करत आहोत. शांत जागा आणि चांगली चव हवी आहे.",
    category = "food",
    areaTag = "धरमपेठ",
    isAnonymous = true,
    upvotes = 3,
    commentCount = 1,
    createdAt = minutesAgo(180)
)

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { content() }
}

@Composable
private fun InteractiveCards() {
    PreviewSurface {
        PostCard(
            post = freshTrending, currentVote = "up", onClick = {}, onUserClick = {},
            onUpvote = {}, onToggleSave = {}, isSaved = true, onReport = {}
        )
        PostCard(
            post = pinnedLocked, onClick = {}, onUpvote = {}, onToggleSave = {}, onReport = {}
        )
        PostCard(
            post = marathi, onClick = {}, isOwnPost = true, onUpvote = {}, onToggleSave = {},
            onEdit = {}, onDelete = {}
        )
    }
}

@Preview(name = "Light · 360dp", showBackground = true, widthDp = 360)
@Composable
fun PostCardLightPreview() = NagpurPulseTheme(darkTheme = false) { InteractiveCards() }

@Preview(name = "Dark · 360dp", showBackground = true, widthDp = 360)
@Composable
fun PostCardDarkPreview() = NagpurPulseTheme(darkTheme = true) { InteractiveCards() }

@Preview(name = "Narrow 320dp · font 1.5x", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
fun PostCardLargeFontPreview() = NagpurPulseTheme(darkTheme = false) { InteractiveCards() }

@Preview(name = "Read-only (no handlers)", showBackground = true, widthDp = 360)
@Composable
fun PostCardReadOnlyPreview() = NagpurPulseTheme(darkTheme = false) {
    PreviewSurface { PostCard(post = freshTrending, onClick = {}) }
}
