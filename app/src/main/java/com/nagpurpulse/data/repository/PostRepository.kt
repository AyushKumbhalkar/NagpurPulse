//This is the PostRepository.kt file
// java/com/nagpurpulse/data/repository/PostRepository.kt


package com.nagpurpulse.data.repository

import com.nagpurpulse.data.model.Profile
import com.nagpurpulse.data.model.UserPreferences
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import java.io.ByteArrayInputStream
import io.github.jan.supabase.storage.storage
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import io.github.jan.supabase.storage.upload
import java.util.UUID
import com.nagpurpulse.data.model.Comment
import com.nagpurpulse.data.model.CommentLikeResult
import com.nagpurpulse.data.model.CommentPage
import com.nagpurpulse.data.model.ThreadCommentRow
import com.nagpurpulse.data.model.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class PostRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository

)

 {

    // ── Posts ─────────────────────────────────────────────────────────────────

    suspend fun getPosts(
        category: String? = null,
        sortBy: String = "top",
        page: Int = 0,
        pageSize: Int = 20
    ): Result<List<Post>> {
        return try {
            val from = (page * pageSize).toLong()
            val to   = (from + pageSize - 1)
            val response = client.postgrest["posts"].select {
                filter {
                    if (category != null) eq("category", category)
                }
                when (sortBy) {
                    "top" -> order("upvotes",      Order.DESCENDING)
                    "new" -> order("created_at",   Order.DESCENDING)
                    "hot" -> order("comment_count", Order.DESCENDING)
                    else  -> order("upvotes",      Order.DESCENDING)
                }
                range(from, to)
            }
            val posts = response.decodeList<Post>()
            // Respect profile visibility in every feed page, not only search results.
            // This is a client-side UX guard; Supabase RLS is still required for
            // security because clients can bypass repository filtering.
            val visiblePosts = filterVisiblePosts(posts, forSearch = false)
            Result.success(enrichPostsWithUsernames(visiblePosts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostById(id: String): Result<Post> {
        return try {
            val post = client.postgrest["posts"]
                .select { filter { eq("id", id) } }
                .decodeSingle<Post>()
            if (!isPostVisible(post)) {
                return Result.failure(IllegalStateException("This post is no longer available."))
            }
            // Increment view count only after visibility has been checked.
            try {
                client.postgrest["posts"].update(
                    mapOf("view_count" to post.viewCount + 1)
                ) { filter { eq("id", id) } }
            } catch (_: Exception) {}
            Result.success(enrichPostWithUsername(post))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads a post for a notification preview without incrementing its view count.
     */
    suspend fun getPostPreviewById(id: String): Result<Post> {
        return try {
            val post = client.postgrest["posts"]
                .select { filter { eq("id", id) } }
                .decodeSingle<Post>()
            if (!isPostVisible(post)) {
                return Result.failure(IllegalStateException("This post is no longer available."))
            }
            Result.success(enrichPostWithUsername(post))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Re-check visibility for direct links and notification previews, which do not
     * pass through the home-feed/search filtering path.
     */
    private suspend fun isPostVisible(post: Post): Boolean {
        if (post.isAnonymous || authRepository.currentUserId == post.userId) return true
        val profile = fetchUserProfiles(listOf(post.userId))[post.userId] ?: return false
        return !profile.hideProfile && !profile.hidePosts
    }

    suspend fun searchPosts(query: String): Result<List<Post>> {
        return try {
            val posts = client.postgrest["posts"].select {
                filter { ilike("title", "%$query%") }
                order("upvotes", Order.DESCENDING)
                limit(100)
            }.decodeList<Post>()

            // Apply profile visibility preferences before returning search results.
            // Anonymous posts remain anonymous and are not joined to profile identity.
            val visiblePosts = filterVisiblePosts(posts, forSearch = true).take(20)
            Result.success(enrichPostsWithUsernames(visiblePosts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Newest posts tagged with [area] (case-insensitive exact match on `area_tag`).
     * Used by Explore's "Browse by area". Applies the same visibility rules as the main feed.
     */
    suspend fun getPostsByArea(area: String): Result<List<Post>> {
        return try {
            val posts = client.postgrest["posts"].select {
                filter { ilike("area_tag", area) }
                order("created_at", Order.DESCENDING)
                limit(30)
            }.decodeList<Post>()
            val visiblePosts = filterVisiblePosts(posts, forSearch = false)
            Result.success(enrichPostsWithUsernames(visiblePosts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByUser(userId: String): Result<List<Post>> {
        return try {
            val posts = client.postgrest["posts"].select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<Post>()

            val isOwnProfile = authRepository.currentUserId == userId
            if (isOwnProfile) {
                Result.success(enrichPostsWithUsernames(posts))
            } else {
                val profile = fetchUserProfiles(listOf(userId))[userId]
                if (profile == null || profile.hideProfile || profile.hidePosts) {
                    Result.success(emptyList())
                } else {
                    Result.success(enrichPostsWithUsernames(posts))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun filterVisiblePosts(
        posts: List<Post>,
        forSearch: Boolean
    ): List<Post> {
        val authorIds = posts.filter { !it.isAnonymous }.map { it.userId }.distinct()
        val profiles = fetchUserProfiles(authorIds)
        return posts.filter { post ->
            if (post.isAnonymous) {
                true
            } else {
                val profile = profiles[post.userId]
                // Fail closed when a profile can't be loaded: do not expose a post
                // whose visibility rules cannot be checked.
                profile != null &&
                    !profile.hideProfile &&
                    !profile.hidePosts &&
                    (!forSearch || !profile.hideFromSearch)
            }
        }
    }

    suspend fun getSavedPosts(postIds: List<String>): Result<List<Post>> {
        if (postIds.isEmpty()) return Result.success(emptyList())
        return try {
            // Supabase: filter by list of IDs using 'in' filter
            val posts = mutableListOf<Post>()
            postIds.chunked(10).forEach { chunk ->
                try {
                    val batch = client.postgrest["posts"].select {
                        filter {
                            isIn("id", chunk)
                        }
                    }.decodeList<Post>()
                    posts.addAll(batch)
                } catch (_: Exception) {}
            }
            // Saved-post lists must follow the same visibility rules as the home feed.
            val visiblePosts = filterVisiblePosts(posts, forSearch = false)
            Result.success(enrichPostsWithUsernames(visiblePosts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPostImage(
        imageBytes: ByteArray
    ): Result<String> {

        return try {

            val currentUserId = authRepository.currentUserId
                ?: return Result.failure(IllegalStateException("You must be logged in to upload an image."))

            val fileName = "${UUID.randomUUID()}.jpg"
            val storagePath = "$currentUserId/$fileName"

            client.storage["post-images"].upload(
                path = storagePath,
                data = compressImage(imageBytes)
            )

            val publicUrl = client.storage["post-images"]
                .publicUrl(storagePath)

            Result.success(publicUrl)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getPostByIdForDelete(postId: String): Post? {
        return try {
            client.postgrest["posts"]
                .select {
                    filter {
                        eq("id", postId)
                    }
                }
                .decodeSingle<Post>()
        } catch (e: Exception) {
            null
        }
    }
    // ── Create Post ───────────────────────────────────────────────────────────
    suspend fun deletePost(postId: String): Result<Unit> {

        return try {

            val post = getPostByIdForDelete(postId)
                ?: return Result.failure(
                    Exception("Post not found")
                )

            client.postgrest["deleted_posts"].insert(
                buildJsonObject {

                    put("id", post.id)

                    put("original_post_id", post.id)
                    put("title", post.title)
                    put("body", post.body)
                    put("user_id", post.userId)
                    put("username", post.username)
                    put("category", post.category)
                    put("image_url", post.imageUrl)
                    put("created_at", post.createdAt)

                }
            )

            client.postgrest["posts"]
                .delete {
                    filter {
                        eq("id", postId)
                    }
                }

            Result.success(Unit)

        } catch (e: Exception) {

            android.util.Log.e(
                "DELETE_POST",
                "Repository delete failed: ${e.message}",
                e
            )

            Result.failure(e)
        }
    }

    private fun compressImage(imageBytes: ByteArray): ByteArray {

        val originalBitmap = BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size
        )

        val exif = ExifInterface(
            ByteArrayInputStream(imageBytes)
        )

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()

        when (orientation) {

            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.postRotate(90f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.postRotate(180f)

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.postRotate(270f)
        }

        android.util.Log.d(
            "IMAGE_DEBUG",
            "Original width=${originalBitmap.width}, height=${originalBitmap.height}"
        )
        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
        val newWidth = 1280

        val newHeight = (
                rotatedBitmap.height *
                        (newWidth.toFloat() / rotatedBitmap.width)
                ).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(
            rotatedBitmap,
            newWidth,
            newHeight,
            true
        )

        val outputStream = ByteArrayOutputStream()

        resizedBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            65,
            outputStream
        )

        return outputStream.toByteArray()
    }
     suspend fun createPost(
         userId: String,
         title: String,
         body: String?,
         category: String?,
         areaTag: String?,
         isAnonymous: Boolean,
         postType: String = "normal",
         isAlert: Boolean = false,
         alertSeverity: String? = null,
         imageUrl: String? = null
     ): Result<Post> {
         return try {
             val post = client.postgrest["posts"].insert(
                 buildJsonObject {
                     put("user_id", userId)
                     put("title", title)
                     put("body", body)
                     put("category", category)
                     put("area_tag", areaTag)
                     put("is_anonymous", isAnonymous)
                     put("is_alert", isAlert)
                     put("post_type", postType)
                     put("alert_severity", alertSeverity)
                     put("image_url", imageUrl)
                 }
             ) { select() }.decodeSingle<Post>()

             // Queue this post for future notification evaluation
             client.postgrest["notification_queue"].insert(
                 buildJsonObject {
                     put("post_id", post.id)
                 }
             )


             updateKarma(userId, 5)
             tryAwardBadges(userId, category)

             Result.success(enrichPostWithUsername(post))
         } catch (e: Exception) {
             Result.failure(e)
         }
     }


     suspend fun updatePost(
         postId: String,
         title: String,
         body: String?,
         category: String?,
         areaTag: String?,
         isAnonymous: Boolean,
         postType: String = "normal",
         isAlert: Boolean = false,
         alertSeverity: String? = null,
         imageUrl: String? = null
     ): Result<Unit> {

         return try {

             val currentUserId = authRepository.currentUserId
                 ?: return Result.failure(Exception("You must be logged in"))

             // Get the existing post
             val existingPost = client.postgrest["posts"]
                 .select {
                     filter {
                         eq("id", postId)
                     }
                 }
                 .decodeSingle<Post>()

             val isOwner = existingPost.userId == currentUserId

             // Check whether current user is an admin
             val isAdmin = client.postgrest["admin_roles"]
                 .select {
                     filter {
                         eq("user_id", currentUserId)
                     }
                 }
                 .decodeList<kotlinx.serialization.json.JsonObject>()
                 .isNotEmpty()

             // Only owner or admin can edit
             if (!isOwner && !isAdmin) {
                 return Result.failure(
                     Exception("You are not allowed to edit this post")
                 )
             }

             // Admin edit = mark it as admin-edited
             val editedByAdmin = isAdmin
             val updateData = buildJsonObject {

                 put("title", title.trim())
                 put("body", body?.trim())
                 put("category", category)
                 put("area_tag", areaTag)
                 put("is_anonymous", isAnonymous)
                 put("post_type", postType)
                 put("is_alert", isAlert)
                 put("alert_severity", alertSeverity)

                 // Only replace image when a new image was supplied
                 if (imageUrl != null) {
                     put("image_url", imageUrl)
                 }

                 put("edited_at", kotlinx.datetime.Clock.System.now().toString())
                 put("edited_by_admin", editedByAdmin)
             }

             client.postgrest["posts"].update(updateData) {
                 filter {
                     eq("id", postId)
                 }
             }

             android.util.Log.d(
                 "EDIT_POST",
                 "Post updated successfully. postId=$postId, adminEdit=$editedByAdmin"
             )

             Result.success(Unit)

         } catch (e: Exception) {

             android.util.Log.e(
                 "EDIT_POST",
                 "Post update failed",
                 e
             )

             Result.failure(e)
         }
     }

    // ── Comments ──────────────────────────────────────────────────────────────
    // Raw `comments` rows are private (they contain user_id). Everything the app
    // shows or changes goes through SECURITY DEFINER RPCs that mask anonymous
    // authors, enforce ownership/admin rights and apply rate limits.
    // See supabase/migrations/20261008120000_comments_overhaul.sql.

    /** Emits when a comment in this thread is added, edited or removed (likes do not fire it). */
    fun subscribeToComments(postId: String): Flow<Unit> = flow {
        val channel = client.realtime.channel("thread_events_$postId")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "comment_events"
            filter("post_id", FilterOperator.EQ, postId)
        }.map { Unit }
        channel.subscribe(blockUntilSubscribed = true)
        try {
            inserts.collect { emit(Unit) }
        } finally {
            withContext(NonCancellable) { runCatching { channel.unsubscribe() } }
        }
    }

    /** One page: [rootLimit] top-level comments from [rootOffset] ("top"/"new"/"old") plus all replies. */
    suspend fun getComments(
        postId: String, sort: String = "top", rootLimit: Int = 20, rootOffset: Int = 0
    ): Result<CommentPage> = runCatching {
        val rows = client.postgrest.rpc("get_thread_comments", buildJsonObject {
            put("p_post_id", postId); put("p_sort", sort)
            put("p_limit", rootLimit); put("p_offset", rootOffset)
        }).decodeList<ThreadCommentRow>()
        CommentPage(rows.map { it.toComment() }, rows.firstOrNull()?.totalRoots?.toInt() ?: 0)
    }

    /** Profile comments. Other people's anonymous comments are never returned. */
    suspend fun getCommentsByUser(userId: String): Result<List<Comment>> = runCatching {
        client.postgrest.rpc("get_user_comments", buildJsonObject {
            put("p_user_id", userId); put("p_limit", 100); put("p_offset", 0)
        }).decodeList<ThreadCommentRow>().map { it.toComment() }
    }

    /** Creates a comment or reply. The server assigns the anonymous alias. */
    suspend fun addComment(
        postId: String, body: String, isAnonymous: Boolean, parentId: String? = null
    ): Result<Comment> = runCatching {
        client.postgrest.rpc("add_comment", buildJsonObject {
            put("p_post_id", postId); put("p_body", body); put("p_is_anonymous", isAnonymous)
            if (parentId != null) put("p_parent_id", parentId)
        }).decodeList<ThreadCommentRow>().first().toComment()
    }

    /** Owners and admins can edit. Owner edit => "Edited"; admin edit is silent (audited server-side). */
    suspend fun updateComment(commentId: String, newBody: String): Result<Comment> = runCatching {
        require(newBody.isNotBlank()) { "Comment cannot be empty" }
        client.postgrest.rpc("edit_comment", buildJsonObject {
            put("p_comment_id", commentId); put("p_body", newBody.trim())
        }).decodeList<ThreadCommentRow>().first().toComment()
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = runCatching {
        client.postgrest.rpc("delete_comment", buildJsonObject { put("p_comment_id", commentId) })
        Unit
    }

    suspend fun toggleCommentLike(commentId: String): Result<CommentLikeResult> = runCatching {
        client.postgrest.rpc("toggle_comment_like", buildJsonObject { put("p_comment_id", commentId) })
            .decodeList<CommentLikeResult>().first()
    }

    suspend fun reportComment(commentId: String, reason: String): Result<Unit> = runCatching {
        client.postgrest.rpc("report_comment", buildJsonObject {
            put("p_comment_id", commentId); put("p_reason", reason)
        })
        Unit
    }

     suspend fun reportPost(
         postId: String,
         reportedBy: String,
         reason: String
     ): Result<Unit> {

         return try {

             android.util.Log.d(
                 "POST_REPORT",
                 "Reporting post=$postId by=$reportedBy reason=$reason"
             )

             client.postgrest["post_reports"].insert(
                 buildJsonObject {
                     put("post_id", postId)
                     put("reported_by", reportedBy)
                     put("reason", reason)
                     put("status", "pending")
                 }
             )

             android.util.Log.d("POST_REPORT", "Insert successful")

             Result.success(Unit)

         }

         catch (e: Exception) {

             if (e.message?.contains("duplicate key value") == true) {

                 android.util.Log.d(
                     "POST_REPORT",
                     "User already reported this post."
                 )

                 return Result.failure(
                     Exception("You've already reported this post.")
                 )
             }

             android.util.Log.e(
                 "POST_REPORT",
                 "Insert failed",
                 e
             )

             Result.failure(e)
         }

     }

    // ── Voting ────────────────────────────────────────────────────────────────

    suspend fun votePost(userId: String, postId: String, voteType: String): Result<Unit> {
        return try {
            require(voteType == "up" || voteType == "down") { "Invalid vote type" }
            val currentUserId = authRepository.currentUserId
            require(!currentUserId.isNullOrBlank() && currentUserId == userId) {
                "You must be signed in as the voting user."
            }

            // Vote row, post counters, and owner karma are changed in one DB transaction.
            client.postgrest.rpc(
                "vote_post_atomic",
                parameters = buildJsonObject {
                    put("p_post_id", postId)
                    put("p_vote_type", voteType)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Atomic vote failed for post $postId", e)
            Result.failure(e)
        }
    }

    suspend fun getUserVote(userId: String, postId: String): Result<String?> {
        return try {
            val votes = client.postgrest["votes"].select {
                filter { eq("user_id", userId); eq("post_id", postId) }
            }.decodeList<kotlinx.serialization.json.JsonObject>()
            val voteType = votes.firstOrNull()?.get("vote_type")?.jsonPrimitive?.content
            Result.success(voteType)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * One round-trip replacement for calling [getUserVote] once per post.
     * Returns post_id -> vote_type ("up" / "down"); posts without a vote are absent.
     */
    suspend fun getUserVotesForPosts(
        userId: String,
        postIds: List<String>
    ): Result<Map<String, String>> {
        if (postIds.isEmpty()) return Result.success(emptyMap())
        return try {
            val rows = client.postgrest["votes"].select {
                filter {
                    eq("user_id", userId)
                    isIn("post_id", postIds)
                }
            }.decodeList<kotlinx.serialization.json.JsonObject>()

            val votes = rows.mapNotNull { row ->
                val postId = row["post_id"]?.jsonPrimitive?.content
                val voteType = row["vote_type"]?.jsonPrimitive?.content
                if (postId != null && voteType != null) postId to voteType else null
            }.toMap()
            Result.success(votes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

    suspend fun getAlerts(category: String? = null): Result<List<Post>> {
        return try {
            val posts = client.postgrest["posts"].select {
                filter {
                    eq("is_alert", true)
                    if (category != null) eq("category", category)
                }
                order("created_at", Order.DESCENDING)
                limit(50)
            }.decodeList<Post>()
            Result.success(enrichPostsWithUsernames(posts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

     private suspend fun enrichPostsWithUsernames(posts: List<Post>): List<Post> {
         val userIds = posts
             .filter { !it.isAnonymous }
             .map { it.userId }
             .distinct()

         if (userIds.isEmpty()) return posts

         val profileMap = fetchUserProfiles(userIds)

         return posts.map { post ->
             if (!post.isAnonymous) {
                 val profile = profileMap[post.userId]

                 post.copy(
                     username = profile?.username ?: "unknown",
                     isVerified = profile?.isVerified ?: false,
                     authorAvatarUrl = profile?.avatarUrl
                 )
             } else {
                 post.copy(
                     username = null,
                     isVerified = false,
                     authorAvatarUrl = null
                 )
             }
         }
     }

     private suspend fun enrichPostWithUsername(post: Post): Post {
         if (post.isAnonymous) {
             return post.copy(
                 username = null,
                 isVerified = false
             )
         }

         val profileMap = fetchUserProfiles(listOf(post.userId))
         val profile = profileMap[post.userId]

         return post.copy(
             username = profile?.username ?: "unknown",
             isVerified = profile?.isVerified ?: false
         )
     }

     private suspend fun fetchUserProfiles(
         userIds: List<String>
     ): Map<String, Profile> {
         if (userIds.isEmpty()) return emptyMap()

         return try {
             val profiles = client.postgrest["profiles"].select {
                 filter {
                     isIn("id", userIds)
                 }
             }.decodeList<Profile>()

             profiles.associateBy { it.id }
         } catch (_: Exception) {
             emptyMap()
         }
     }
     private suspend fun fetchUsernames(
         userIds: List<String>
     ): Map<String, String> {
         if (userIds.isEmpty()) return emptyMap()

         return try {
             val profiles = client.postgrest["profiles"].select {
                 filter {
                     isIn("id", userIds)
                 }
             }.decodeList<Profile>()

             profiles.associate { profile ->
                 profile.id to profile.username
             }
         } catch (_: Exception) {
             emptyMap()
         }
     }
    private suspend fun updateKarma(userId: String, delta: Int) {
        try {
            val profile = client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()
                .firstOrNull() ?: return
            val currentKarma = profile["karma"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val newKarma = (currentKarma + delta).coerceAtLeast(0)
            client.postgrest["profiles"].update(mapOf("karma" to newKarma)) {
                filter { eq("id", userId) }
            }
            // Check top contributor badge
            if (newKarma >= 100) tryAwardBadge(userId, "top_contributor")
        } catch (_: Exception) {}
    }

    private suspend fun tryAwardBadges(userId: String, category: String?) {
        try {
            // Count posts in this category for this user
            val posts = client.postgrest["posts"].select {
                if (category == null) return

                val posts = client.postgrest["posts"].select {
                    filter {
                        eq("user_id", userId)
                        eq("category", category)
                    }
                }.decodeList<Post>()
            }.decodeList<Post>()
            val count = posts.size
            when {
                category == "food"      && count >= 10 -> tryAwardBadge(userId, "food_expert")
                category == "nightlife" && count >= 5  -> tryAwardBadge(userId, "night_owl")
            }
            // Area local: 20+ posts in same area
            val allPosts = client.postgrest["posts"].select {
                filter { eq("user_id", userId) }
            }.decodeList<Post>()
            val areaCounts = allPosts.groupBy { it.areaTag }
            if (areaCounts.any { it.value.size >= 20 }) {
                tryAwardBadge(userId, "area_local")
            }
        } catch (_: Exception) {}
    }

    private suspend fun tryAwardBadge(userId: String, badgeType: String) {
        try {
            val existing = client.postgrest["badges"].select {
                filter { eq("user_id", userId); eq("badge_type", badgeType) }
            }.decodeList<kotlinx.serialization.json.JsonObject>()
            if (existing.isEmpty()) {
                client.postgrest["badges"].insert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("badge_type", badgeType)
                    }
                )
            }
        } catch (_: Exception) {}
    }
}