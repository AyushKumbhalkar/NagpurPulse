//java/com/nagpurpulse/data/model/CreateProfileRequest.kt

package com.nagpurpulse.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateProfileRequest(
    val id: String,
    val username: String,
    val karma: Int = 0
)