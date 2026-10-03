// java/com/nagpurpulse/ui/screens/onboarding/ProfilePictureScreen.kt

package com.nagpurpulse.ui.screens.onboarding


import com.nagpurpulse.ui.screens.profile.RandomImages
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

import kotlin.random.Random

// ── Placeholder avatar list ───────────────────────────────────────────────────


// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun ProfilePictureScreen(
    onBack: () -> Unit,
    isSaving: Boolean,
    onContinue: (String) -> Unit
) {
    var currentAvatarIndex by remember {
        mutableStateOf(Random.nextInt(RandomImages.avatars.size))
    }
    var visible by remember { mutableStateOf(true) }

    val isDark = LocalIsDarkTheme.current
    val bgColor = if (isDark) BackgroundDark else BackgroundLight
    val currentAvatar = RandomImages.avatars[currentAvatarIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    "Back",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                // Mini logo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, OrangePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("~", color = OrangePrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Progress stepper (3 steps - all leading up to profile pic) ─
            OnboardingProgressStepper(
                steps = listOf("Create Account", "Your Identity", "Choose Username", "Profile Picture"),
                currentStep = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Title ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                                append("Your ")
                            }
                            withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                                append("Profile Picture")
                            }
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "We've generated a profile picture for you.\nYou can keep it or generate a new one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Avatar preview ────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow ring
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(OrangeGlow.copy(0.6f), Color.Transparent)
                                )
                            )
                    )
                    // Orange border ring
                    Box(
                        modifier = Modifier
                            .size(168.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(listOf(OrangePrimary, OrangeLight)),
                                shape = CircleShape
                            )
                    )
                    // Avatar image
                    AsyncImage(
                        model = currentAvatar,
                        contentDescription = "Profile avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(158.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Generate New button ───────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (isDark) SurfaceOne else Color.White
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(0.3f),
                            RoundedCornerShape(26.dp)
                        )


                        .pressScale(onClick = {
                            var newIndex: Int

                            do {
                                newIndex = Random.nextInt(RandomImages.avatars.size)
                            } while (newIndex == currentAvatarIndex)

                            currentAvatarIndex = newIndex
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Generate New",
                            color = OrangePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── "You can change it later" info card ───────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) SurfaceOne else Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(OrangeSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "You can change it later",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Don't worry, you can always update your profile picture from your profile settings.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Continue button ───────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.horizontalGradient(listOf(OrangePrimary, OrangeLight)))
                        .then(
                            if (!isSaving)
                                Modifier.pressScale(
                                    onClick = {
                                        onContinue(currentAvatar)
                                    }
                                )
                            else
                                Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )

                            Spacer(Modifier.width(12.dp))

                            Text(
                                "Creating Profile...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                    } else {

                        Text(
                            "Continue →",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
