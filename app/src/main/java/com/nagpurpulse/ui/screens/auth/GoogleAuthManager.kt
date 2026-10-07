package com.nagpurpulse.ui.screens.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.nagpurpulse.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import com.nagpurpulse.R

/** What happened when the user tried to sign in with Google. */
sealed interface GoogleSignInOutcome {
    /**
     * [rawNonce] must be sent to Supabase together with [idToken]; Google embeds
     * the SHA-256 of it in the token, which stops a stolen token being replayed.
     */
    data class Success(val idToken: String, val rawNonce: String) : GoogleSignInOutcome

    /** User closed the Google sheet. Not an error - show nothing. */
    object Cancelled : GoogleSignInOutcome

    data class Failure(val message: String) : GoogleSignInOutcome
}

class GoogleAuthManager(
    private val context: Context
) {

    suspend fun signIn(): GoogleSignInOutcome {
        if (!isOnline()) {
            return GoogleSignInOutcome.Failure(
                context.getString(R.string.google_err_no_internet)
            )
        }

        val rawNonce = generateRawNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .setNonce(sha256Hex(rawNonce))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = CredentialManager.create(context).getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInOutcome.Success(
                    idToken = googleCredential.idToken,
                    rawNonce = rawNonce
                )
            } else {
                GoogleSignInOutcome.Failure(context.getString(R.string.auth_err_google_failed))
            }
        } catch (e: CancellationException) {
            // Never swallow coroutine cancellation (screen closed, etc.).
            throw e
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInOutcome.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInOutcome.Failure(
                context.getString(R.string.google_err_no_account)
            )
        } catch (_: GetCredentialProviderConfigurationException) {
            GoogleSignInOutcome.Failure(
                context.getString(R.string.google_err_unavailable)
            )
        } catch (_: GetCredentialInterruptedException) {
            GoogleSignInOutcome.Failure(context.getString(R.string.google_err_interrupted))
        } catch (_: GoogleIdTokenParsingException) {
            GoogleSignInOutcome.Failure(context.getString(R.string.auth_err_google_failed))
        } catch (_: GetCredentialException) {
            GoogleSignInOutcome.Failure(
                if (isOnline()) context.getString(R.string.auth_err_google_failed)
                else context.getString(R.string.google_err_no_internet)
            )
        } catch (_: Exception) {
            GoogleSignInOutcome.Failure(context.getString(R.string.auth_err_google_failed))
        }
    }

    private fun generateRawNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** Best effort. If the check itself fails we assume online and let the real call decide. */
    private fun isOnline(): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)
}
