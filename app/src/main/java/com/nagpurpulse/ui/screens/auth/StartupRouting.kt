package com.nagpurpulse.ui.screens.auth

import android.content.Context
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.OnboardingProbe

internal enum class OnboardingState { Complete, Incomplete, Unknown }

/** Remembers, per user id, that profile setup was finished so a flaky network can never undo it. */
private object OnboardingCache {
    private const val PREFS = "onboarding_cache"
    private fun key(uid: String) = "done_$uid"
    fun isDone(context: Context, uid: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key(uid), false)
    fun set(context: Context, uid: String, done: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key(uid), done).apply()
    }
}

/**
 * Decides where a signed-in user belongs.
 *  - server says complete    -> Complete (and cached)
 *  - server says incomplete  -> Incomplete
 *  - server unreachable      -> Complete if we already know they finished, else Unknown (show Retry)
 */
internal suspend fun resolveOnboarding(context: Context, repo: AuthRepository): OnboardingState {
    val uid = repo.currentUserId ?: return OnboardingState.Incomplete
    return when (repo.probeOnboarding()) {
        OnboardingProbe.COMPLETE -> { OnboardingCache.set(context, uid, true); OnboardingState.Complete }
        OnboardingProbe.INCOMPLETE -> { OnboardingCache.set(context, uid, false); OnboardingState.Incomplete }
        OnboardingProbe.UNREACHABLE ->
            if (OnboardingCache.isDone(context, uid)) OnboardingState.Complete else OnboardingState.Unknown
    }
}
