// java/com/nagpurpulse/ui/screens/onboarding/UsernameScreen.kt

package com.nagpurpulse.ui.screens.onboarding

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.nagpurpulse.data.repository.UsernameAvailability
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

// ── Username generator ────────────────────────────────────────────────────────

private val nagpurPlaces = listOf(
    "ZeroMile",
    "Futala",
    "Mahal",
    "Itwari",
    "Sadar",
    "Koradi",
    "Ajni",
    "Trimurti",
    "Khamla",
    "Besa",
    "Jamtha",
    "Pardi",
    "Wadi",
    "Indora",
    "Nari",
    "Mihan",
    "Gokulpeth",
    "Mominpura",
    "Nandanvan",
    "Reshimbagh",
    "Hudkeshwar",
    "Chaoni",
"Mankapur",
    "Dighori",
    "Kamthi"
)

private val nagpurFood = listOf(
    "Poha",
    "Tarri",
    "Saoji",
    "Tea",
    "Orange",
    "Kachori",
    "Samosa",
    "Biryani"

)

private val nagpurThings = listOf(
    "Fox",
    "Wolf",
    "Tiger",
    "Owl",
    "Soul",
    "Scout",
    "Rider",
    "Ninja",
    "Boss",
    "King",
    "Guru",
    "Pulse",
    "Legend",
    "Hero",
    "Star",
    "Pro",
    "Buddy",
    "Chief",
    "Hunter",
    "Walker",
    "Driver"
)

private val nagpurAdjectives = listOf(
    "Cool",
    "Epic",
    "Lucky",
    "Bold",
    "Chill",
    "Urban",
    "Fast",
    "Brave",
    "Happy",
    "Smart",
    "Wild",
    "Golden",
    "Silent",
    "Mighty"
)

fun generateNagpurUsername(): String {

    val number = (10..99).random()

    return when ((1..4).random()) {

        1 -> "${nagpurPlaces.random()}${nagpurThings.random()}$number"

        2 -> "${nagpurFood.random()}${nagpurThings.random()}$number"

        3 -> "${nagpurAdjectives.random()}${nagpurPlaces.random()}$number"

        else -> "${nagpurPlaces.random()}${nagpurFood.random()}$number"
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun UsernameScreen(
    onBack: () -> Unit,
    onNext: (String) -> Unit,
    checkUsernameAvailable: suspend (String) -> UsernameAvailability,
    initialUsername: String? = null
) {
    var username by rememberSaveable { mutableStateOf((initialUsername ?: generateNagpurUsername()).lowercase()) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameAvailability by remember { mutableStateOf<UsernameAvailability?>(null) }
    val usernameIsValid = username.matches(Regex("^[A-Za-z][A-Za-z0-9_]{2,23}$"))
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(username) {
        usernameAvailability = null
        if (!usernameIsValid) {
            isCheckingUsername = false
            return@LaunchedEffect
        }
        isCheckingUsername = true
        kotlinx.coroutines.delay(350)
        usernameAvailability = checkUsernameAvailable(username)
        isCheckingUsername = false
    }

    val isCompactWidth = LocalConfiguration.current.screenWidthDp < 360
    val isDark = LocalIsDarkTheme.current
    val bgColor = if (isDark) BackgroundDark else BackgroundLight

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
                // NagpurPulse brand mark
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, OrangePrimary.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.nagpurpulse_orange_n_icon),
                        contentDescription = "NagpurPulse",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Progress stepper (2 steps shown per screenshot) ───────
            OnboardingProgressStepper(
                steps = listOf("Create Account", "Your Identity", "Choose Username", "Profile Picture"),
                currentStep = 2,
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
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                                append("Choose Your ")
                            }
                            withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
                                append("Username")
                            }
                        },
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "We've generated a unique username for you.\nYou can keep it or generate a new one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Username card ─────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) SurfaceOne else Color.White)
                        .border(1.dp, OrangePrimary.copy(0.25f), RoundedCornerShape(20.dp))
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // User avatar circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(OrangeSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Your username",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { input ->
                                username = input.filter { it.isLetterOrDigit() || it == '_' }.take(24).lowercase()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            label = { Text("Username") },
                            singleLine = true,
                            isError = username.isNotEmpty() && (!usernameIsValid || usernameAvailability == UsernameAvailability.TAKEN || usernameAvailability == UsernameAvailability.UNABLE_TO_CHECK),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Ascii
                            ),
                            supportingText = {
                                Text("3–24 characters; start with a letter. Letters, numbers and _ only.")
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        val statusText = when {
                            !usernameIsValid -> "Enter a valid username"
                            isCheckingUsername -> "Checking availability…"
                            usernameAvailability == UsernameAvailability.AVAILABLE -> "Username is available"
                            usernameAvailability == UsernameAvailability.TAKEN -> "Username is already taken"
                            usernameAvailability == UsernameAvailability.UNABLE_TO_CHECK -> "Couldn’t check availability. Check your connection and edit to retry."
                            else -> "Checking availability…"
                        }
                        Text(
                            text = statusText,
                            color = when {
                                !usernameIsValid || usernameAvailability == UsernameAvailability.TAKEN || usernameAvailability == UsernameAvailability.UNABLE_TO_CHECK -> MaterialTheme.colorScheme.error
                                usernameAvailability == UsernameAvailability.AVAILABLE -> GreenSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Three buttons row ─────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Transparent)
                            .border(1.dp, OrangePrimary.copy(0.6f), RoundedCornerShape(24.dp))
                            .pressScale(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("←", color = OrangePrimary, fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Previous",
                                color = OrangePrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Generate New button
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Transparent)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.5f), RoundedCornerShape(24.dp))
                            .pressScale(onClick = { username = generateNagpurUsername().lowercase() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isCompactWidth) "Generate" else "Generate New",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isCompactWidth) 11.sp else 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Next button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.horizontalGradient(listOf(OrangePrimary, OrangeLight)))
                            .then(
                                if (usernameIsValid && usernameAvailability == UsernameAvailability.AVAILABLE && !isCheckingUsername)
                                    Modifier.pressScale(onClick = { onNext(username.trim()) })
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Next",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("→", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── "Why this username?" info card ────────────────────────
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
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Why this username?",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Usernames must be unique and can't be changed later. Don't worry, you can generate a new one if you'd like!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
