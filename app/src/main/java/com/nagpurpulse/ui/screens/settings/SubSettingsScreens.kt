package com.nagpurpulse.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.biometric.BiometricManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                        Text("Incognito Mode", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Incognito is a preference, not guaranteed anonymity", color = SecondaryText, fontSize = 12.sp)
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
                        Text("What Incognito means", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Incognito currently saves your preference. It does not automatically anonymize posts or comments, hide your identity from moderators, or guarantee invisibility. Use Privacy & Safety for the individual visibility controls.",
                            color = SecondaryText, fontSize = 13.sp
                        )
                    }
                }
                item { SectionHeader("INCognito STATUS") }
                item {
                    SettingsGroup {
                        SettingsRowToggle(
                            "Incognito Mode", "Save this preference to your account; this alone does not anonymize content",
                            Icons.Filled.VisibilityOff, PurpleNight, state.incognitoMode
                        ) { vm.toggle("incognitoMode", it) }
                    }
                }
            }
        }
    }
}

data class SecurityUiState(
    val isSendingReset: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isSigningOutEverywhere: Boolean = false,
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

    fun changePassword(current: String, newPassword: String, confirm: String) {
        if (newPassword.length < 8) {
            _state.value = _state.value.copy(error = "New password must be at least 8 characters.")
            return
        }
        if (newPassword != confirm) {
            _state.value = _state.value.copy(error = "New passwords do not match.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isChangingPassword = true, message = null, error = null)
            authRepository.changePasswordWithCurrentPassword(current, newPassword).fold(
                onSuccess = { _state.value = _state.value.copy(isChangingPassword = false, message = "Password changed successfully.") },
                onFailure = { _state.value = _state.value.copy(isChangingPassword = false, error = it.message ?: "Password change failed.") }
            )
        }
    }

    fun signOutEverywhere(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSigningOutEverywhere = true, message = null, error = null)
            authRepository.signOutEverywhere().fold(
                onSuccess = { _state.value = _state.value.copy(isSigningOutEverywhere = false, message = "Signed out on all devices."); onSuccess() },
                onFailure = { _state.value = _state.value.copy(isSigningOutEverywhere = false, error = "Couldn't revoke sessions. Check your connection and try again.") }
            )
        }
    }

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
    var showCurrentPasswordDialog by remember { mutableStateOf(false) }
    var showSignOutEverywhereDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val securityPrefs = remember { context.getSharedPreferences("security", android.content.Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(securityPrefs.getBoolean("biometric_enabled", false)) }
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }
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
                        enabled = !state.isSendingReset,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = Divider,
                            focusedTextColor = Color(0xFFF7F3F5),
                            unfocusedTextColor = Color(0xFFF7F3F5),
                            cursorColor = OrangePrimary,
                            focusedLabelColor = OrangePrimary,
                            unfocusedLabelColor = SecondaryText,
                            focusedContainerColor = Color(0xFF201B22),
                            unfocusedContainerColor = Color(0xFF201B22)
                        )
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
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),)
    }

    if (showCurrentPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isChangingPassword) showCurrentPasswordDialog = false },
            title = { Text("Change password", color = PrimaryText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Confirm your current password, then choose a new one.", color = SecondaryText)
                    OutlinedTextField(currentPassword, { currentPassword = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Divider, focusedTextColor = Color(0xFFF7F3F5), unfocusedTextColor = Color(0xFFF7F3F5), cursorColor = OrangePrimary, focusedLabelColor = OrangePrimary, unfocusedLabelColor = SecondaryText, focusedContainerColor = Color(0xFF201B22), unfocusedContainerColor = Color(0xFF201B22)), label = { Text("Current password") }, singleLine = true, visualTransformation = if (showCurrentPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) { Icon(if (showCurrentPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = SecondaryText) } })
                    OutlinedTextField(newPassword, { newPassword = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Divider, focusedTextColor = Color(0xFFF7F3F5), unfocusedTextColor = Color(0xFFF7F3F5), cursorColor = OrangePrimary, focusedLabelColor = OrangePrimary, unfocusedLabelColor = SecondaryText, focusedContainerColor = Color(0xFF201B22), unfocusedContainerColor = Color(0xFF201B22)), label = { Text("New password (8+ characters)") }, singleLine = true, visualTransformation = if (showNewPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showNewPassword = !showNewPassword }) { Icon(if (showNewPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = SecondaryText) } })
                    OutlinedTextField(confirmPassword, { confirmPassword = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, unfocusedBorderColor = Divider, focusedTextColor = Color(0xFFF7F3F5), unfocusedTextColor = Color(0xFFF7F3F5), cursorColor = OrangePrimary, focusedLabelColor = OrangePrimary, unfocusedLabelColor = SecondaryText, focusedContainerColor = Color(0xFF201B22), unfocusedContainerColor = Color(0xFF201B22)), label = { Text("Confirm new password") }, singleLine = true, visualTransformation = if (showConfirmPassword) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) { Icon(if (showConfirmPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = SecondaryText) } })
                }
            },
            confirmButton = { TextButton(enabled = !state.isChangingPassword, onClick = { vm.changePassword(currentPassword, newPassword, confirmPassword); showCurrentPasswordDialog = false; currentPassword = ""; newPassword = ""; confirmPassword = "" }) { Text(if (state.isChangingPassword) "Updating…" else "Change password", color = OrangePrimary) } },
            dismissButton = { TextButton(onClick = { showCurrentPasswordDialog = false }) { Text("Cancel", color = SecondaryText) } },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),)
    }
    if (showSignOutEverywhereDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isSigningOutEverywhere) showSignOutEverywhereDialog = false },
            title = { Text("Sign out everywhere?", color = PrimaryText) },
            text = { Text("This revokes your sign-in sessions on other devices too. You may need to sign in again on this device.", color = SecondaryText) },
            confirmButton = { TextButton(enabled = !state.isSigningOutEverywhere, onClick = { vm.signOutEverywhere { navController.navigate(com.nagpurpulse.ui.navigation.Screen.Login.route) { popUpTo(navController.graph.id) { inclusive = true } } }; showSignOutEverywhereDialog = false }) { Text(if (state.isSigningOutEverywhere) "Signing out…" else "Sign out everywhere", color = RedAlert) } },
            dismissButton = { TextButton(onClick = { showSignOutEverywhereDialog = false }) { Text("Cancel", color = SecondaryText) } },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),)
    }

    infoMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text("Not available yet", color = PrimaryText) },
            text = { Text(message, color = SecondaryText) },
            confirmButton = { TextButton(onClick = { infoMessage = null }) { Text("Got it", color = OrangePrimary) } },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),)
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
                    SettingsRow("Change Password", "Use your current password to choose a new one", Icons.Filled.Lock, OrangePrimary) {
                        showCurrentPasswordDialog = true
                    }
                    SettingsDivider()
                    SettingsRow("Reset via Email", "Send a secure password-reset link to your inbox", Icons.Filled.Email, BlueInfo) {
                        email = ""
                        showPasswordDialog = true
                    }
                    SettingsDivider()
                    SettingsRowToggle(
                        "Biometric App Lock",
                        if (biometricAvailable) "Require fingerprint or face unlock when opening NagpurPulse" else "Set up device biometrics or screen lock to enable",
                        Icons.Filled.Fingerprint,
                        BlueInfo,
                        biometricEnabled,
                        onCheckedChange = { enabled ->
                            if (!biometricAvailable && enabled) {
                                infoMessage = "Biometric authentication is unavailable. Set up a supported biometric or device screen lock first."
                            } else if (enabled) {
                                // Enable only after an explicit successful biometric verification.
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity == null) {
                                    infoMessage = "Biometric verification could not be started on this screen."
                                } else {
                                    val prompt = androidx.biometric.BiometricPrompt(
                                        activity,
                                        androidx.core.content.ContextCompat.getMainExecutor(context),
                                        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                                                securityPrefs.edit().putBoolean("biometric_enabled", true).apply()
                                                biometricEnabled = true
                                            }
                                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                infoMessage = "Biometric lock was not enabled."
                                            }
                                        }
                                    )
                                    prompt.authenticate(androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Enable NagpurPulse app lock")
                                        .setSubtitle("Verify your identity to enable biometric app lock")
                                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                                        .setNegativeButtonText("Cancel")
                                        .build())
                                }
                            } else {
                                securityPrefs.edit().putBoolean("biometric_enabled", false).apply()
                                biometricEnabled = false
                            }
                        }
                    )
                    // Two-factor authentication is intentionally hidden until server-side MFA is configured.
                    /*
                    SettingsDivider()
                    SettingsRow("Two-Factor Authentication", "Server-side MFA (not implemented yet)", Icons.Filled.Security, GreenSuccess) { }
                    */
                }
            }
            item { SectionHeader("SESSIONS") }
            item {
                SettingsGroup {
                    // Active Devices is intentionally hidden until a trusted session inventory API exists.
                    /*
                    SettingsRow("Active Devices", "Manage signed-in devices", Icons.Filled.Devices, PurpleNight) { }
                    SettingsDivider()
                    */
                    SettingsRow("Sign Out Everywhere", "Revoke sign-in sessions on all devices", Icons.Filled.Logout, RedAlert) {
                        showSignOutEverywhereDialog = true
                    }
                }
            }
        }
    }
}
