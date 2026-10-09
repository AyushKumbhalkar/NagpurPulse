package com.nagpurpulse.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.theme.*

@Composable
fun VoteButtons(
    upvotes: Int,
    downvotes: Int,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    userVote: String? = null,
    modifier: Modifier = Modifier
) {
    var upBurst      by remember { mutableStateOf(false) }
    var downBurst    by remember { mutableStateOf(false) }
    var displayCount by remember { mutableIntStateOf(upvotes) }
    var voted        by remember { mutableStateOf(userVote) }

    val upScale   by animateFloatAsState(if (upBurst) 1.45f else 1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh), label = "up", finishedListener = { upBurst = false })
    val downScale by animateFloatAsState(if (downBurst) 1.35f else 1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh), label = "down", finishedListener = { downBurst = false })

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Upvote
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(if (voted == "up") OrangeSubtle else SurfaceAlt)
                .border(1.dp, if (voted == "up") OrangePrimary.copy(0.6f) else Color.Transparent, RoundedCornerShape(22.dp))
                .pressScale {
                    upBurst = true
                    val newVoted = if (voted == "up") null else "up"
                    displayCount = when (newVoted) {
                        "up"  -> upvotes + 1
                        null  -> if (voted == "up") upvotes - 1 else upvotes
                        else  -> upvotes
                    }
                    voted = newVoted
                    onUpvote()
                }
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp, null,
                tint = if (voted == "up") OrangePrimary else SecondaryText,
                modifier = Modifier.size(20.dp).scale(upScale)
            )
            Spacer(Modifier.width(5.dp))
            AnimatedContent(
                displayCount,
                transitionSpec = {
                    if (targetState >= initialState)
                        (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                    else
                        (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "vote_count"
            ) { count ->
                Text(
                    formatCount(count),
                    color = if (voted == "up") OrangePrimary else SecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Downvote
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(if (voted == "down") RedSubtle else SurfaceAlt)
                .border(1.dp, if (voted == "down") RedAlert.copy(0.6f) else Color.Transparent, RoundedCornerShape(22.dp))
                .pressScale {
                    downBurst = true
                    voted     = if (voted == "down") null else "down"
                    onDownvote()
                }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown, null,
                tint = if (voted == "down") RedAlert else SecondaryText,
                modifier = Modifier.size(20.dp).scale(downScale)
            )
        }
    }
}
