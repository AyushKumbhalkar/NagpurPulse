// java/com/nagpurpulse/ui/components/CommentUtils.kt
package com.nagpurpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nagpurpulse.data.model.Comment
import java.util.concurrent.ConcurrentHashMap

/** Wall clock the thread screen ticks once a minute so "5m" labels stay fresh. */
val LocalNowMillis = compositionLocalOf { System.currentTimeMillis() }

/** Must match the database check constraint. */
const val MAX_COMMENT_LENGTH = 2000

fun commentDisplayName(comment: Comment): String = when {
    comment.isAnonymous -> comment.anonAlias?.takeIf { it.isNotBlank() } ?: "Anonymous"
    else -> comment.username?.takeIf { it.isNotBlank() } ?: "unknown"
}

/** In-memory drafts so half-written comments survive leaving a thread (never written to disk). */
object CommentDrafts {
    private val drafts = ConcurrentHashMap<String, String>()
    fun get(key: String): String = drafts[key].orEmpty()
    fun set(key: String, text: String) { if (text.isBlank()) drafts.remove(key) else drafts[key] = text }
}

private val AvatarPalette = listOf(
    Color(0xFFFF7A3D) to Color(0xFFFFB347), Color(0xFF7C4DFF) to Color(0xFFB388FF),
    Color(0xFF00B8A9) to Color(0xFF6FE7DD), Color(0xFFE91E63) to Color(0xFFFF80AB),
    Color(0xFF3D8BFF) to Color(0xFF82B1FF), Color(0xFF43A047) to Color(0xFF9CCC65)
)

private fun initialsFor(name: String, isAnonymous: Boolean): String {
    if (isAnonymous) {
        val parts = name.split('_').filter { it.isNotBlank() }
        if (parts.size >= 3 && parts[0].equals("Anon", ignoreCase = true)) {
            return "${parts[1].first()}${parts[2].first()}".uppercase()
        }
        return ""
    }
    return name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
}

/** Avatar that never calls a third-party service: profile photo, else locally drawn initials. */
@Composable
fun CommentAvatar(
    displayName: String, avatarUrl: String?, isAnonymous: Boolean,
    modifier: Modifier = Modifier, size: Dp = 36.dp
) {
    val colors = remember(displayName) { AvatarPalette[Math.floorMod(displayName.hashCode(), AvatarPalette.size)] }
    val initials = remember(displayName, isAnonymous) { initialsFor(displayName, isAnonymous) }
    Box(
        modifier = modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.first, colors.second))),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isEmpty()) {
            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = Color.White,
                modifier = Modifier.size(size * 0.5f))
        } else {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
        }
        if (!isAnonymous && !avatarUrl.isNullOrBlank()) {
            AsyncImage(model = avatarUrl, contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
        }
    }
}

private val UrlRegex = Regex("""(https?://[^\s]+)""")
private val MentionRegex = Regex("""(?<![\w])@[A-Za-z0-9_]{2,}""")

/** Styles @mentions and makes links tappable. Plain text is returned untouched. */
fun linkifyComment(text: String, accent: Color): AnnotatedString {
    if (!text.contains("http") && !text.contains('@')) return AnnotatedString(text)
    data class Span(val start: Int, val end: Int, val isUrl: Boolean)
    val spans = mutableListOf<Span>()
    UrlRegex.findAll(text).forEach { m ->
        var end = m.range.last + 1
        while (end > m.range.first && text[end - 1] in ".,;:!?)\"'") end--
        if (end > m.range.first) spans += Span(m.range.first, end, true)
    }
    MentionRegex.findAll(text).forEach { m ->
        val overlaps = spans.any { it.isUrl && m.range.first < it.end && m.range.last + 1 > it.start }
        if (!overlaps) spans += Span(m.range.first, m.range.last + 1, false)
    }
    if (spans.isEmpty()) return AnnotatedString(text)
    spans.sortBy { it.start }
    return buildAnnotatedString {
        var cursor = 0
        for (span in spans) {
            if (span.start < cursor) continue
            append(text.substring(cursor, span.start))
            val piece = text.substring(span.start, span.end)
            if (span.isUrl) {
                withLink(LinkAnnotation.Url(piece, TextLinkStyles(
                    SpanStyle(color = accent, textDecoration = TextDecoration.Underline)))) { append(piece) }
            } else {
                pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)); append(piece); pop()
            }
            cursor = span.end
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}
