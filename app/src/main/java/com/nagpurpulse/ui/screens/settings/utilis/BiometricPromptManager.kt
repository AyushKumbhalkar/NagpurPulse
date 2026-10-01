package com.nagpurpulse.ui.screens.settings.utilis


import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricPromptManager {

    fun show(
        activity: FragmentActivity,
        onSuccess: () -> Unit
    ) {

        val executor =
            ContextCompat.getMainExecutor(activity)

        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        onSuccess()
                    }
                }
            )

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock NagpurPulse")
                .setSubtitle("Verify your identity")
                .setNegativeButtonText("Cancel")
                .build()

        biometricPrompt.authenticate(promptInfo)
    }
}