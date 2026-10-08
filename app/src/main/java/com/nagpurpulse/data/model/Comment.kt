// java/com/nagpurpulse/data/model/Comment.kt
package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class Comment(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    // Blank for anonymous comments when the row comes from the public RPCs.
    @SerialName("user_id") val userId: String = "",
    @SerialName("parent_id") val parentId: String? = null,
    val body: String = "",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("deleted_by_author") val deletedByAuthor: Boolean = false,
    val upvotes: Int = 0,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("anon_alias") val anonAlias: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    // Only set when the author edits their own comment. Moderator edits are
    // intentionally silent, so nothing about them is ever surfaced in the UI.
    @SerialName("edited_at") val editedAt: String? = null,
    // Raw table column, kept so admin tooling can still decode rows. UI never reads it.
    @SerialName("edited_by_admin") val editedByAdmin: Boolean = false,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @kotlinx.serialization.Transient val likedByCurrentUser: Boolean = false,
    @kotlinx.serialization.Transient val isMine: Boolean = false,
    @kotlinx.serialization.Transient val isPostAuthor: Boolean = false
)

/** One page of a thread: loaded top-level comments with all of their replies. */
data class CommentPage(val comments: List<Comment>, val totalRoots: Int)

@Serializable
data class CommentLikeResult(
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("like_count") val likeCount: Int = 0
)

/** Row shape returned by the comment RPCs (public.thread_comment). */
@Serializable
data class ThreadCommentRow(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("user_id") val userId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    val body: String? = "",
    @SerialName("is_deleted") val isDeleted: Boolean? = false,
    @SerialName("deleted_by_author") val deletedByAuthor: Boolean? = false,
    val upvotes: Int? = 0,
    @SerialName("is_anonymous") val isAnonymous: Boolean? = false,
    @SerialName("anon_alias") val anonAlias: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_post_author") val isPostAuthor: Boolean? = false,
    @SerialName("is_mine") val isMine: Boolean? = false,
    @SerialName("liked_by_me") val likedByMe: Boolean? = false,
    @SerialName("total_roots") val totalRoots: Long? = 0
) {
    fun toComment(): Comment = Comment(
        id = id, postId = postId, userId = userId.orEmpty(), parentId = parentId,
        body = body.orEmpty(), isDeleted = isDeleted == true, deletedByAuthor = deletedByAuthor == true,
        upvotes = upvotes ?: 0, isAnonymous = isAnonymous == true, anonAlias = anonAlias,
        createdAt = createdAt, editedAt = editedAt, username = username, avatarUrl = avatarUrl,
        likedByCurrentUser = likedByMe == true, isMine = isMine == true, isPostAuthor = isPostAuthor == true
    )
}

/** Parses Postgres/PostgREST timestamps ("...+00:00" or "...Z") safely. */
fun parseInstantOrNull(iso: String?): Instant? {
    if (iso.isNullOrBlank()) return null
    return try { OffsetDateTime.parse(iso).toInstant() } catch (_: Exception) {
        try { Instant.parse(iso) } catch (_: Exception) { null }
    }
}

/** "just now" / "5m" / "3h" / "2d", then a short date after a week. */
fun formatRelativeTime(iso: String?, nowMillis: Long = System.currentTimeMillis()): String {
    val created = parseInstantOrNull(iso) ?: return "recently"
    val seconds = ((nowMillis - created.toEpochMilli()) / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m"
        seconds < 86_400 -> "${seconds / 3_600}h"
        seconds < 7 * 86_400 -> "${seconds / 86_400}d"
        else -> {
            val zone = ZoneId.systemDefault()
            val sameYear = created.atZone(zone).year == Instant.ofEpochMilli(nowMillis).atZone(zone).year
            DateTimeFormatter.ofPattern(if (sameYear) "d MMM" else "d MMM yyyy", Locale.getDefault())
                .withZone(zone).format(created)
        }
    }
}

fun Comment.timeAgo(nowMillis: Long = System.currentTimeMillis()): String =
    formatRelativeTime(createdAt, nowMillis)
