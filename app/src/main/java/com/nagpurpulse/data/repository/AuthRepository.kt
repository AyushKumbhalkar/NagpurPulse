//java/com/nagpurpulse/data/repository/AuthRepository.kt

package com.nagpurpulse.data.repository



import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
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
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

enum class UsernameAvailability { AVAILABLE, TAKEN, UNABLE_TO_CHECK }

class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    val currentUser   get() = client.auth.currentUserOrNull()
    val currentUserId get() = client.auth.currentUserOrNull()?.id

    fun isLoggedIn(): Boolean = client.auth.currentUserOrNull() != null

    suspend fun signUp(
        email: String,
        password: String
    ): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) { this.email = email; this.password = password }
            client.auth.currentUserOrNull()?.id
                ?: return Result.failure(
                    IllegalStateException(
                        "Your account may have been created, but email confirmation is required before you can continue. Check your inbox, then sign in."
                    )
                )
            // The database trigger on auth.users already creates the profiles row.
            // Do not insert it again here: profiles.id is the primary key.
            registerFcmTokenForCurrentUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }







    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) { this.email = email; this.password = password }
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
            // This schema currently stores one token per user, so this removes
            // that user's registered token. Multi-device support needs a schema change.
            removeCurrentDeviceToken()
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private suspend fun registerFcmTokenForCurrentUser() {
        val userId = currentUserId ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            client.postgrest["device_tokens"].upsert(
                mapOf(
                    "user_id" to userId,
                    "fcm_token" to token,
                    "updated_at" to java.time.Instant.now().toString()
                )
            ) {
                onConflict = "user_id"
            }
        } catch (e: Exception) {
            // Push registration must never turn a successful login into a failure.
            android.util.Log.w("FCM_DEBUG", "Could not register device token after authentication")
        }
    }

    private suspend fun removeCurrentDeviceToken() {
        val userId = currentUserId ?: return
        try {
            client.postgrest["device_tokens"].delete {
                filter { eq("user_id", userId) }
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

            profile.username.isNotBlank() &&
                !profile.avatarUrl.isNullOrBlank()

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


    // Password reset — triggers Supabase email
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            client.auth.resetPasswordForEmail(email)
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

    // ── Change password ───────────────────────────────────────────────────────
    suspend fun changePassword(newPassword: String): Result<Unit> {
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
            val imageLoader = coil.ImageLoader.Builder(context).build()
            imageLoader.memoryCache?.clear()
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
