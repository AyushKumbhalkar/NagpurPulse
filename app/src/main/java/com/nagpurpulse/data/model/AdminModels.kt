// data/model/AdminModels.kt
package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Admin Role ────────────────────────────────────────────────────────────────
@Serializable
data class AdminRole(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val role: String = "",          // super_admin | admin | moderator
    @SerialName("granted_at") val grantedAt: String = ""
)

// ── Post Report ────────────────────────────────────────────────────────────────
@Serializable
data class PostReport(
    val id: String = "",
    @SerialName("post_id") val postId: String = "",
    @SerialName("reported_by") val reportedBy: String = "",
    val reason: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = ""
)

// ── Comment Report ─────────────────────────────────────────────────────────────
@Serializable
data class CommentReport(
    val id: String = "",
    @SerialName("comment_id") val commentId: String = "",
    @SerialName("reported_by") val reportedBy: String = "",
    val reason: String = "",
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = ""
)

// ── Admin Audit Action ─────────────────────────────────────────────────────────
@Serializable
data class AdminAction(
    val id: String = "",
    @SerialName("admin_id") val adminId: String = "",
    @SerialName("action_type") val actionType: String = "",
    @SerialName("target_type") val targetType: String = "",
    @SerialName("target_id") val targetId: String = "",
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    // Enriched locally — not from Supabase
    val adminUsername: String? = null,
    val adminAvatarUrl: String? = null
)

fun AdminAction.timeAgo(): String {
    return try {
        val created = java.time.Instant.parse(createdAt)
        val now = java.time.Instant.now()
        val diff = java.time.Duration.between(created, now)
        when {
            diff.toMinutes() < 1 -> "just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24 -> "${diff.toHours()}h ago"
            else -> "${diff.toDays()}d ago"
        }
    } catch (_: Exception) { "recently" }
}

fun AdminAction.actionLabel(): String = when (actionType) {
    "delete_post"     -> "deleted post"
    "delete_comment"  -> "deleted comment"
    "warn_user"       -> "warned user"
    "suspend_user"    -> "suspended user"
    "ban_user"        -> "permanently banned user"
    "resolve_report"  -> "resolved report on post"
    "dismiss_report"  -> "dismissed report"
    "pin_post"        -> "pinned post"
    "lock_post"       -> "locked post"
    else              -> actionType
}

// ── User Suspension ────────────────────────────────────────────────────────────
@Serializable
data class UserSuspension(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val reason: String? = null,
    @SerialName("suspended_until") val suspendedUntil: String? = null,
    @SerialName("is_permanent") val isPermanent: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

// ── Dashboard Stats ────────────────────────────────────────────────────────────
data class AdminStats(
    val postsToday: Int = 0,
    val commentsToday: Int = 0,
    val newUsersToday: Int = 0,
    val reportedPosts: Int = 0,
    val reportedComments: Int = 0,
    val urgentReports: Int = 0        // posts with 3+ reports
)

// ── Moderation Queue Item ──────────────────────────────────────────────────────
data class QueueItem(
    val id: String,                   // report ID
    val type: String,                 // "post" or "comment"
    val title: String,                // post title or comment excerpt
    val authorUsername: String,
    val authorUserId: String = "",    // author's UUID — used for warn/suspend actions
    val reportCount: Int,
    val topReason: String,
    val timeAgo: String,
    // Underlying IDs
    val targetId: String,             // post_id or comment_id
    val postId: String = "",           // for comments: parent post id
    val commentId: String = ""
)

// ── Admin Post (post with admin metadata) ──────────────────────────────────────
data class AdminPost(
    val post: Post,
    val reportCount: Int = 0,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false
)

// ── Admin Comment (comment with context) ──────────────────────────────────────
data class AdminComment(
    val comment: Comment,
    val postTitle: String = "",
    val reportCount: Int = 0
)

// ── Admin User Detail (for user management screen) ────────────────────────────
data class AdminUserDetail(
    val profile: Profile,
    val postCount: Int = 0,
    val commentCount: Int = 0,
    val reportCount: Int = 0,         // reports received on their content
    val suspension: UserSuspension? = null
)
