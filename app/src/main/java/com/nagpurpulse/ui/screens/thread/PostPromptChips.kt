// PostPromptChips.kt
// "Not sure what to post?" – tappable ideas from get_post_prompts. Tapping one fills the
// title, category and (when known) area; the user can still edit everything.

package com.nagpurpulse.ui.screens.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nagpurpulse.data.model.PostPrompt

@Composable
internal fun PostPromptChips(
    prompts: List<PostPrompt>,
    onPick: (PostPrompt) -> Unit,
    modifier: Modifier = Modifier
) {
    if (prompts.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = OrangeMain,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Not sure what to post? Try one of these",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(prompts, key = { it.key }) { prompt ->
                val shape = RoundedCornerShape(14.dp)
                Text(
                    text = prompt.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .clip(shape)
                        .background(OrangeMain.copy(alpha = 0.10f))
                        .border(1.dp, OrangeMain.copy(alpha = 0.35f), shape)
                        .clickable { onPick(prompt) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
