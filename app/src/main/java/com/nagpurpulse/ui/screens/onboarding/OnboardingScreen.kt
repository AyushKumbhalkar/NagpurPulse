package com.nagpurpulse.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale

private val OnboardingBackground = Color(0xFFFFFBF6)
private val OnboardingOrange = Color(0xFFF45B0B)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    onGuestMode: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
    ) {
        // Capture BoxWithConstraints dimensions before entering nested layout scopes.
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val artworkHeight = (screenWidth * 0.98f).coerceIn(300.dp, 620.dp)
        val headlineSize = if (screenWidth < 360.dp) 29.sp else 35.sp
        val horizontalPadding = if (screenWidth < 360.dp) 16.dp else 24.dp

        // The supplied logo + community illustration is square. Keep its
        // native 1:1 aspect ratio and clip only the overflow of the artwork area.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(artworkHeight)
                    .clipToBounds()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.onboarding_logo),
                    contentDescription = "NagpurPulse logo and community network illustration",
                    modifier = Modifier
                        .width(screenWidth)
                        .height(screenWidth)
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.TopCenter
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF171717))) {
                            append("Your city, ")
                        }
                        withStyle(SpanStyle(color = OnboardingOrange)) {
                            append("together.")
                        }
                    },
                    fontSize = headlineSize,
                    lineHeight = headlineSize * 1.16f,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = if (screenWidth < 380.dp) 2 else 1
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Ask questions, share finds and discover what’s happening around you in Nagpur.",
                    color = Color(0xFF737373),
                    fontSize = if (screenWidth < 360.dp) 16.sp else 18.sp,
                    lineHeight = if (screenWidth < 360.dp) 22.sp else 25.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF65B0B), Color(0xFFFF7015))
                            )
                        )
                        .pressScale(onClick = onGetStarted),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = "→",
                            color = Color.White,
                            fontSize = 30.sp,
                            lineHeight = 30.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Static onboarding page indicators matching the supplied reference.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(38.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(OnboardingOrange)
                    )
                    repeat(2) {
                        Box(
                            Modifier
                                .width(12.dp)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD8D5D1))
                        )
                    }
                }

            }
        }
    }
}
