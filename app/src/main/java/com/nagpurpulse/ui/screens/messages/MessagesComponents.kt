// java/com/nagpurpulse/ui/screens/messages/MessagesComponents.kt
//
// Small building blocks shared by the inbox and the chat screen.

package com.nagpurpulse.ui.screens.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.theme.*

/** Emoji "incognito" avatar with an optional green online dot. */
@Composable
internal fun IncognitoAvatar(
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    isOnline: Boolean = false
) {
    val color = incognitoColor(seed)
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(0.5f), color.copy(0.2f))))
                .border(1.5.dp, color.copy(0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(incognitoEmoji(seed), fontSize = (size.value * 0.42f).sp)
        }
        if (isOnline) {
            Box(
                Modifier
                    .size(size * 0.26f)
                    .clip(CircleShape)
                    .background(GreenSuccess)
                    .border(2.dp, Background, CircleShape)
            )
        }
    }
}

/** Three softly bouncing dots, used for "typing…". */
@Composable
internal fun TypingDots(
    modifier: Modifier = Modifier,
    color: Color = SecondaryText
) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0, 160, 320).forEach { delayMs ->
            val a by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 480, delayMillis = delayMs),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$delayMs"
            )
            Box(
                Modifier
                    .size(6.dp)
                    .alpha(a)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

private val ReportReasons = listOf(
    "Spam",
    "Harassment or bullying",
    "Inappropriate content",
    "Scam or fraud",
    "Other"
)

/** Reason picker used from the chat menu. [onSubmit] receives the reason and whether to also block. */
@Composable
internal fun ReportDialog(
    username: String,
    isAlreadyBlocked: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, alsoBlock: Boolean) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    var alsoBlock by remember { mutableStateOf(!isAlreadyBlocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = PrimaryText,
        textContentColor = SecondaryText,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Report $username") },
        text = {
            Column {
                Text("Tell us what's wrong. Reports are reviewed by our team and the person isn't told who reported them.")
                Spacer(Modifier.height(10.dp))
                ReportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selected = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                        )
                        Text(reason, color = PrimaryText)
                    }
                }
                if (!isAlreadyBlocked) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { alsoBlock = !alsoBlock }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alsoBlock,
                            onCheckedChange = { alsoBlock = it },
                            colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                        )
                        Text("Also block $username", color = PrimaryText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let { onSubmit(it, alsoBlock && !isAlreadyBlocked) } }
            ) {
                Text("Report", color = if (selected != null) RedAlert else SecondaryText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryText) } }
    )
}

/** Slim "you're offline" strip that slides in/out. Shared by the inbox and the chat. */
@Composable
internal fun OfflineBanner(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RedAlert.copy(alpha = 0.14f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, color = PrimaryText, fontSize = 12.sp, maxLines = 2)
        }
    }
}
