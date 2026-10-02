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
import com.nagpurpulse.data.model.Post
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
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
            // Enrich with usernames
            Result.success(enrichPostsWithUsernames(posts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostById(id: String): Result<Post> {
        return try {
            val post = client.postgrest["posts"]
                .select { filter { eq("id", id) } }
                .decodeSingle<Post>()
            // Increment view count
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

    suspend fun searchPosts(query: String): Result<List<Post>> {
        return try {
            val posts = client.postgrest["posts"].select {
                filter { ilike("title", "%$query%") }
                order("upvotes", Order.DESCENDING)
                limit(20)
            }.decodeList<Post>()
            Result.success(enrichPostsWithUsernames(posts))
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
            Result.success(enrichPostsWithUsernames(posts))
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.success(enrichPostsWithUsernames(posts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPostImage(
        imageBytes: ByteArray
    ): Result<String> {

        return try {

            val fileName = "${UUID.randomUUID()}.jpg"

            client.storage["post-images"].upload(
                path = fileName,
                data = compressImage(imageBytes)
            )

            val publicUrl = client.storage["post-images"]
                .publicUrl(fileName)

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

    suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val comments = client.postgrest["comments"].select {
                filter { eq("post_id", postId) }
                order("upvotes", Order.DESCENDING)
            }.decodeList<Comment>()
            Result.success(enrichCommentsWithUsernames(withCurrentUserLikeState(comments)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommentsByUser(userId: String): Result<List<Comment>> {
        return try {
            val comments = client.postgrest["comments"].select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<Comment>()
            Result.success(enrichCommentsWithUsernames(withCurrentUserLikeState(comments)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(
        postId: String,
        userId: String,
        body: String,
        isAnonymous: Boolean,
        parentId: String? = null,
        anonAlias: String? = null
    ): Result<Comment> {
        return try {

            android.util.Log.d(
                "ANON_DB",
                "Saving comment. isAnonymous=$isAnonymous, anonAlias=$anonAlias"
            )
            val comment = client.postgrest["comments"].insert(
                buildJsonObject {
                    put("post_id", postId)
                    put("user_id", userId)
                    put("body", body)
                    put("is_anonymous", isAnonymous)
                    if (parentId != null) put("parent_id", parentId)
                    // Store the alias so every anon comment in this thread uses same identity
                    if (anonAlias != null) put("anon_alias", anonAlias)
                }
            ) { select() }.decodeSingle<Comment>()

            // Increment comment_count on the post
            try {
                val post = client.postgrest["posts"]
                    .select { filter { eq("id", postId) } }
                    .decodeSingle<Post>()
                client.postgrest["posts"].update(
                    buildJsonObject {
                        put("comment_count", post.commentCount + 1)
                    }
                ) {
                    filter { eq("id", postId) }
                }

// Increase commenter karma
                updateKarma(userId, 1)
            } catch (_: Exception) {}

            Result.success(enrichCommentWithUsername(comment))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
     suspend fun updateComment(
         commentId: String,
         newBody: String
     ): Result<Comment> = runCatching {

         val currentUserId = authRepository.currentUserId
             ?: throw Exception("You must be logged in")

         if (newBody.isBlank()) {
             throw Exception("Comment cannot be empty")
         }

         val existingComment = client.postgrest["comments"]
             .select {
                 filter {
                     eq("id", commentId)
                 }
             }
             .decodeSingle<Comment>()

         val isOwner = existingComment.userId == currentUserId

         val isAdmin = client.postgrest["admin_roles"]
             .select {
                 filter {
                     eq("user_id", currentUserId)
                 }
             }
             .decodeList<kotlinx.serialization.json.JsonObject>()
             .isNotEmpty()

         if (!isOwner && !isAdmin) {
             throw Exception("You are not allowed to edit this comment")
         }

         val editedByAdmin = isAdmin && !isOwner

         client.postgrest["comments"].update(
             buildJsonObject {
                 put("body", newBody.trim())
                 put("edited_at", kotlinx.datetime.Clock.System.now().toString())
                 put("edited_by_admin", editedByAdmin)
             }
         ) {
             filter {
                 eq("id", commentId)
             }
         }

         if (editedByAdmin) {
             try {
                 client.postgrest["admin_actions"].insert(
                     buildJsonObject {
                         put("admin_id", currentUserId)
                         put("action_type", "edit_comment")
                         put("target_type", "comment")
                         put("target_id", commentId)
                         put(
                            "reason",
                            buildJsonObject {
                                put("old_body", existingComment.body)
                                put("new_body", newBody.trim())
                            }.toString()
                        )
                     }
                 )
             } catch (e: Exception) {
                 android.util.Log.e("EDIT_COMMENT", "Failed to log admin comment edit", e)
             }
         }

         val updatedComment = client.postgrest["comments"]
             .select {
                 filter {
                     eq("id", commentId)
                 }
             }
             .decodeSingle<Comment>()

         // Re-enrich the original author details so admin edits never change the visible identity.
         enrichCommentWithUsername(updatedComment)
     }
    /**
     * Toggle the current user's like on a comment. The comment_likes table is the
     * source of truth; database triggers maintain comments.upvotes and emit
     * notifications only for a newly inserted like.
     *
     * Returns true when the comment is liked after this operation, false when unliked.
     */
    suspend fun upvoteComment(commentId: String, userId: String): Result<Boolean> {
        return try {
            val authenticatedUserId = authRepository.currentUserId
                ?: return Result.failure(Exception("You must be logged in"))
            if (authenticatedUserId != userId) {
                return Result.failure(Exception("Authenticated user mismatch"))
            }

            val existing = client.postgrest["comment_likes"].select {
                filter {
                    eq("comment_id", commentId)
                    eq("user_id", authenticatedUserId)
                }
            }.decodeList<kotlinx.serialization.json.JsonObject>()

            if (existing.isNotEmpty()) {
                client.postgrest["comment_likes"].delete {
                    filter {
                        eq("comment_id", commentId)
                        eq("user_id", authenticatedUserId)
                    }
                }
                Result.success(false)
            } else {
                client.postgrest["comment_likes"].insert(
                    buildJsonObject {
                        put("comment_id", commentId)
                        put("user_id", authenticatedUserId)
                    }
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun withCurrentUserLikeState(comments: List<Comment>): List<Comment> {
        val userId = authRepository.currentUserId
        if (comments.isEmpty() || userId == null) return comments
        return try {
            val commentIds = comments.map { it.id }.distinct()
            val likes = client.postgrest["comment_likes"].select {
                filter {
                    eq("user_id", userId)
                    isIn("comment_id", commentIds)
                }
            }.decodeList<kotlinx.serialization.json.JsonObject>()
            val likedIds = likes.mapNotNull { it["comment_id"]?.jsonPrimitive?.content }.toSet()
            comments.map { it.copy(likedByCurrentUser = it.id in likedIds) }
        } catch (_: Exception) {
            comments
        }
    }

     suspend fun reportComment(
         commentId: String,
         reportedBy: String,
         reason: String
     ): Result<Unit> {
         return try {

             client.postgrest["comment_reports"].insert(
                 buildJsonObject {
                     put("comment_id", commentId)
                     put("reported_by", reportedBy)
                     put("reason", reason)
                     put("status", "pending")
                 }
             )

             Result.success(Unit)

         } catch (e: Exception) {
             Result.failure(e)
         }
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
            // Check existing vote
            val existing = try {
                client.postgrest["votes"].select {
                    filter {
                        eq("user_id", userId)
                        eq("post_id", postId)
                    }
                }.decodeList<kotlinx.serialization.json.JsonObject>()
            } catch (_: Exception) { emptyList() }

            val post = client.postgrest["posts"]
                .select { filter { eq("id", postId) } }
                .decodeSingle<Post>()

            if (existing.isEmpty()) {
                // New vote
                client.postgrest["votes"].insert(
                    mapOf("user_id" to userId, "post_id" to postId, "vote_type" to voteType)
                )
                val newUpvotes   = if (voteType == "up")   post.upvotes   + 1 else post.upvotes
                val newDownvotes = if (voteType == "down") post.downvotes + 1 else post.downvotes
                // Increment karma for post owner
                val karmaChange = if (voteType == "up") 1 else -1
                updateKarma(post.userId, karmaChange)
                client.postgrest["posts"].update(
                    mapOf("upvotes" to newUpvotes, "downvotes" to newDownvotes)
                ) { filter { eq("id", postId) } }
            } else {
                val oldVote = existing[0]["vote_type"]?.jsonPrimitive?.content
                if (oldVote == voteType) {
                    // Toggle off
                    client.postgrest["votes"].delete {
                        filter { eq("user_id", userId); eq("post_id", postId) }
                    }
                    val newUpvotes   = if (voteType == "up")   (post.upvotes   - 1).coerceAtLeast(0) else post.upvotes
                    val newDownvotes = if (voteType == "down") (post.downvotes - 1).coerceAtLeast(0) else post.downvotes
                    val karmaChange  = if (voteType == "up") -1 else 1
                    updateKarma(post.userId, karmaChange)
                    client.postgrest["posts"].update(
                        mapOf("upvotes" to newUpvotes, "downvotes" to newDownvotes)
                    ) { filter { eq("id", postId) } }
                } else {
                    // Change vote
                    client.postgrest["votes"].update(mapOf("vote_type" to voteType)) {
                        filter { eq("user_id", userId); eq("post_id", postId) }
                    }
                    val newUpvotes   = if (voteType == "up") post.upvotes + 1 else (post.upvotes   - 1).coerceAtLeast(0)
                    val newDownvotes = if (voteType == "down") post.downvotes + 1 else (post.downvotes - 1).coerceAtLeast(0)
                    val karmaDelta = when {
                        oldVote == "up" && voteType == "down" -> -2
                        oldVote == "down" && voteType == "up" -> 2
                        else -> 0
                    }
                    if (karmaDelta != 0) {
                        updateKarma(post.userId, karmaDelta)
                    }
                    client.postgrest["posts"].update(
                        mapOf("upvotes" to newUpvotes, "downvotes" to newDownvotes)
                    ) { filter { eq("id", postId) } }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.success(null)
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
                     isVerified = profile?.isVerified ?: false
                 )
             } else {
                 post.copy(
                     username = null,
                     isVerified = false
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

    private suspend fun enrichCommentsWithUsernames(comments: List<Comment>): List<Comment> {
        val userIds = comments.filter { !it.isAnonymous }.map { it.userId }.distinct()
        if (userIds.isEmpty()) return comments
        val map = fetchUsernames(userIds)
        return comments.map { c ->
            if (!c.isAnonymous) c.copy(username = map[c.userId] ?: "unknown")
            else c.copy(username = null)
        }
    }

    private suspend fun enrichCommentWithUsername(comment: Comment): Comment {
        if (comment.isAnonymous) return comment.copy(username = null)
        val map = fetchUsernames(listOf(comment.userId))
        return comment.copy(username = map[comment.userId] ?: "unknown")
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