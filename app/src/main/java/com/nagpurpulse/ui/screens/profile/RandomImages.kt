package com.nagpurpulse.ui.screens.profile

/**
 * Public avatar assets verified against the NagpurPulse Supabase Storage objects.
 * Bucket: "Nagpu Pulse Avatars" (public)
 */
object RandomImages {
    val boys: List<String> = listOf(
        "${base}/Boys_Avatar/Avatar_boy_1.png",
        "${base}/Boys_Avatar/Avatar_boy_2.png",
        "${base}/Boys_Avatar/Avatar_boy_3.png",
        "${base}/Boys_Avatar/Avatar_boy_4.png",
        "${base}/Boys_Avatar/Avatar_boy_5.png",
        "${base}/Boys_Avatar/Avatar_boy_6.png",
        "${base}/Boys_Avatar/Avatar_boy_7.png",
        "${base}/Boys_Avatar/Avatar_boy_8.png",
        "${base}/Boys_Avatar/Avatar_boy_9.png",
        "${base}/Boys_Avatar/Avatar_boy_10.png",
        "${base}/Boys_Avatar/Avatar_boy_11.png",
        "${base}/Boys_Avatar/Avatar_boy_12.png",
        "${base}/Boys_Avatar/Avatar_boy_13.png",
        "${base}/Boys_Avatar/Avatar_boy_14.png",
        "${base}/Boys_Avatar/Avatar_boy_15.png",
        "${base}/Boys_Avatar/Avatar_boy_16.png",
        "${base}/Boys_Avatar/Avatar_boy_17.png",
        "${base}/Boys_Avatar/Avatar_boy_18.png",
        "${base}/Boys_Avatar/Avatar_boy_19.png",
        "${base}/Boys_Avatar/Avatar_boy_20.png",
        "${base}/Boys_Avatar/Avatar_boy_21.png",
        "${base}/Boys_Avatar/Avatar_boy_22.png",
        "${base}/Boys_Avatar/Avatar_boy_23.png",
        "${base}/Boys_Avatar/Avatar_boy_24.png",
        "${base}/Boys_Avatar/Avatar_boy_25.png",
        "${base}/Boys_Avatar/Avatar_boy_26.png",
        "${base}/Boys_Avatar/Avatar_boy_27.png",
        "${base}/Boys_Avatar/Avatar_boy_28.png",
        "${base}/Boys_Avatar/Avatar_boy_29.png",
        "${base}/Boys_Avatar/Avatar_boy_30.png",
        "${base}/Boys_Avatar/Avatar_boy_31.png",
        "${base}/Boys_Avatar/Avatar_boy_32.png",
        "${base}/Boys_Avatar/Avatar_boy_33.png",
        "${base}/Boys_Avatar/Avatar_boy_34.png",
        "${base}/Boys_Avatar/Avatar_boy_35.png",
        "${base}/Boys_Avatar/Avatar_boy_36.png",
        "${base}/Boys_Avatar/Avatar_boy_37.png",
        "${base}/Boys_Avatar/Avatar_boy_38.png",
        "${base}/Boys_Avatar/Avatar_boy_39.png",
        "${base}/Boys_Avatar/Avatar_boy_40.png",
        "${base}/Boys_Avatar/Avatar_boy_41.png",
        "${base}/Boys_Avatar/Avatar_boy_42.png",
        "${base}/Boys_Avatar/Avatar_boy_43.png",
        "${base}/Boys_Avatar/Avatar_boy_44.png",
        "${base}/Boys_Avatar/Avatar_boy_45.png",
        "${base}/Boys_Avatar/Avatar_boy_46.png",
        "${base}/Boys_Avatar/Avatar_boy_47.png",
        "${base}/Boys_Avatar/Avatar_boy_48.png",
        "${base}/Boys_Avatar/Avatar_boy_49.png",
        "${base}/Boys_Avatar/Avatar_boy_50.png"
    )

    val girls: List<String> = listOf(
        "${base}/Girls_Avatar/Avatar_girl_1.png",
        "${base}/Girls_Avatar/Avatar_girl_2.png",
        "${base}/Girls_Avatar/Avatar_girl_3.png",
        "${base}/Girls_Avatar/Avatar_girl_4.png",
        "${base}/Girls_Avatar/Avatar_girl_5.png",
        "${base}/Girls_Avatar/Avatar_girl_6.png",
        "${base}/Girls_Avatar/Avatar_girl_7.png",
        "${base}/Girls_Avatar/Avatar_girl_8.png",
        "${base}/Girls_Avatar/Avatar_girl_9.png",
        "${base}/Girls_Avatar/Avatar_girl_10.png",
        "${base}/Girls_Avatar/Avatar_girl_11.png",
        "${base}/Girls_Avatar/Avatar_girl_12.png",
        "${base}/Girls_Avatar/Avatar_girl_13.png",
        "${base}/Girls_Avatar/Avatar_girl_14.png",
        "${base}/Girls_Avatar/Avatar_girl_15.png",
        "${base}/Girls_Avatar/Avatar_girl_16.png",
        "${base}/Girls_Avatar/Avatar_girl_17.png",
        "${base}/Girls_Avatar/Avatar_girl_18.png",
        "${base}/Girls_Avatar/Avatar_girl_19.png",
        "${base}/Girls_Avatar/Avatar_girl_20.png",
        "${base}/Girls_Avatar/Avatar_girl_21.png",
        "${base}/Girls_Avatar/Avatar_girl_22.png",
        "${base}/Girls_Avatar/Avatar_girl_23.png",
        "${base}/Girls_Avatar/Avatar_girl_24.png",
        "${base}/Girls_Avatar/Avatar_girl_25.png",
        "${base}/Girls_Avatar/Avatar_girl_26.png",
        "${base}/Girls_Avatar/Avatar_girl_27.png",
        "${base}/Girls_Avatar/Avatar_girl_28.png",
        "${base}/Girls_Avatar/Avatar_girl_29.png",
        "${base}/Girls_Avatar/Avatar_girl_30.png",
        "${base}/Girls_Avatar/Avatar_girl_31.png",
        "${base}/Girls_Avatar/Avatar_girl_32.png",
        "${base}/Girls_Avatar/Avatar_girl_33.png",
        "${base}/Girls_Avatar/Avatar_girl_34.png",
        "${base}/Girls_Avatar/Avatar_girl_35.png",
        "${base}/Girls_Avatar/Avatar_girl_36.png",
        "${base}/Girls_Avatar/Avatar_girl_37.png",
        "${base}/Girls_Avatar/Avatar_girl_38.png",
        "${base}/Girls_Avatar/Avatar_girl_39.png",
        "${base}/Girls_Avatar/Avatar_girl_40.png",
        "${base}/Girls_Avatar/Avatar_girl_41.png",
        "${base}/Girls_Avatar/Avatar_girl_42.png",
        "${base}/Girls_Avatar/Avatar_girl_43.png",
        "${base}/Girls_Avatar/Avatar_girl_44.png",
        "${base}/Girls_Avatar/Avatar_girl_45.png",
        "${base}/Girls_Avatar/Avatar_girl_46.png"
    )

    /** The full catalog is useful for users who don't disclose a gender. */
    val avatars: List<String> = boys + girls

    fun forGender(gender: String?): List<String> = when (gender?.lowercase()) {
        "male" -> boys
        "female" -> girls
        else -> avatars
    }
}
