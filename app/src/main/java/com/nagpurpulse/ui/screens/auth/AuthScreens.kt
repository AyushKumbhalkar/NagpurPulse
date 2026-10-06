package com.nagpurpulse.ui.screens.auth

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.EmailConfirmationRequiredException
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.TextPrimary
import com.nagpurpulse.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val forgotPasswordSent: Boolean = false,
    val infoMessage: String? = null
)


private fun safeAuthError(e: Throwable, fallback: String): String {
    val message = e.message?.lowercase().orEmpty()
    return when {
        message.contains("invalid login") || message.contains("invalid credentials") ||
            message.contains("email not confirmed") || message.contains("invalid password") ->
            "Invalid email or password."
        message.contains("rate limit") || message.contains("too many requests") ->
            "Too many attempts. Please wait a moment and try again."
        message.contains("network") || message.contains("timeout") ->
            "Unable to connect right now. Check your internet connection and try again."
        else -> fallback
    }
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun showError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    fun signUp(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        if (password.length < 8) {
            _uiState.value = AuthUiState(error = "Password must be at least 8 characters.")
            return
        }
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signUp(email.trim(), password).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = if (e is EmailConfirmationRequiredException) {
                        AuthUiState(infoMessage = e.message)
                    } else {
                        AuthUiState(error = safeAuthError(e, "Unable to create your account. Please try again."))
                    }
                }
            )
        }
    }

    fun verifySignupEmailOtp(email: String, token: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.verifySignupEmailOtp(email.trim(), token.trim()).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState(error = safeAuthError(e, "Invalid verification code. Please check the code and try again."))
                }
            )
        }
    }

    fun resendSignupEmailOtp(email: String, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.resendSignupEmailOtp(email.trim()).fold(
                onSuccess = {
                    _uiState.value = AuthUiState()
                    onComplete(null)
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState(error = safeAuthError(e, "Could not resend the verification code. Please try again later."))
                    onComplete(safeAuthError(e, "Could not resend the verification code. Please try again later."))
                }
            )
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.signIn(email, password).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                    onSuccess()
                },
                onFailure = { e -> _uiState.value = AuthUiState(error = safeAuthError(e, "Unable to sign in. Please try again.")) }
            )
        }
    }

    fun signInWithGoogleToken(
        idToken: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)

            authRepository.signInWithGoogleToken(idToken)
                .fold(
                    onSuccess = {
                        _uiState.value = AuthUiState(isSuccess = true)
                        onSuccess()
                    },
                    onFailure = { e ->
                        _uiState.value =
                            AuthUiState(
                                error = safeAuthError(e, "Google sign-in failed. Please try again.")
                            )
                    }
                )
        }
    }


    fun sendPasswordReset(email: String) {
        val normalizedEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            _uiState.value = AuthUiState(error = "Enter a valid email address first")
            return
        }
        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)
            authRepository.sendPasswordReset(normalizedEmail).fold(
                onSuccess = { _uiState.value = AuthUiState(forgotPasswordSent = true) },
                onFailure = { e -> _uiState.value = AuthUiState(error = safeAuthError(e, "Unable to send the reset email. Please try again later.")) }
            )
        }
    }


    fun signInWithGoogleToken(
        idToken: String,
        onExistingUser: () -> Unit,
        onNewUser: () -> Unit
    ) {

        viewModelScope.launch {
            if (_uiState.value.isLoading) return@launch
            _uiState.value = AuthUiState(isLoading = true)

            authRepository
                .signInWithGoogleToken(idToken)
                .fold(

                    onSuccess =
                        {

                        val completed =
                            authRepository
                                .hasCompletedOnboarding()

                        _uiState.value =
                            AuthUiState(isSuccess = true)

                        if (completed) {
                            onExistingUser()

                        } else {
                            onNewUser()
                        }
                    },

                    onFailure = { e ->

                        _uiState.value =
                            AuthUiState(
                                error = safeAuthError(e, "Google sign-in failed. Please try again.")
                            )
                    }
                )
        }
    }
}

// ─── Shared text-field colors ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = OrangePrimary,
    unfocusedBorderColor    = Color(0xFF2A2A2A),
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
    cursorColor             = OrangePrimary,
    focusedContainerColor   = Color(0xFF161616),
    unfocusedContainerColor = Color(0xFF141414),
    focusedLabelColor       = OrangePrimary,
    unfocusedLabelColor     = TextSecondary
)









