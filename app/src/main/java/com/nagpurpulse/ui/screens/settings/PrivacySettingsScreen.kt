package com.nagpurpulse.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class PrivacyUiState(
    val showProfile: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val allowDms: Boolean = true,
    val hideComments: Boolean = false,
    val hidePosts: Boolean = false,
    val hideFromSearch: Boolean = false,
    val incognitoMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state
    private val saveMutex = Mutex()
    private var lastSaved = PrivacyUiState(isLoading = false)

    init { loadSettings() }

    fun dismissError() { _state.value = _state.value.copy(errorMessage = null) }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            authRepository.getCurrentProfile().fold(
                onSuccess = { profile ->
                    val loaded = PrivacyUiState(
                        showProfile = !profile.hideProfile,
                        showOnlineStatus = profile.showOnlineStatus,
                        allowDms = profile.allowDms,
                        hideComments = profile.hideComments,
                        hidePosts = profile.hidePosts,
                        hideFromSearch = profile.hideFromSearch,
                        incognitoMode = profile.incognitoMode,
                        isLoading = false
                    )
                    lastSaved = loaded
                    _state.value = loaded
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Privacy settings couldn't be loaded. Check your connection and retry."
                    )
                }
            )
        }
    }

    fun retryLoad() = loadSettings()

    fun toggle(field: String, value: Boolean) {
        val updated = when (field) {
            "showProfile" -> _state.value.copy(showProfile = value, errorMessage = null)
            "showOnlineStatus" -> _state.value.copy(showOnlineStatus = value, errorMessage = null)
            "allowDms" -> _state.value.copy(allowDms = value, errorMessage = null)
            "hideComments" -> _state.value.copy(hideComments = value, errorMessage = null)
            "hidePosts" -> _state.value.copy(hidePosts = value, errorMessage = null)
            "hideFromSearch" -> _state.value.copy(hideFromSearch = value, errorMessage = null)
            "incognitoMode" -> _state.value.copy(incognitoMode = value, errorMessage = null)
            else -> return
        }
        _state.value = updated
        persistLatest()
    }

    private fun persistLatest() {
        val userId = authRepository.currentUserId ?: run {
            _state.value = _state.value.copy(errorMessage = "Please sign in again to save privacy settings.")
            return
        }
        viewModelScope.launch {
            saveMutex.withLock {
                val attempted = _state.value.copy(isLoading = false, isSaving = true, errorMessage = null)
                _state.value = _state.value.copy(isSaving = true)
                val result = authRepository.updatePrivacySettings(
                    userId = userId,
                    hideComments = attempted.hideComments,
                    hidePosts = attempted.hidePosts,
                    hideProfile = !attempted.showProfile,
                    allowDms = attempted.allowDms,
                    showOnlineStatus = attempted.showOnlineStatus,
                    incognitoMode = attempted.incognitoMode,
                    hideFromSearch = attempted.hideFromSearch
                )
                if (result.isSuccess) {
                    lastSaved = attempted.copy(isSaving = false)
                    _state.value = _state.value.copy(isSaving = false)
                } else {
                    val current = _state.value
                    _state.value = if (
                        current.showProfile == attempted.showProfile &&
                        current.showOnlineStatus == attempted.showOnlineStatus &&
                        current.allowDms == attempted.allowDms &&
                        current.hideComments == attempted.hideComments &&
                        current.hidePosts == attempted.hidePosts &&
                        current.hideFromSearch == attempted.hideFromSearch &&
                        current.incognitoMode == attempted.incognitoMode
                    ) {
                        lastSaved.copy(isLoading = false, isSaving = false,
                            errorMessage = "Couldn't save that change. Your last saved settings were restored.")
                    } else {
                        current.copy(isSaving = false,
                            errorMessage = "A privacy change couldn't be saved. Please try again.")
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsScreen(
    navController: NavController,
    vm: PrivacySettingsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.errorMessage) {
        s.errorMessage?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 120f))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Privacy & Safety", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Control what others can see", color = SecondaryText, fontSize = 12.sp)
                    }
                    if (s.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = OrangePrimary)
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { padding ->
        when {
            s.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            s.errorMessage != null && s.showProfile && s.isLoading.not() -> {
                // Keep the settings UI available; the snackbar reports errors and retry is explicit below.
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    item {
                        SettingsGroup {
                            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                                Text("Couldn't load privacy settings", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text("Your saved settings may not be current. Retry before changing them.", color = SecondaryText)
                                TextButton(onClick = { vm.retryLoad() }) { Text("Retry", color = OrangePrimary) }
                            }
                        }
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Surface).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Your privacy, your choice", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                            Text("Only options backed by your account profile are shown here.", color = SecondaryText, fontSize = 12.sp)
                        }
                    }
                }
                item { SectionHeader("PROFILE VISIBILITY") }
                item {
                    SettingsGroup {
                        SettingsRowToggle("Show Profile to Others", "Allow people to open your profile",
                            Icons.Filled.Visibility, OrangePrimary, s.showProfile) { vm.toggle("showProfile", it) }
                        SettingsDivider()
                        SettingsRowToggle("Show Online Status", "Let others see when you're online",
                            Icons.Filled.FiberManualRecord, GreenSuccess, s.showOnlineStatus) { vm.toggle("showOnlineStatus", it) }
                        SettingsDivider()
                        SettingsRowToggle("Allow Direct Messages", "Allow other users to message you",
                            Icons.Filled.Message, BlueInfo, s.allowDms) { vm.toggle("allowDms", it) }
                        SettingsDivider()
                        SettingsRowToggle("Hide From Search", "Exclude your profile from in-app search",
                            Icons.Filled.Search, PurpleNight, s.hideFromSearch) { vm.toggle("hideFromSearch", it) }
                    }
                }
                item { SectionHeader("CONTENT VISIBILITY") }
                item {
                    SettingsGroup {
                        SettingsRowToggle("Hide My Posts", "Hide your posts from other users where supported",
                            Icons.Filled.Article, RedAlert, s.hidePosts) { vm.toggle("hidePosts", it) }
                        SettingsDivider()
                        SettingsRowToggle("Hide My Comments", "Hide your comments from other users where supported",
                            Icons.Filled.Comment, RedAlert, s.hideComments) { vm.toggle("hideComments", it) }
                    }
                }
                item { SectionHeader("INCOGNITO") }
                item {
                    SettingsGroup {
                        SettingsRowToggle("Incognito Mode", "Save this preference to your account",
                            Icons.Filled.VisibilityOff, PurpleNight, s.incognitoMode) { vm.toggle("incognitoMode", it) }
                    }
                }
                item {
                    Text(
                        "Note: these preferences are saved to your profile. Their effect across feeds, search, and public profiles also depends on the app's query and visibility rules.",
                        color = SecondaryText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
