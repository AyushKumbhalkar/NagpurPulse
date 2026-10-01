// this is the SubSettingsScreens.kt file
// java/com/nagpurpulse/ui/screens/settings/SubSettingsScreens.kt

package com.nagpurpulse.ui.screens.settings

import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.BlueInfo
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.PurpleNight
import com.nagpurpulse.ui.theme.RedAlert
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Surface

// ══════════════════════════════════════════════════════════════════════════════
//  INCOGNITO SETTINGS SCREEN
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun IncognitoSettingsScreen(navController: NavController) {
    var incognitoMode   by remember { mutableStateOf(true) }
    var handleVisible   by remember { mutableStateOf(true) }
    var randomAvatar    by remember { mutableStateOf(true) }
    var hideLocation    by remember { mutableStateOf(true) }
    var hideMyComments  by remember { mutableStateOf(true) }
    var hidePosts       by remember { mutableStateOf(false) }
    var hideUpvotes     by remember { mutableStateOf(true) }
    var hideFollowers   by remember { mutableStateOf(true) }
    var noLeaderboard   by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Surface, Background),
                            0f,
                            120f
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = PrimaryText
                        )
                    }

                    Text(
                        "Incognito Settings",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                HorizontalDivider(
                    color = Divider,
                    thickness = 0.5.dp
                )
            }
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = PurpleNight,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Stay private. Stay incognito.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }
            }

            item { SectionHeader("INCOGNITO MODE") }

            item {
                SettingsGroup {
                    SettingsRowToggle(
                        "Enable Incognito Mode",
                        "Your activities stay private",
                        Icons.Filled.VisibilityOff,
                        PurpleNight,
                        incognitoMode
                    ) { incognitoMode = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Incognito Handle Visibility",
                        "Show handle only, hide real identity",
                        Icons.Filled.Badge,
                        OrangePrimary,
                        handleVisible
                    ) { handleVisible = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Random Avatar",
                        "Use avatars, not real photos",
                        Icons.Filled.AccountCircle,
                        BlueInfo,
                        randomAvatar
                    ) { randomAvatar = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide Real Location",
                        "Others won't see your exact location",
                        Icons.Filled.LocationOff,
                        GreenSuccess,
                        hideLocation
                    ) { hideLocation = it }
                }
            }

            item { SectionHeader("PRIVACY CONTROLS") }

            item {
                SettingsGroup {
                    SettingsRowToggle(
                        "Hide My Comments",
                        "Hide all my comments",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        hideMyComments
                    ) { hideMyComments = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide My Posts",
                        "Only you can see your posts",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        hidePosts
                    ) { hidePosts = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide Upvotes",
                        "Hide my upvotes & reactions",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        hideUpvotes
                    ) { hideUpvotes = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide Followers/Following",
                        "Hide my followers & following",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        hideFollowers
                    ) { hideFollowers = it }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Don't Show in Leaderboard",
                        "Hide from top contributors list",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        noLeaderboard
                    ) { noLeaderboard = it }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PurpleNight.copy(0.12f))
                        .border(
                            1.dp,
                            PurpleNight.copy(0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(
    DensityManager.cardPadding.dp
)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = PurpleNight,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            "Incognito Tips",
                            color = PurpleNight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    listOf(
                        "Your handle is your identity.",
                        "No real name is visible.",
                        "You're protected from tracking."
                    ).forEach { tip ->

                        Row(
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Text(
                                "•  ",
                                color = PurpleNight,
                                fontSize = 12.sp
                            )

                            Text(
                                tip,
                                color = SecondaryText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    }



@Composable
fun SecuritySettingsScreen(
    navController: NavController
) {
    var biometricLogin by remember { mutableStateOf(false) }
    var twoFactorAuth by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Surface, Background),
                            0f,
                            120f
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = PrimaryText
                        )
                    }

                    Text(
                        text = "Security",
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                HorizontalDivider(
                    color = Divider,
                    thickness = 0.5.dp
                )
            }
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                SectionHeader("ACCOUNT SECURITY")
            }

            item {
                SettingsGroup {

                    SettingsRow(
                        "Change Password",
                        "Update your account password",
                        Icons.Filled.Lock,
                        OrangePrimary
                    ) {}

                    SettingsDivider()

                    SettingsRowToggle(
                        "Biometric Login",
                        "Use fingerprint or face unlock",
                        Icons.Filled.Fingerprint,
                        BlueInfo,
                        biometricLogin
                    ) {

                        biometricLogin = it

                        val prefs =
                            navController.context.getSharedPreferences(
                                "security",
                                android.content.Context.MODE_PRIVATE
                            )

                        prefs.edit()
                            .putBoolean("biometric_enabled", it)
                            .apply()
                    }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Two-Factor Authentication",
                        "Extra protection for your account",
                        Icons.Filled.Security,
                        GreenSuccess,
                        twoFactorAuth
                    ) {
                        twoFactorAuth = it
                    }
                }
            }

            item {
                SectionHeader("SESSIONS")
            }

            item {
                SettingsGroup {

                    SettingsRow(
                        "Active Devices",
                        "Manage logged-in devices",
                        Icons.Filled.Devices,
                        PurpleNight
                    ) {}

                    SettingsDivider()

                    SettingsRow(
                        "Sign Out Everywhere",
                        "Log out from all devices",
                        Icons.Filled.Logout,
                        RedAlert
                    ) {}
                }
            }

            item {
                Spacer(Modifier.height(20.dp))


            }
        }
    }
}