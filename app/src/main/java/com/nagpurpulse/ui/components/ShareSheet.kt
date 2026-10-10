// java/com/nagpurpulse/ui/components/ShareSheet.kt

package com.nagpurpulse.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.data.model.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WhatsAppGreen = Color(0xFF25D366)
private val TelegramBlue = Color(0xFF229ED9)
private val InstagramBrush = Brush.linearGradient(listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF)))

/**
 * Share sheet shown instead of the bare system chooser.
 * It previews the exact card friends will receive, puts the one-tap WhatsApp action first
 * (the app people in Nagpur actually share on) and keeps everything else one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostShareSheet(post: Post, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var card by remember(post.id) { mutableStateOf<Bitmap?>(null) }
    var copied by remember { mutableStateOf(false) }
    val shareUrl = remember(post.id) { PostLinks.shareUrl(post.id) }
    val caption = remember(post, shareUrl) { ShareCardRenderer.caption(post, shareUrl) }

    LaunchedEffect(post.id) { card = ShareCardRenderer.render(context, post) }
    LaunchedEffect(copied) { if (copied) { delay(1800); copied = false } }

    // The preview settles into place like a card being handed over.
    val settle by animateFloatAsState(
        targetValue = if (card != null) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "share_card_settle"
    )

    fun send(targetPackage: String?) {
        haptic.tap()
        scope.launch {
            val bitmap = card ?: ShareCardRenderer.render(context, post).also { card = it }
            val uri = ShareCardRenderer.saveToCache(context, bitmap, post.id)
            val intent = Intent(Intent.ACTION_SEND).apply {
                if (uri != null) {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_SUBJECT, post.title)
            }
            val launched = if (targetPackage != null) {
                try {
                    context.startActivityCompat(Intent(intent).setPackage(targetPackage))
                    true
                } catch (_: ActivityNotFoundException) {
                    false // app not installed: fall back to the full chooser below
                }
            } else false
            if (!launched) {
                try {
                    context.startActivityCompat(Intent.createChooser(intent, null))
                } catch (_: Exception) {
                    // No app can handle a share; nothing sensible to show.
                }
            }
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Share with your people",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                socialProofLine(post),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // ── Live preview of exactly what they will receive ─────────
            Crossfade(targetState = card, label = "share_preview") { bmp ->
                if (bmp == null) {
                    Box(
                        modifier = Modifier
                            .height(300.dp)
                            .width(240.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .shimmerEffect(RoundedCornerShape(22.dp))
                    )
                } else {
                    Image(
                        bitmap = remember(bmp) { bmp.asImageBitmap() },
                        contentDescription = "Preview of the card your friends will see",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(300.dp)
                            .graphicsLayer {
                                rotationZ = -3f * (1f - settle) - 1f
                                val s = 0.9f + 0.1f * settle
                                scaleX = s; scaleY = s
                            }
                            .shadow(18.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Primary action ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(WhatsAppGreen)
                    .pressScale { send("com.whatsapp") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Send on WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(18.dp))

            // ── Secondary targets ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareTarget("Instagram", Icons.Filled.PhotoCamera, brush = InstagramBrush) { send("com.instagram.android") }
                ShareTarget("Telegram", Icons.AutoMirrored.Filled.Send, color = TelegramBlue) { send("org.telegram.messenger") }
                ShareTarget(
                    label = if (copied) "Copied" else if (shareUrl != null) "Copy link" else "Copy text",
                    icon = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    color = if (copied) WhatsAppGreen else MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = if (copied) Color.White else MaterialTheme.colorScheme.onSurface
                ) {
                    haptic.tap()
                    clipboard.setText(AnnotatedString(shareUrl ?: caption))
                    copied = true
                }
                ShareTarget(
                    "More", Icons.Filled.MoreHoriz,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurface
                ) { send(null) }
            }
        }
    }
}

@Composable
private fun ShareTarget(
    label: String,
    icon: ImageVector,
    color: Color = Color.Unspecified,
    brush: Brush? = null,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.pressScale(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .let { if (brush != null) it.background(brush) else it.background(color) },
            contentAlignment = Alignment.Center
