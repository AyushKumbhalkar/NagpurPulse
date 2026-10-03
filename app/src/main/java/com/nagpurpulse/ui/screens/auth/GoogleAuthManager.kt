package com.nagpurpulse.ui.screens.auth



import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthManager(
    private val context: Context
) {

    suspend fun getGoogleIdToken(): String {

        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(
                    "249353068594-0tpbik5v3pl7470gt2ppbqdjq1d7ih12.apps.googleusercontent.com"
                )
                .setAutoSelectEnabled(false)
                .build()

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

        val credentialManager =
            CredentialManager.create(context)

        val result =
            credentialManager.getCredential(
                context = context,
                request = request
            )

        val credential = result.credential

        val googleCredential =
            GoogleIdTokenCredential.createFrom(
                credential.data
            )

        return googleCredential.idToken
    }
}