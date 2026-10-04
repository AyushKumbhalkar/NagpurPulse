package com.nagpurpulse.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    onGuestMode: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFFFBF6))) {
        // The supplied artwork contains the brand logo and community network.
        // Keep it edge-to-edge and let the lower blank area hold the copy/CTA.
        Image(
            painter = painterResource(id = R.drawable.nagpurpulse_social_network_onboarding),
            contentDescription = "NagpurPulse community network",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF171717))) { append("Your city, ") }
                    withStyle(SpanStyle(color = Color(0xFFF45B0B))) { append("together.") }
                },
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Ask questions, share finds and discover\nwhat’s happening around you in Nagpur.",
                color = Color(0xFF737373),
                fontSize = 18.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(34.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
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
                        "Get Started",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("→", color = Color.White, fontSize = 31.sp, lineHeight = 31.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF45B0B))
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
