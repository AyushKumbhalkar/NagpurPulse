
// data/repository/AdminRepository.kt
package com.nagpurpulse.data.repository


import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CancellationException
import com.nagpurpulse.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class AdminRepository @Inject constructor(
    private val client: SupabaseClient
) {

    private val currentUserId get() = client.auth.currentUserOrNull()?.id

    // ── Access check ────────────────────────────────────────────────────────────

    suspend fun isAdmin(): Boolean {
        val uid = currentUserId ?: return false
        return try {
            val roles = client.postgrest["admin_roles"]
                .select { filter { eq("user_id", uid) } }
                .decodeList<AdminRole>()
            roles.isNotEmpty()
        } catch (_: Exception) { false }
    }

    suspend fun getAdminRole(): String? {
        val uid = currentUserId ?: return null
        return try {
            client.postgrest["admin_roles"]
                .select { filter { eq("user_id", uid) } }
                .decodeList<AdminRole>()
                .firstOrNull()?.role
        } catch (_: Exception) { null }
    }

    // ── Dashboard Stats ─────────────────────────────────────────────────────────

    suspend fun getDashboardStats(): Result<AdminStats> = runCatching {
        val todayStart = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant().toString()

        val postsToday = safeCount {
            client.postgrest["posts"]
                .select { filter { gte("created_at", todayStart) } }
                .decodeList<Post>().size
        }

        val commentsToday = safeCount {
            client.postgrest["comments"]
                .select { filter { gte("created_at", todayStart) } }
                .decodeList<Comment>().size
        }

        val newUsersToday = safeCount {
            client.postgrest["profiles"]
                .select { filter { gte("created_at", todayStart) } }
                .decodeList<Profile>().size
        }

        val pendingPostReports = safeCount {
            client.postgrest["post_reports"]
                .select { filter { eq("status", "pending") } }
                .decodeList<PostReport>().size
        }

        val pendingCommentReports = safeCount {
            client.postgrest["comment_reports"]
                .select { filter { eq("status", "pending") } }
                .decodeList<CommentReport>().size
        }

        // Urgent = post_reports grouped by post_id with 3+ reports
        val allPostReports = try {
            client.postgrest["post_reports"]
                .select { filter { eq("status", "pending") } }
                .decodeList<PostReport>()
        } catch (_: Exception) { emptyList() }
        val urgentCount = allPostReports.groupBy { it.postId }.count { it.value.size >= 3 }

        AdminStats(
            postsToday        = postsToday,
            commentsToday     = commentsToday,
            newUsersToday     = newUsersToday,
            reportedPosts     = pendingPostReports,
            reportedComments  = pendingCommentReports,
            urgentReports     = urgentCount
        )
    }

    suspend fun getWeeklyActivity(): Result<List<Int>> = runCatching {
        (6 downTo 0).map { daysAgo ->
            val date = java.time.LocalDate.now().minusDays(daysAgo.toLong())
            val start = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toString()
            val end   = date.atTime(23, 59, 59).atZone(java.time.ZoneOffset.UTC).toInstant().toString()
            try {


                val posts = client.postgrest["posts"].select {
                    filter {
                        gte("created_at", start)
                        lte("created_at", end)
                    }
                }.decodeList<Post>()
                val comments = client.postgrest["comments"].select {
                    filter {
                        gte("created_at", start)
                        lte("created_at", end)
                    }
                }.decodeList<Comment>()
                posts.size + comments.size
            } catch (_: Exception) { 0 }
        }
    }

    // ── Moderation Queue ────────────────────────────────────────────────────────

    suspend fun getModerationQueue(filter: String = "all"): Result<List<QueueItem>> = runCatching {

        android.util.Log.d(
            "ADMIN_QUEUE",
            "getModerationQueue() called. Filter = $filter"
        )

        android.util.Log.d(
            "ADMIN_QUEUE",
            "Current user = ${client.auth.currentUserOrNull()?.id}"
        )

        val items = mutableListOf<QueueItem>()

        if (filter == "all" || filter == "posts" || filter == "urgent") {

            val postReports = try {
                client.postgrest["post_reports"]
                    .select {
                        filter { eq("status", "pending") }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<PostReport>()
            } catch (e: Exception) {
                android.util.Log.e(
                    "ADMIN_QUEUE",
                    "Failed to load post reports",
                    e
                )
                emptyList()
            }

            android.util.Log.d(
                "ADMIN_QUEUE",
                "Pending post reports = ${postReports.size}"
            )

            postReports.groupBy { it.postId }.forEach { (postId, reports) ->
                if (filter == "urgent" && reports.size < 3) return@forEach
                try {
                    val post = client.postgrest["posts"]
                        .select {
                            filter { eq("id", postId) }
                        }
                        .decodeSingle<Post>()

                    items.add(
                        QueueItem(
                            id = reports.first().id,
                            type = "post",
                            title = post.title,
                            authorUsername = fetchUsernameById(post.userId),
                            authorUserId = post.userId,
                            reportCount = reports.size,
                            topReason = reports.groupBy { it.reason }
                                .maxByOrNull { it.value.size }?.key ?: "Spam",
                            timeAgo = timeAgo(reports.first().createdAt),
                            targetId = postId
                        )
                    )

                } catch (e: Exception) {
                    android.util.Log.e(
                        "ADMIN_QUEUE",
                        "Failed loading post $postId",
                        e
                    )
                }
            }
        }

        if (filter == "all" || filter == "comments") {
            val commentReports = try {
                client.postgrest["comment_reports"]
                    .select {
                        filter { eq("status", "pending") }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CommentReport>()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "ADMIN_QUEUE",
                    "Failed to load comment reports",
                    e
                )
                emptyList()
            }

            android.util.Log.d(
                "ADMIN_QUEUE",
                "Pending comment reports = ${commentReports.size}"
            )

            commentReports.groupBy { it.commentId }.forEach { (commentId, reports) ->

                android.util.Log.d(
                    "ADMIN_QUEUE",
                    "Processing commentId = $commentId"
                )
                try {
                    val comment = client.postgrest["comments"]
                        .select { filter { eq("id", commentId) } }
                        .decodeSingle<Comment>()
                    val postTitle = try {
                        client.postgrest["posts"]
                            .select { filter { eq("id", comment.postId) } }
                            .decodeSingle<Post>().title
                    } catch (_: Exception) { "Unknown post" }
                    items.add(QueueItem(
                        id              = reports.first().id,
                        type            = "comment",
                        title           = comment.body.take(80) + if (comment.body.length > 80) "..." else "",
                        authorUsername  = fetchUsernameById(comment.userId),
                        authorUserId    = comment.userId,
                        reportCount     = reports.size,
                        topReason       = reports.groupBy { it.reason }
                                            .maxByOrNull { it.value.size }?.key
                                            ?.replaceFirstChar { it.uppercase() } ?: "Spam",
                        timeAgo         = timeAgo(reports.first().createdAt),
                        targetId        = commentId,
                        commentId = comment.id,
                        postId          = comment.postId
                    )


                    )
                } catch (e: Exception) {
                    android.util.Log.e(
                        "ADMIN_QUEUE",
                        "Failed loading comment report $commentId",
                        e
                    )
                }
            }
        }

        items.sortedByDescending { it.reportCount }
    }

    // ── Posts Management ────────────────────────────────────────────────────────

    suspend fun getAllPosts(
        sortBy: String = "latest",
        query: String? = null,
        limit: Int = 20,
        date: String? = null
    ): Result<List<AdminPost>> = runCatching {

        val posts = client.postgrest["posts"].select {
            if (!query.isNullOrBlank()) {
                filter { ilike("title", "%$query%") }
            }

            if (!date.isNullOrBlank()) {
                val selectedDate = java.time.LocalDate.parse(date)
                val zone = java.time.ZoneId.of("Asia/Kolkata")

                val start = selectedDate
                    .atStartOfDay(zone)
                    .toInstant()
                    .toString()

                val end = selectedDate
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toString()

                android.util.Log.d(
                    "ADMIN_DATE_DEBUG",
                    "REPOSITORY -> selectedDate=$date"
                )

                android.util.Log.d(
                    "ADMIN_DATE_DEBUG",
                    "REPOSITORY -> IST start=$start"
                )

                android.util.Log.d(
                    "ADMIN_DATE_DEBUG",
                    "REPOSITORY -> IST end=$end"
                )

                filter {
                    and {
                        gte("created_at", start)
                        lt("created_at", end)
                    }
                }
            }

            when (sortBy) {
                "latest"       -> order("created_at", Order.DESCENDING)
                "most_upvoted" -> order("upvotes", Order.DESCENDING)
                "most_reported" -> order("created_at", Order.DESCENDING)
                else            -> order("created_at", Order.DESCENDING)
            }

            limit(limit.toLong())
        }.decodeList<Post>()

        android.util.Log.d(
            "ADMIN_DATE_DEBUG",
            "REPOSITORY -> Supabase returned ${posts.size} posts for date=$date"
        )

        posts.forEachIndexed { index, post ->
            android.util.Log.d(
                "ADMIN_DATE_DEBUG",
                "REPOSITORY -> post[$index] id=${post.id}, " +
                        "createdAt=${post.createdAt}, " +
                        "title='${post.title}'"
            )
        }

        // Enrich with usernames + report counts
        posts.map { post ->
            val username = fetchUsernameById(post.userId)
            val enrichedPost = post.copy(username = username)
            val reportCount = try {
                client.postgrest["post_reports"]
                    .select { filter { eq("post_id", post.id) } }
                    .decodeList<PostReport>().size
            } catch (_: Exception) { 0 }
            AdminPost(
                post        = enrichedPost,
                reportCount = reportCount,
                isPinned    = post.isPinned,
                isLocked    = post.isLocked
            )
        }.let { list ->
            if (sortBy == "most_reported") list.sortedByDescending { it.reportCount } else list
        }
    }

    // ── Comments Management ─────────────────────────────────────────────────────

    suspend fun getAllComments(
        sortBy: String = "latest",
        limit: Int = 20,
        date: String? = null
    ): Result<List<AdminComment>> = runCatching {

        val comments = client.postgrest["comments"].select {

            if (!date.isNullOrBlank()) {
                val selectedDate = java.time.LocalDate.parse(date)
                val zone = java.time.ZoneId.of("Asia/Kolkata")

                val start = selectedDate
                    .atStartOfDay(zone)
                    .toInstant()
                    .toString()

                val end = selectedDate
                    .plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toString()

                filter {
                    and {
                        gte("created_at", start)
                        lt("created_at", end)
                    }
                }
            }

            when (sortBy) {
                "most_reported" -> order("created_at", Order.DESCENDING)
                else            -> order("created_at", Order.DESCENDING)
            }

            limit(limit.toLong())
        }.decodeList<Comment>()

        comments.map { comment ->
            val username = fetchUsernameById(comment.userId)
            val postTitle = try {
                client.postgrest["posts"]
                    .select { filter { eq("id", comment.postId) } }
                    .decodeSingle<Post>().title.take(50)
            } catch (_: Exception) { "Unknown post" }
            val reportCount = try {
                client.postgrest["comment_reports"]
                    .select { filter { eq("comment_id", comment.id) } }
                    .decodeList<CommentReport>().size
            } catch (_: Exception) { 0 }
            AdminComment(
                comment      = comment.copy(username = username),
                postTitle    = postTitle,
                reportCount  = reportCount
            )
        }.let { list ->
            if (sortBy == "most_reported") list.sortedByDescending { it.reportCount } else list
        }
    }

    // ── User Management ─────────────────────────────────────────────────────────

    suspend fun getUsers(userFilter: String = "all"): Result<List<Profile>> = runCatching {
        client.postgrest["profiles"]
            .select {
                if (userFilter == "verified") {
                    filter { eq("is_verified", true) }
                }
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<Profile>()
    }

    suspend fun searchUser(query: String): Result<AdminUserDetail?> = runCatching {
        if (query.isBlank()) return@runCatching null
        val profile = try {
            client.postgrest["profiles"]
                .select { filter { ilike("username", "%$query%") }; limit(1) }
                .decodeList<Profile>()
                .firstOrNull() ?: return@runCatching null
        } catch (_: Exception) { return@runCatching null }
        buildUserDetail(profile)
    }

    suspend fun getUserDetail(userId: String): Result<AdminUserDetail> = runCatching {
        val profile = client.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<Profile>()
        buildUserDetail(profile)
    }

    // ── Admin Actions ───────────────────────────────────────────────────────────

    suspend fun deletePost(postId: String, reason: String): Result<Unit> {

        return try {

            android.util.Log.d("ADMIN_DELETE", "Deleting post: $postId")



            // client.postgrest["notifications"].delete {
//     filter {
//         eq("related_post_id", postId)
//     }
// }

// android.util.Log.d("ADMIN_DELETE", "Notifications deleted")

            client.postgrest["posts"].delete {
                filter {
                    eq("id", postId)
                }
            }

            android.util.Log.d("ADMIN_DELETE", "Post deleted successfully")

            client.postgrest["post_reports"].update(
                mapOf("status" to "resolved")
            ) {
                filter {
                    eq("post_id", postId)
                }
            }

            android.util.Log.d("ADMIN_DELETE", "Reports resolved")

            logAction("delete_post", "post", postId, reason)

            Result.success(Unit)

        } catch (e: Exception) {

            android.util.Log.e(
                "ADMIN_DELETE",
                "DELETE FAILED",
                e
            )

            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: String, reason: String): Result<Unit> = runCatching {
        // Direct table updates are blocked for moderators, so removal uses an admin-checked RPC
        // (it also resolves open reports for the comment).
        client.postgrest.rpc("admin_remove_comment", buildJsonObject { put("p_comment_id", commentId) })

        logAction("delete_comment", "comment", commentId, reason)
    }

    suspend fun pinPost(postId: String, pin: Boolean): Result<Unit> = runCatching {
        client.postgrest["posts"].update(mapOf("is_pinned" to pin)) {
            filter { eq("id", postId) }
        }
        logAction("pin_post", "post", postId, if (pin) "Pinned" else "Unpinned")
    }

    suspend fun lockPost(postId: String, lock: Boolean): Result<Unit> = runCatching {
        client.postgrest["posts"].update(mapOf("is_locked" to lock)) {
            filter { eq("id", postId) }
        }
        logAction("lock_post", "post", postId, if (lock) "Comments locked" else "Comments unlocked")
    }

    suspend fun warnUser(userId: String, reason: String): Result<Unit> = runCatching {
        // Insert a notification to the user about the warning
        try {
            client.postgrest["notifications"].insert(
                mapOf(
                    "user_id" to userId,
                    "type"    to "admin_warning",
                    "title"   to "⚠️ Community Warning",
                    "body"    to "Your content violated our community guidelines. Reason: $reason"
                )
            )
        } catch (_: Exception) {}
        logAction("warn_user", "user", userId, reason)
    }

    suspend fun suspendUser(userId: String, days: Int, reason: String): Result<Unit> = runCatching {
        val adminId = currentUserId ?: throw Exception("Not authenticated")
        val until = java.time.Instant.now()
            .plus(days.toLong(), java.time.temporal.ChronoUnit.DAYS)
            .toString()
        try {
            client.postgrest["user_suspensions"].delete { filter { eq("user_id", userId) } }
        } catch (_: Exception) {}
        client.postgrest["user_suspensions"].insert(
            mapOf(
                "user_id"         to userId,
                "suspended_by"    to adminId,
                "reason"          to reason,
                "suspended_until" to until,
                "is_permanent"    to false
            )
        )
        try {
            client.postgrest["notifications"].insert(
                mapOf(
                    "user_id" to userId,
                    "type"    to "admin_suspension",
                    "title"   to "🚫 Account Suspended",
                    "body"    to "Your account has been suspended for $days days. Reason: $reason"
                )
            )
        } catch (_: Exception) {}
        logAction("suspend_user", "user", userId, "Suspended $days days: $reason")
    }

    suspend fun permanentBan(userId: String, reason: String): Result<Unit> = runCatching {
        val adminId = currentUserId ?: throw Exception("Not authenticated")
        try {
            client.postgrest["user_suspensions"].delete { filter { eq("user_id", userId) } }
        } catch (_: Exception) {}
        client.postgrest["user_suspensions"].insert(
            mapOf(
                "user_id"      to userId,
                "suspended_by" to adminId,
                "reason"       to reason,
                "is_permanent" to true
            )
        )
        try {
            client.postgrest["notifications"].insert(
                mapOf(
                    "user_id" to userId,
                    "type"    to "admin_ban",
                    "title"   to "🚫 Account Permanently Banned",
                    "body"    to "Your account has been permanently banned. Reason: $reason"
                )
            )
        } catch (_: Exception) {}
        logAction("ban_user", "user", userId, reason)
    }

    suspend fun resolveReport(reportId: String, reportType: String): Result<Unit> = runCatching {
        val table = if (reportType == "post") "post_reports" else "comment_reports"
        client.postgrest[table].update(mapOf("status" to "resolved")) {
            filter { eq("id", reportId) }
        }
        logAction("resolve_report", "report", reportId, "No violation found")
    }

    suspend fun dismissReport(reportId: String, reportType: String): Result<Unit> = runCatching {
        val table = if (reportType == "post") "post_reports" else "comment_reports"
        client.postgrest[table].update(mapOf("status" to "dismissed")) {
            filter { eq("id", reportId) }
        }
        logAction("dismiss_report", "report", reportId, "Dismissed")
    }

    // ── Admin Logs ──────────────────────────────────────────────────────────────

    suspend fun getAdminLogs(filter: String = "all"): Result<List<AdminAction>> = runCatching {
        val actions = client.postgrest["admin_actions"].select {
            if (filter != "all") {
                val types = when (filter) {
                    "deletions" -> listOf("delete_post", "delete_comment")
                    "bans"      -> listOf("suspend_user", "ban_user")
                    "warnings"  -> listOf("warn_user")
                    else        -> emptyList()
                }
                if (types.isNotEmpty()) filter { isIn("action_type", types) }
            }
            order("created_at", Order.DESCENDING)
            limit(60)
        }.decodeList<AdminAction>()

        // Enrich with admin profile info
        val profileCache = mutableMapOf<String, Profile>()
        actions.map { action ->
            if (action.adminId.isBlank()) return@map action
            val profile = profileCache.getOrPut(action.adminId) {
                try {
                    client.postgrest["profiles"]
                        .select { filter { eq("id", action.adminId) } }
                        .decodeSingle<Profile>()
                } catch (_: Exception) { Profile(username = "Admin") }
            }
            action.copy(
                adminUsername  = profile.username,
                adminAvatarUrl = profile.avatarUrl
            )
        }
    }

    // ── Settings profile stats (no admin RLS needed) ────────────────────────────

    suspend fun getUserQuickStats(userId: String): Triple<Int, Int, Int> {
        val postCount = safeCount {
            client.postgrest["posts"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<Post>().size
        }
        val commentCount = safeCount {
            client.postgrest["comments"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<Comment>().size
        }
        val badgeCount = safeCount {
            client.postgrest["badges"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>().size
        }
        return Triple(postCount, commentCount, badgeCount)
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private suspend fun fetchUsernameById(userId: String): String {
        if (userId.isBlank()) return "user"
        return try {
            client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<Profile>().username
        } catch (_: Exception) { "user" }
    }

    private suspend fun buildUserDetail(profile: Profile): AdminUserDetail {
        val postCount = safeCount {
            client.postgrest["posts"]
                .select { filter { eq("user_id", profile.id) } }
                .decodeList<Post>().size
        }
        val commentCount = safeCount {
            client.postgrest["comments"]
                .select { filter { eq("user_id", profile.id) } }
                .decodeList<Comment>().size
        }
        // Count reports on their posts
        val userPostIds = try {
            client.postgrest["posts"]
                .select { filter { eq("user_id", profile.id) } }
                .decodeList<Post>().map { it.id }
        } catch (_: Exception) { emptyList() }
        var reportCount = 0
        userPostIds.chunked(10).forEach { chunk ->
            try {
                val count = client.postgrest["post_reports"]
                    .select { filter { isIn("post_id", chunk) } }
                    .decodeList<PostReport>().size
                reportCount += count
            } catch (_: Exception) {}
        }
        val suspension = try {
            client.postgrest["user_suspensions"]
                .select { filter { eq("user_id", profile.id) } }
                .decodeSingle<UserSuspension>()
        } catch (_: Exception) { null }

        return AdminUserDetail(
            profile      = profile,
            postCount    = postCount,
            commentCount = commentCount,
            reportCount  = reportCount,
            suspension   = suspension
        )
    }

    private suspend fun logAction(
        actionType: String,
        targetType: String,
        targetId: String,
        reason: String
    ) {
        val uid = currentUserId ?: return
        try {
            client.postgrest["admin_actions"].insert(
                mapOf(
                    "admin_id"    to uid,
                    "action_type" to actionType,
                    "target_type" to targetType,
                    "target_id"   to targetId,
                    "reason"      to reason
                )
            )
        } catch (_: Exception) {}
    }

    private inline fun safeCount(block: () -> Int): Int = try { block() } catch (_: Exception) { 0 }

    private fun timeAgo(iso: String): String {
        return try {
            val created = java.time.Instant.parse(iso)
            val diff    = java.time.Duration.between(created, java.time.Instant.now())
            when {
                diff.toMinutes() < 1  -> "just now"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
                diff.toHours()   < 24 -> "${diff.toHours()}h ago"
                else                   -> "${diff.toDays()}d ago"
            }
        } catch (_: Exception) { "recently" }
    }
}

