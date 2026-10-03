package com.nagpurpulse.ui.screens.profile

/**
 * Public avatars stored in the Supabase "Nagpu Pulse Avatars" bucket.
 * Keep these URL paths in sync with the exact Storage bucket/folder names.
 */
object RandomImages {
    private const val BASE_URL =
        "https://eazkmfzegxmdkbowohiy.supabase.co/storage/v1/object/public/Nagpu%20Pulse%20Avatars"

    val boysAvatars: List<String> = (1..50).map { index ->
        "$BASE_URL/Boys_Avatar/Avatar_boy_$index.png"
    }

    val girlsAvatars: List<String> = (1..46).map { index ->
        "$BASE_URL/Girls_Avatar/Avatar_girl_$index.png"
    }

    val allAvatars: List<String> = boysAvatars + girlsAvatars

    /**
     * Male and female users get their matching avatar set.
     * Other/prefer-not-to-say and guest flows can choose from both sets.
     */
    fun forGender(gender: String?): List<String> = when (gender?.trim()?.lowercase()) {
        "male", "man", "boy" -> boysAvatars
        "female", "woman", "girl" -> girlsAvatars
        else -> allAvatars
    }

    // Backwards-compatible list for existing random-avatar entry points.
    val avatars: List<String> get() = allAvatars
}
