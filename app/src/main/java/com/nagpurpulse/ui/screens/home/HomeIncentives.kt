// HomeIncentives.kt
// Home-screen building blocks that make it easy and rewarding to take part:
//   • MomentumCard                – streak, next goal and social proof (get_posting_momentum)
//   • ReplyOpportunitiesStrip     – "Be the first to reply" (get_reply_opportunities)
// Both render nothing when there is nothing real to show.

package com.nagpurpulse.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.PostingMomentum
import com.nagpurpulse.data.model.categoryDisplay
import com.nagpurpulse.data.model.categoryEmoji
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.OrangePrimary

/** Headline + supporting line for the user's current goal. */
private fun momentumCopy(m: PostingMomentum): Pair<String, String> {
    val proof = when {
        m.repliesReceived7d > 0 ->
            " ${m.repliesReceived7d} ${if (m.repliesReceived7d == 1) "reply" else "replies"} on your posts this week."
        else -> ""
    }
    return when (m.nextGoal) {
        "first_post" -> "Share your first post" to
            if (m.peoplePostedToday > 0)
                "${m.peoplePostedToday} Nagpurians posted today. Your neighbours would love to hear from you."
            else "Be the first voice in Nagpur today."
        "three_posts" -> "Reach 3 posts" to
            "You're off to a great start. Every post helps your area come alive.$proof"
        "seven_day_streak" -> "Build a 7-day streak" to
            (if (m.activeToday) "Nice, you're active today. Come back tomorrow to keep it going."
             else "Post or reply today to keep your streak alive.") + proof
        else -> "Post 10 times this week" to "You're a regular. Keep Nagpur talking.$proof"
    }
}

/**
 * Streak + next goal. Shows a "Share a thought" button until the user has been active today,
 * so the next step is always one tap away.
 */
@Composable
internal fun MomentumCard(
    momentum: PostingMomentum?,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (momentum == null) return
    val (headline, support) = momentumCopy(momentum)
    val target = momentum.goalTarget.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = (momentum.goalProgress.toFloat() / target).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "goal_progress"
    )
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrangePrimary.copy(alpha = 0.08f))
            .border(1.dp, OrangePrimary.copy(alpha = 0.25f), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (momentum.streakDays >= 2) Icons.Filled.LocalFireDepartment else Icons.Filled.Bolt,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = headline,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (momentum.streakDays >= 2) {
                Text(
                    text = "${momentum.streakDays}-day streak",
                    color = OrangePrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = support,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = OrangePrimary,
                trackColor = OrangePrimary.copy(alpha = 0.18f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${momentum.goalProgress.coerceAtMost(target)}/$target",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (!momentum.activeToday || momentum.postsTotal == 0) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onCreatePost,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Share a thought", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Horizontal strip of recent posts that still need a first reply. */
@Composable
internal fun ReplyOpportunitiesStrip(
    posts: List<Post>,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (posts.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Be the first to reply",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(posts, key = { it.id }) { post ->
                ReplyOpportunityCard(post = post, onClick = { onPostClick(post.id) })
            }
        }
    }
}

@Composable
private fun ReplyOpportunityCard(post: Post, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape)
            .pressScale(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = "${post.categoryEmoji()} ${post.categoryDisplay()} · ${post.timeAgo()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = post.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (post.commentCount == 0) "No replies yet · Say something" else "1 reply · Join in",
            color = OrangePrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
