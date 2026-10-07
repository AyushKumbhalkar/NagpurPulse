//java/com/nagpurpulse/data/repository/AuthRepository.kt

package com.nagpurpulse.data.repository



import com.google.firebase.messaging.FirebaseMessaging
import coil.imageLoader
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.serialization.Serializable
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import com.nagpurpulse.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

enum class UsernameAvailability { AVAILABLE, TAKEN, UNABLE_TO_CHECK }

class EmailConfirmationRequiredException : IllegalStateException(
    "Your account was created. Check your email to confirm your address, then sign in."
)

/** Thrown when Supabase silently accepts a signup for an already-registered email. */
class EmailAlreadyUsedException : IllegalStateException("user already registered")

private const val AUTH_LOG_TAG = "NP_AUTH_FLOW"

class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    val currentUser   get() = client.auth.currentUserOrNull()
    val currentUserId get() = client.auth.currentUserOrNull()?.id

    /**
     * Returns true only for a live Supabase session whose email has been verified.
     *
     * Supabase can persist a session on the device. Checking only currentUser would
     * therefore allow an unverified email account to look authenticated after an app
     * restart. Email verification is part of the authentication boundary, not merely
     * a signup-screen concern.
     */
    fun isLoggedIn(): Boolean {
        val user = client.auth.currentUserOrNull()
        val confirmed = user?.emailConfirmedAt != null
        android.util.Log.d(
            AUTH_LOG_TAG,
            "SESSION_CHECK: sessionUserPresent=${user != null}, emailConfirmed=$confirmed"
        )
        return user != null && confirmed
    }

    /** Server-backed admin check used only for UI routing. Database RLS/RPCs remain authoritative. */
    suspend fun isCurrentUserAdmin(): Boolean {
        if (!isLoggedIn()) return false
        val uid = currentUserId ?: return false
        return try {
            client.postgrest["admin_roles"]
                .select { filter { eq("user_id", uid) } }
                .decodeList<JsonObject>()
                .isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signUp(
        email: String,
        password: String
    ): Result<Unit> {
        android.util.Log.d(AUTH_LOG_TAG, "SIGNUP_START: starting Supabase email/password signup")
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        return try {
            android.util.Log.d(AUTH_LOG_TAG, "SIGNUP_REQUEST: calling Supabase signUpWith(Email)")
            client.auth.signUpWith(Email) { this.email = email; this.password = password }
            val signupUser = client.auth.currentUserOrNull()
            android.util.Log.d(AUTH_LOG_TAG, "SIGNUP_RESPONSE: request completed; sessionUserPresent=${signupUser != null}, emailConfirmed=${signupUser?.emailConfirmedAt != null}, identitiesCount=${signupUser?.identities?.size}")

            // Supabase silently "succeeds" for already-registered emails instead of throwing.
            // The tell-tale sign is that the returned user has an empty identities list.
            // A genuine new signup always has at least one identity entry.
            if (signupUser != null && signupUser.identities?.isEmpty() == true) {
                android.util.Log.w(AUTH_LOG_TAG, "SIGNUP_DUPLICATE: empty identities list detected — email already registered")
                // Clean up the ghost session Supabase created.
                try { client.auth.signOut() } catch (_: Exception) {}
                return Result.failure(EmailAlreadyUsedException())
            }

            // Supabase may return no active session when email confirmation is required.
            // Account creation is still successful in that case; the verification
            // dialog handles the next step.
            if (client.auth.currentUserOrNull() != null) {
                registerFcmTokenForCurrentUser()
            }
            android.util.Log.d(AUTH_LOG_TAG, "SIGNUP_RESULT: signup returned success to ViewModel")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(AUTH_LOG_TAG, "SIGNUP_ERROR: Supabase signup failed type=${e::class.java.simpleName}, message=${e.message}", e)
            Result.failure(e)
        }
    }







    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
            val signedInUser = client.auth.currentUserOrNull()
                ?: return Result.failure(IllegalStateException("Sign-in completed without a user session"))

            // Defense in depth: never keep an email/password session in the app if
            // Supabase reports that the email is still unverified. Normally Supabase
            // rejects this sign-in when Confirm Email is enabled, but this guard also
            // protects the client if the Auth configuration is changed later.
            if (signedInUser.emailConfirmedAt == null) {
                try { client.auth.signOut() } catch (_: Exception) { }
                return Result.failure(EmailConfirmationRequiredException())
            }

            val userId = signedInUser.id
            // Signing in again is the explicit reactivation action for a temporarily deactivated account.
            val profile = client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<Profile>()
            if (profile.isDeactivated) {
                client.postgrest.rpc("reactivate_user_account")
            }
            registerFcmTokenForCurrentUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleToken(
        idToken: String
    ): Result<Unit> {

        return try {

            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                provider = Google
            }

            registerFcmTokenForCurrentUser()
            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            removeCurrentDeviceToken()
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    /** Marks the current profile deactivated on the server, then ends this device's session. */
    suspend fun deactivateAccount(): Result<Unit> {
        return try {
            if (currentUserId == null) {
                return Result.failure(IllegalStateException("Please sign in again to deactivate your account."))
            }
            client.postgrest.rpc("deactivate_user_account")
            // Token cleanup is best-effort; still revoke all refresh sessions if cleanup fails.
            try { removeCurrentDeviceToken() } catch (_: Exception) { }
            client.auth.signOut(io.github.jan.supabase.auth.SignOutScope.GLOBAL)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun registerFcmTokenForCurrentUser() {
        val userId = currentUserId ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            // One row per physical FCM token supports the same account on multiple devices.
            // If this device switches accounts, its token row is reassigned to the signed-in user.
            client.postgrest["device_tokens"].upsert(
                mapOf(
                    "user_id" to userId,
                    "fcm_token" to token,
                    "updated_at" to java.time.Instant.now().toString()
                )
            ) {
                onConflict = "fcm_token"
            }
        } catch (e: Exception) {
            // Push registration must never turn a successful login into a failure.
            android.util.Log.w("FCM_DEBUG", "Could not register device token after authentication")
        }
    }

    private suspend fun removeCurrentDeviceToken() {
        val userId = currentUserId ?: return
        try {
            // Remove only this installation's token. Do not disable push on the
            // user's other signed-in devices.
            val token = FirebaseMessaging.getInstance().token.await()
            client.postgrest["device_tokens"].delete {
                filter {
                    eq("user_id", userId)
                    eq("fcm_token", token)
                }
            }
        } catch (e: Exception) {
            // Continue sign-out even if the device-token cleanup is temporarily unavailable.
            android.util.Log.w("FCM_DEBUG", "Could not remove device token during sign-out")
        }
    }

    suspend fun getCurrentProfile(): Result<Profile> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not logged in"))
            val profile = client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUsernameAvailable(
        username: String
    ): UsernameAvailability {
        return try {
            val result = client.postgrest["profiles"]
                .select {
                    filter {
                        ilike("username", username.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))
                    }
                }
                .decodeList<Profile>()

            val signedInUserId = currentUserId
            if (result.none { profile -> profile.id != signedInUserId }) {
                UsernameAvailability.AVAILABLE
            } else {
                UsernameAvailability.TAKEN
            }
        } catch (_: Exception) {
            UsernameAvailability.UNABLE_TO_CHECK
        }
    }

    suspend fun hasCompletedOnboarding(): Boolean {

        return try {

            val userId = currentUserId
                ?: return false

            val profile =
                client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingle<Profile>()

            // A profile is considered fully onboarded only after the user has
            // completed both required identity fields and selected a profile picture.
            // This lets a verified account safely resume the identity flow after an
            // interrupted onboarding session or app restart.
            profile.username.isNotBlank() && !profile.avatarUrl.isNullOrBlank()

        } catch (_: Exception) {

            false
        }
    }

    suspend fun updateProfile(userId: String, tagline: String? = null, areas: List<String>? = null): Result<Unit> {
        return try {
            if (currentUserId != userId) {
                return Result.failure(SecurityException("You can only update your own profile"))
            }
            val updates = mutableMapOf<String, Any?>()
            tagline?.let { updates["tagline"] = it }
            areas?.let   { updates["areas"]   = it }
            if (updates.isNotEmpty()) {
                client.postgrest["profiles"].update(updates) { filter { eq("id", userId) } }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }


    suspend fun verifySignupEmailOtp(email: String, token: String): Result<Unit> {
        android.util.Log.d(AUTH_LOG_TAG, "OTP_VERIFY_START: validating verification code locally")
        val normalizedEmail = email.trim()
        val normalizedToken = token.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (!normalizedToken.matches(Regex("\\d{6}"))) {
            return Result.failure(IllegalArgumentException("Enter the 6-digit verification code."))
        }
        return try {
            android.util.Log.d(AUTH_LOG_TAG, "OTP_VERIFY_REQUEST: calling Supabase verifyEmailOtp type=SIGNUP")
            client.auth.verifyEmailOtp(
                type = OtpType.Email.SIGNUP,
                email = normalizedEmail,
                token = normalizedToken
            )
            val verifiedUser = client.auth.currentUserOrNull()
            android.util.Log.d(AUTH_LOG_TAG, "OTP_VERIFY_RESPONSE: verification call completed; sessionUserPresent=${verifiedUser != null}, emailConfirmed=${verifiedUser?.emailConfirmedAt != null}")
            registerFcmTokenForCurrentUser()
            android.util.Log.d(AUTH_LOG_TAG, "OTP_VERIFY_RESULT: verification succeeded")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(AUTH_LOG_TAG, "OTP_VERIFY_ERROR: verification failed type=${e::class.java.simpleName}, message=${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun resendSignupEmailOtp(email: String): Result<Unit> {
        android.util.Log.d(AUTH_LOG_TAG, "OTP_RESEND_START: validating email locally")
        val normalizedEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        return try {
            android.util.Log.d(AUTH_LOG_TAG, "OTP_RESEND_REQUEST: calling Supabase resendEmail type=SIGNUP")
            client.auth.resendEmail(OtpType.Email.SIGNUP, normalizedEmail)
            android.util.Log.d(AUTH_LOG_TAG, "OTP_RESEND_RESULT: resend request completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(AUTH_LOG_TAG, "OTP_RESEND_ERROR: resend failed type=${e::class.java.simpleName}, message=${e.message}", e)
            Result.failure(e)
        }
    }

    // Password reset — triggers Supabase email
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val normalizedEmail = email.trim()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        return try {
            client.auth.resetPasswordForEmail(
                email = normalizedEmail,
                redirectUrl = "nagpurpulse://auth"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // ── Guest mode ────────────────────────────────────────────────────────────
    private var _isGuest = false

    private var _guestAvatarUrl: String? = null
    val guestAvatarUrl get() = _guestAvatarUrl
    private var _guestUsername: String? = null
    val guestUsername get() = _guestUsername
    private var _guestGender: String? = null
    val guestGender get() = _guestGender

    val isGuest get() = _isGuest && !isLoggedIn()

    fun enterGuestMode(
        avatarUrl: String? = null,
        username: String? = null,
        gender: String? = null
    ) {
        _isGuest = true
        _guestAvatarUrl = avatarUrl ?: _guestAvatarUrl
        _guestUsername = username ?: _guestUsername
        _guestGender = gender ?: _guestGender
    }

    fun exitGuestMode() {
        _isGuest = false
        _guestAvatarUrl = null
        _guestUsername = null
        _guestGender = null
    }

    /** Returns true if user can perform write actions */
    fun canWrite(): Boolean = isLoggedIn()

    // ── Profile update (full) ─────────────────────────────────────────────────
    suspend fun updateFullProfile(
        userId: String,
        displayName: String? = null,
        username: String? = null,
        bio: String? = null,
        location: String? = null,
        website: String? = null,
        avatarUrl: String? = null,
        coverUrl: String? = null,
        gender: String? = null
    ): Result<Unit> {
        return try {
            val authenticatedUserId = currentUserId
                ?: return Result.failure(IllegalStateException("Please sign in again to save your profile"))
            if (authenticatedUserId != userId) {
                return Result.failure(SecurityException("You can only update your own profile"))
            }

            val m = mutableMapOf<String, String?>()
            displayName?.let { m["display_name"] = it }
            username?.let { m["username"] = it }
            // Gender is onboarding-only and must not be written to the public profile.
            bio?.let { m["tagline"] = it }
            location?.let { m["location"] = it }
            website?.let { m["website"] = it }
            avatarUrl?.let { m["avatar_url"] = it }
            coverUrl?.let { m["cover_url"] = it }

            if (m.isNotEmpty()) {
                client.postgrest["profiles"].update(m) {
                    filter { eq("id", authenticatedUserId) }
                }

                // PostgREST may return success for an update that affected zero rows.
                // Confirm that the authenticated user's profile still exists before
                // reporting onboarding/profile-save success.
                client.postgrest["profiles"]
                    .select {
                        filter { eq("id", authenticatedUserId) }
                    }
                    .decodeSingle<Profile>()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            // Avoid logging profile values, user IDs, or other personal data.
            android.util.Log.w("PROFILE_SAVE", "Profile update failed")
            Result.failure(e)
        }
    }

    suspend fun createProfileIfMissing(
        userId: String,
        username: String,
        avatarUrl: String?,
        gender: String?
    ): Result<Unit> {

        return try {

            if (currentUserId != userId) {
                return Result.failure(SecurityException("You can only create your own profile"))
            }

            val existing =
                client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeList<Profile>()

            if (existing.isEmpty()) {

                client.postgrest["profiles"]
                    .insert(
                        mapOf(
                            "id" to userId,
                            "username" to username,
                            "display_name" to username,
                            "avatar_url" to avatarUrl,
                            "karma" to 0
                        )
                    )
            }

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ── Privacy settings ──────────────────────────────────────────────────────
    suspend fun updatePrivacySettings(
        userId: String,
        hideComments: Boolean? = null,
        hidePosts: Boolean? = null,
        hideProfile: Boolean? = null,
        allowDms: Boolean? = null,
        showOnlineStatus: Boolean? = null,
        incognitoMode: Boolean? = null,
        hideFromSearch: Boolean? = null
    ): Result<Unit> {
        return try {
            if (currentUserId != userId) {
                return Result.failure(SecurityException("You can only update your own privacy settings"))
            }
            val m = mutableMapOf<String, Boolean>()
            hideComments?.let    { m["hide_comments"]      = it }
            hidePosts?.let       { m["hide_posts"]         = it }
            hideProfile?.let     { m["hide_profile"]       = it }
            allowDms?.let        { m["allow_dms"]          = it }
            showOnlineStatus?.let{ m["show_online_status"] = it }
            incognitoMode?.let   { m["incognito_mode"]     = it }
            hideFromSearch?.let  { m["hide_from_search"]   = it }
            if (m.isNotEmpty()) {
                client.postgrest["profiles"].update(m) {
                    filter { eq("id", userId) }
                }
            }

            // An update request can succeed without changing a row (for example,
            // when profile RLS or a missing row blocks the write). Read the saved
            // profile back before telling the UI that privacy changes were saved.
            val saved = client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<Profile>()

            val matches = (hideComments == null || saved.hideComments == hideComments) &&
                (hidePosts == null || saved.hidePosts == hidePosts) &&
                (hideProfile == null || saved.hideProfile == hideProfile) &&
                (allowDms == null || saved.allowDms == allowDms) &&
                (showOnlineStatus == null || saved.showOnlineStatus == showOnlineStatus) &&
                (incognitoMode == null || saved.incognitoMode == incognitoMode) &&
                (hideFromSearch == null || saved.hideFromSearch == hideFromSearch)

            if (!matches) {
                return Result.failure(IllegalStateException("Privacy settings could not be verified after saving"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(
        userId: String,
        bytes: ByteArray
    ): Result<String> {

        return try {

            val path = "avatars/$userId.jpg"

            client.storage
                .from("profile-images")
                .upload(
                    path = path,
                    data = bytes
                ) {
                    upsert = true
                }

            val url =
                client.storage
                    .from("profile-images")
                    .publicUrl(path)

            Result.success(url)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Change email ──────────────────────────────────────────────────────────
    suspend fun changeEmail(newEmail: String): Result<Unit> {
        return try {
            client.auth.updateUser { email = newEmail }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // Change password only after proving knowledge of the current password.
    suspend fun changePasswordWithCurrentPassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val email = client.auth.currentUserOrNull()?.email
                ?: return Result.failure(IllegalStateException("Your account email is unavailable. Use the email reset option."))
            if (currentPassword.isBlank() || newPassword.length < 8) {
                return Result.failure(IllegalArgumentException("Enter your current password and a new password with at least 8 characters."))
            }
            // Re-authentication prevents a stale unlocked session alone from changing credentials.
            client.auth.signInWith(Email) {
                this.email = email
                this.password = currentPassword
            }
            client.auth.updateUser { password = newPassword }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(IllegalStateException("Password change failed. Check your current password and try again."))
        }
    }

    // Revoke refresh sessions for this user on all devices; callers must still handle failure.
    suspend fun signOutEverywhere(): Result<Unit> {
        return try {
            removeCurrentDeviceToken()
            client.auth.signOut(io.github.jan.supabase.auth.SignOutScope.GLOBAL)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Change password ───────────────────────────────────────────────────────
    suspend fun changePassword(newPassword: String): Result<Unit> {
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        return try {
            client.auth.updateUser { password = newPassword }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Delete account ────────────────────────────────────────────────────────
    suspend fun deleteAccount(): Result<Unit> {
        return try {

            val userId = currentUserId
                ?: return Result.failure(
                    Exception("User not logged in")
                )

            client.postgrest.rpc(
                "delete_user_account",
                buildJsonObject {
                    put("target_user_id", userId)
                }
            )

            client.auth.signOut()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    // ── Clear cache (SharedPrefs) ─────────────────────────────────────────────
    fun clearLocalCache(context: android.content.Context) {
        context.cacheDir.deleteRecursively()
        // Clear Coil image cache
        try {
            context.imageLoader.memoryCache?.clear()
        } catch (_: Exception) {}
    }

    suspend fun uploadProfileImage(
        userId: String,
        bytes: ByteArray,
        isBanner: Boolean = false
    ): Result<String> {

        return try {

            val path =
                if (isBanner)
                    "banners/$userId.jpg"
                else
                    "avatars/$userId.jpg"

            client.storage
                .from("profile-images")
                .upload(
                    path = path,
                    data = bytes
                ) {
                    upsert = true
                }

            val publicUrl =
                client.storage
                    .from("profile-images")
                    .publicUrl(path)

            Result.success(publicUrl)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}
