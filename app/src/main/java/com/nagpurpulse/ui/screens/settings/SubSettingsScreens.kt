package com.nagpurpulse.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
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
import javax.inject.Inject

@Composable
fun IncognitoSettingsScreen(
    navController: NavController,
    vm: PrivacySettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); vm.dismissError() }
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(Brush.verticalGradient(listOf(Surface, Background), 0f, 120f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Incognito Settings", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Manage saved visibility controls", color = SecondaryText, fontSize = 12.sp)
                    }
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = OrangePrimary, strokeWidth = 2.dp)
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (state.loadFailed) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                SettingsGroup {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("Couldn't load your privacy settings", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Retry before changing incognito options so your saved values are not overwritten.", color = SecondaryText)
                        TextButton(onClick = { vm.retryLoad() }) { Text("Retry", color = OrangePrimary) }
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = PurpleNight, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Privacy without false promises", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(
                            "These switches save to your profile. Some app surfaces may need separate visibility enforcement.",
                            color = SecondaryText, fontSize = 12.sp
                        )
                    }
                }
                item { SectionHeader("INCOGNITO MODE") }
                item {
                    SettingsGroup {
                        SettingsRowToggle(
                            "Enable Incognito Mode", "Save incognito mode to your account",
                            Icons.Filled.VisibilityOff, PurpleNight, state.incognitoMode
                        ) { vm.toggle("incognitoMode", it) }
                        SettingsDivider()
                        SettingsRowToggle(
                            "Hide Profile", "Prevent other users from viewing your profile where enforced",
                            Icons.Filled.VisibilityOff, OrangePrimary, !state.showProfile
                        ) { vm.toggle("showProfile", !it) }
                        SettingsDivider()
                        SettingsRowToggle(
                            "Hide From Search", "Exclude your profile from in-app search where enforced",
                            Icons.Filled.VisibilityOff, BlueInfo, state.hideFromSearch
                        ) { vm.toggle("hideFromSearch", it) }
                    }
                }
                item { SectionHeader("CONTENT VISIBILITY") }
                item {
                    SettingsGroup {
                        SettingsRowToggle(
                            "Hide My Posts", "Hide your posts where feed/profile queries respect this setting",
                            Icons.Filled.VisibilityOff, RedAlert, state.hidePosts
                        ) { vm.toggle("hidePosts", it) }
                        SettingsDivider()
                        SettingsRowToggle(
                            "Hide My Comments", "Hide your comments where supported",
                            Icons.Filled.VisibilityOff, RedAlert, state.hideComments
                        ) { vm.toggle("hideComments", it) }
                    }
                }
            }
        }
    }
}

data class SecurityUiState(
    val isSendingReset: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SecurityUiState())
    val state: StateFlow<SecurityUiState> = _state

    fun dismissMessage() { _state.value = _state.value.copy(message = null, error = null) }

    fun sendPasswordReset(email: String) {
        val normalized = email.trim()
        if (normalized.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            _state.value = _state.value.copy(error = "Enter a valid email address.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSendingReset = true, message = null, error = null)
            authRepository.sendPasswordReset(normalized).fold(
                onSuccess = { _state.value = _state.value.copy(isSendingReset = false, message = "If this email belongs to an account, password-reset instructions will arrive shortly.") },
                onFailure = { _state.value = _state.value.copy(isSendingReset = false, error = "Couldn't request a password reset. Check your connection and try again.") }
            )
        }
    }
}

@Composable
fun SecuritySettingsScreen(
    navController: NavController,
    vm: SecuritySettingsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val message = state.error ?: state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.dismissMessage()
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isSendingReset) showPasswordDialog = false },
            title = { Text("Reset password", color = PrimaryText) },
            text = {
                Column {
                    Text("We'll send password-reset instructions to your email address.", color = SecondaryText)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email address") }, singleLine = true,
                        enabled = !state.isSendingReset
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isSendingReset,
                    onClick = {
                        vm.sendPasswordReset(email)
                        showPasswordDialog = false
                    }
                ) { Text(if (state.isSendingReset) "Sending…" else "Send reset email", color = OrangePrimary) }
            },
            dismissButton = { TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel", color = SecondaryText) } },
            containerColor = Surface
        )
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text("Not available yet", color = PrimaryText) },
            text = { Text(message, color = SecondaryText) },
            confirmButton = { TextButton(onClick = { infoMessage = null }) { Text("Got it", color = OrangePrimary) } },
            containerColor = Surface
        )
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(Brush.verticalGradient(listOf(Surface, Background), 0f, 120f)).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Security", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Account protection and sessions", color = SecondaryText, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionHeader("ACCOUNT SECURITY") }
            item {
                SettingsGroup {
                    SettingsRow("Reset Password", "Email yourself a secure password-reset link", Icons.Filled.Lock, OrangePrimary) {
                        email = ""
                        showPasswordDialog = true
                    }
                    SettingsDivider()
                    SettingsRow("Biometric Login", "Requires device biometric integration and login-gate enforcement", Icons.Filled.Fingerprint, BlueInfo) {
                        infoMessage = "Biometric login is not enabled in the authentication flow yet. This screen will not save a pretend enabled state."
                    }
                    SettingsDivider()
                    SettingsRow("Two-Factor Authentication", "Requires a configured server-side second factor", Icons.Filled.Security, GreenSuccess) {
                        infoMessage = "Two-factor authentication is not configured in the current backend flow, so it cannot safely be enabled from this switch."
                    }
                }
            }
            item { SectionHeader("SESSIONS") }
            item {
                SettingsGroup {
                    SettingsRow("Active Devices", "Device-session management is not connected yet", Icons.Filled.Devices, PurpleNight) {
                        infoMessage = "The app does not yet have a verified session-management API to list and revoke all sessions."
                    }
                    SettingsDivider()
                    SettingsRow("Sign Out Everywhere", "Revoke sessions on all devices", Icons.Filled.Logout, RedAlert) {
                        infoMessage = "Signing out everywhere requires a trusted server-side session-revocation endpoint. Only local sign-out is currently available."
                    }
                }
            }
        }
    }
}
