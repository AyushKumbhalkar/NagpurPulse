// This is the Comment.kt file

//java/com/nagpurpulse/data/model/Comment.kt
package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("parent_id") val parentId: String? = null,
    val body: String = "",

    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    val upvotes: Int = 0,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("anon_alias")   val anonAlias: String? = null,
    @SerialName("created_at") val createdAt: String = "",

    @SerialName("edited_at")
    val editedAt: String? = null,

    @SerialName("edited_by_admin")
    val editedByAdmin: Boolean = false,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

fun Comment.timeAgo(): String {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(created, now)
        when {
            diff.toMinutes() < 1 -> "just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m"
            diff.toHours() < 24 -> "${diff.toHours()}h"
            else -> "${diff.toDays()}d"
        }
    } catch (e: Exception) {
        "recently"
    }
}
