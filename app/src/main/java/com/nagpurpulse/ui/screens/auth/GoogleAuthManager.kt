package com.nagpurpulse.ui.screens.auth



import android.util.Log
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthManager(
    private val context: Context
) {

    suspend fun getGoogleIdToken(): String {

        Log.e("GOOGLE_TEST", "STEP 1 - Entered getGoogleIdToken")

        val googleIdOption =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(
                    "249353068594-0tpbik5v3pl7470gt2ppbqdjq1d7ih12.apps.googleusercontent.com"
                )
                .setAutoSelectEnabled(false)
                .build()

        Log.e("GOOGLE_TEST", "STEP 2 - GoogleIdOption created")

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

        Log.e("GOOGLE_TEST", "STEP 3 - Request created")

        val credentialManager =
            CredentialManager.create(context)

        Log.e("GOOGLE_TEST", "STEP 4 - CredentialManager created")

        val result =
            credentialManager.getCredential(
                context = context,
                request = request
            )

        Log.e("GOOGLE_TEST", "STEP 5 - Credential received")

        val credential = result.credential

        val googleCredential =
            GoogleIdTokenCredential.createFrom(
                credential.data
            )

        Log.e("GOOGLE_TEST", "STEP 6 - Token parsed")

        return googleCredential.idToken
    }
}