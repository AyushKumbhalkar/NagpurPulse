package com.nagpurpulse.ui.screens.settings.utilis


import android.content.Context
import androidx.biometric.BiometricManager

object BiometricHelper {

    fun isAvailable(context: Context): Boolean {

        return BiometricManager.from(context)
            .canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}