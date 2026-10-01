// This is the PrivacySettingsScreen.kt file

// java/com/nagpurpulse/ui/screens/settings/PrivacySettingsScreen.kt

package com.nagpurpulse.ui.screens.settings

import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val showProfile: Boolean = true,
    val showOnlineStatus: Boolean = false,
    val showLastActive: Boolean = false,
    val allowDms: Boolean = true,
    val hideComments: Boolean = false,
    val hidePosts: Boolean = false,
    val hideUpvotes: Boolean = false,
    val hideFromSearch: Boolean = false,
    val incognitoMode: Boolean = false,
    val randomAvatar: Boolean = true,
    val hideLocation: Boolean = true
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    val authRepository: AuthRepository
) : ViewModel() {
    private val _s = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _s

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    android.util.Log.d(
                        "PRIVACY_DEBUG",
                        """
    LOAD SETTINGS
    username=${profile.username}
    hideProfile=${profile.hideProfile}
    """.trimIndent()
                    )

                    _s.value = _s.value.copy(
                        showProfile = !profile.hideProfile,
                        hideComments = profile.hideComments,
                        hidePosts = profile.hidePosts,
                        allowDms = profile.allowDms,
                        showOnlineStatus = profile.showOnlineStatus,
                        incognitoMode = profile.incognitoMode,
                        hideFromSearch = profile.hideFromSearch
                    )
                }
        }
    }

    fun toggle(field: String, value: Boolean) {

        android.util.Log.d(
            "PRIVACY_DEBUG",
            "toggle() field=$field value=$value"
        )

        _s.value = when (field) {
            "showProfile"      -> _s.value.copy(showProfile = value)
            "showOnlineStatus" -> _s.value.copy(showOnlineStatus = value)
            "showLastActive"   -> _s.value.copy(showLastActive = value)
            "allowDms"         -> _s.value.copy(allowDms = value)
            "hideComments"     -> _s.value.copy(hideComments = value)
            "hidePosts"        -> _s.value.copy(hidePosts = value)
            "hideUpvotes"      -> _s.value.copy(hideUpvotes = value)
            "hideFromSearch"   -> _s.value.copy(hideFromSearch = value)
            "incognitoMode"    -> _s.value.copy(incognitoMode = value)
            "randomAvatar"     -> _s.value.copy(randomAvatar = value)
            "hideLocation"     -> _s.value.copy(hideLocation = value)
            else               -> _s.value
        }

        persist()
    }

    private fun persist() {

        val uid = authRepository.currentUserId ?: return

        android.util.Log.d(
            "PRIVACY_DEBUG",
            "persist() uid=$uid showProfile=${_s.value.showProfile}"
        )

        viewModelScope.launch {
            authRepository.updatePrivacySettings(
                userId = uid,
                hideComments = _s.value.hideComments,
                hidePosts = _s.value.hidePosts,
                hideProfile = !_s.value.showProfile,
                allowDms = _s.value.allowDms,
                showOnlineStatus = _s.value.showOnlineStatus,
                incognitoMode = _s.value.incognitoMode,
                hideFromSearch = _s.value.hideFromSearch
            )
        }
    }
}

@Composable
fun PrivacySettingsScreen(navController: NavController, vm: PrivacySettingsViewModel = hiltViewModel()) {
    val s = vm.state.collectAsState().value

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

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Privacy & Visibility",
                            color = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "Manage who can see your activity",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
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
                        .padding(
    DensityManager.cardPadding.dp
),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Control what others can see about you on Nagpur Pulse.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }
            }

            item { SectionHeader("PROFILE VISIBILITY") }

            item {
                SettingsGroup {
                    SettingsRowToggle(
                        "Show Profile to Others",
                        "Allow others to view your profile",
                        Icons.Filled.Visibility,
                        OrangePrimary,
                        s.showProfile
                    ) { vm.toggle("showProfile", it) }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Show Online Status",
                        "Let others see when you're online",
                        Icons.Filled.FiberManualRecord,
                        GreenSuccess,
                        s.showOnlineStatus
                    ) { vm.toggle("showOnlineStatus", it) }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Show Last Active",
                        "Let others see your last active time",
                        Icons.Filled.AccessTime,
                        SecondaryText,
                        s.showLastActive
                    ) { vm.toggle("showLastActive", it) }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Allow Direct Messages",
                        "Let others message you",
                        Icons.AutoMirrored.Filled.Message,
                        BlueInfo,
                        s.allowDms
                    ) { vm.toggle("allowDms", it) }
                }
            }

            item { SectionHeader("CONTENT VISIBILITY") }

            item {
                SettingsGroup {
                    SettingsRowToggle(
                        "Hide My Comments",
                        "Hide all my comments from public view",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        s.hideComments
                    ) { vm.toggle("hideComments", it) }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide My Posts",
                        "Only you can see your posts",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        s.hidePosts
                    ) { vm.toggle("hidePosts", it) }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide Upvotes/Interactions",
                        "Hide my likes, upvotes and reactions",
                        Icons.Filled.VisibilityOff,
                        RedAlert,
                        s.hideUpvotes
                    ) { vm.toggle("hideUpvotes", it) }

                    SettingsDivider()

                    SettingsRowToggle(
                        "Hide from Search Results",
                        "Don't show my profile in search",
                        Icons.Filled.SearchOff,
                        OrangePrimary,
                        s.hideFromSearch
                    ) { vm.toggle("hideFromSearch", it) }
                }
            }

            item { SectionHeader("BLOCKING & RESTRICTIONS") }

            item {
                SettingsGroup {
                    SettingsRow(
                        "Blocked Users",
                        "Manage your blocked users",
                        Icons.Filled.Block,
                        RedAlert
                    ) {}

                    SettingsDivider()

                    SettingsRow(
                        "Muted Users",
                        "Manage muted users",
                        Icons.AutoMirrored.Filled.VolumeOff,
                        SecondaryText
                    ) {}
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

}