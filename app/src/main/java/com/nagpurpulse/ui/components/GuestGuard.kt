//java/com/nagpurpulse/ui/components/GuestGuard.kt

package com.nagpurpulse.ui.components


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nagpurpulse.ui.theme.*

/**
 * Show this dialog when a guest tries a write action.
 * Usage: if (authRepository.isGuest) { showGuestDialog = true }
 */
@Composable
fun GuestLoginPrompt(
    visible: Boolean,
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)) +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ),
        exit = fadeOut(animationSpec = tween(200)) +
                scaleOut(
                    targetScale = 0.88f
                )
    ) {
        Dialog(
            onDismissRequest  = onDismiss,
            properties        = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF161616), Color(0xFF0E0E0E)))
                    )
                    .border(1.dp, OrangePrimary.copy(0.2f), RoundedCornerShape(28.dp))
                    .padding(28.dp)
            ) {
                // Close button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceTwo)
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar icon with lock overlay
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(OrangePrimary.copy(0.15f), Color.Transparent)
                                )
                            )
                            .border(1.5.dp, OrangePrimary.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(OrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Please Login to Continue",
                        color      = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp,
                        textAlign  = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        buildAnnotatedString {
                            append("You need to login to access your profile, save your preferences and be a part of the ")
                            withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.SemiBold)) {
                                append("Nagpur Pulse")
                            }
                            append(" community.")
                        },
                        color     = TextSecondary,
                        fontSize  = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // Primary CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.horizontalGradient(listOf(OrangePrimary, OrangeLight))
                            )
                            .pressScale(onClick = onLoginClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Login / Sign Up", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                        Text("  or  ", color = TextTertiary, fontSize = 12.sp)
                        HorizontalDivider(Modifier.weight(1f), color = DividerColor)
                    }

                    Spacer(Modifier.height(14.dp))

                    // Guest continue button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(SurfaceTwo)
                            .border(1.dp, DividerColor, RoundedCornerShape(25.dp))
                            .pressScale(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Continue as Guest", color = TextSecondary, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Some features will be limited", color = TextTertiary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
