// java/com/nagpurpulse/ui/components/CommentThread.kt
package com.nagpurpulse.ui.components

import com.nagpurpulse.data.model.Comment
import com.nagpurpulse.data.model.parseInstantOrNull

/** A reply shown under a top-level comment, flattened to one level. */
data class ThreadReply(val comment: Comment, val replyToName: String?)
data class CommentThread(val root: Comment, val replies: List<ThreadReply>)

/**
 * Groups a flat comment list into conversations. Every descendant of a top-level
 * comment is shown under it in chronological order with a "replying to" label, so
 * no reply is ever hidden however deep the data nests.
 */
fun buildCommentThreads(comments: List<Comment>, sort: String, pinnedId: String? = null): List<CommentThread> {
    val byId = comments.associateBy { it.id }
    val children = comments.groupBy { it.parentId }
    val millis = HashMap<String, Long>()
    fun ms(c: Comment): Long = millis.getOrPut(c.id) { parseInstantOrNull(c.createdAt)?.toEpochMilli() ?: 0L }

    val rootCandidates = comments.filter { it.parentId == null || it.parentId !in byId }
    val sortedRoots = when (sort) {
        "new" -> rootCandidates.sortedByDescending { ms(it) }
        "old" -> rootCandidates.sortedBy { ms(it) }
        else -> rootCandidates.sortedWith(compareByDescending<Comment> { it.upvotes }.thenBy { ms(it) })
    }
    val roots = if (pinnedId != null) sortedRoots.sortedByDescending { it.id == pinnedId } else sortedRoots

    return roots.map { root ->
        val collected = ArrayList<Comment>()
        val stack = ArrayDeque<Comment>()
        stack.addAll(children[root.id].orEmpty())
        while (stack.isNotEmpty()) {
            val c = stack.removeLast()
            collected += c
            stack.addAll(children[c.id].orEmpty())
        }
        val replies = collected.sortedBy { ms(it) }.map { c ->
            val parent = c.parentId?.let { byId[it] }
            val replyTo = if (parent == null || parent.id == root.id || parent.isDeleted) null
            else commentDisplayName(parent)
            ThreadReply(c, replyTo)
        }
        CommentThread(root, replies)
    }
}
