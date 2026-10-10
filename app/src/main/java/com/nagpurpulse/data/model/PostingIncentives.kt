package com.nagpurpulse.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row shapes returned by the posting-incentive RPCs
 * (supabase/migrations/20261011120000_posting_incentives.sql).
 * Every field has a default so a partial or older server response never crashes decoding.
 */

/** A personalised "what to post about" idea (get_post_prompts). */
@Serializable
data class PostPrompt(
    @SerialName("prompt_key") val key: String = "",
    val category: String = "community",
    val title: String = "",
    val hint: String = "",
    /** "quick" (one line) or "normal". */
    val effort: String = "normal",
    /** first_post | easy_start | right_now | this_weekend | nobody_posted_yet | your_interest | fresh_idea */
    val reason: String = "",
    @SerialName("area_tag") val areaTag: String = "Nagpur",
    val score: Double = 0.0
)

/** Streak, goals and social proof for the signed-in user (get_posting_momentum). */
@Serializable
data class PostingMomentum(
    @SerialName("posts_total") val postsTotal: Int = 0,
    @SerialName("posts_7d") val posts7d: Int = 0,
    @SerialName("comments_7d") val comments7d: Int = 0,
    @SerialName("streak_days") val streakDays: Int = 0,
    @SerialName("active_today") val activeToday: Boolean = false,
    @SerialName("last_post_at") val lastPostAt: String? = null,
    @SerialName("replies_received_7d") val repliesReceived7d: Int = 0,
    @SerialName("upvotes_received_14d") val upvotesReceived14d: Int = 0,
    @SerialName("posts_today") val postsToday: Int = 0,
    @SerialName("people_posted_today") val peoplePostedToday: Int = 0,
    /** first_post | three_posts | seven_day_streak | ten_posts_week */
    @SerialName("next_goal") val nextGoal: String = "",
    @SerialName("goal_progress") val goalProgress: Int = 0,
    @SerialName("goal_target") val goalTarget: Int = 1
)

/** Whether and how to nudge the user right now (get_posting_nudge). */
@Serializable
data class PostingNudge(
    @SerialName("should_nudge") val shouldNudge: Boolean = false,
    /** none | reply_back | first_post | streak | comeback */
    val kind: String = "none",
    val title: String? = null,
    val body: String? = null,
    val category: String? = null,
    @SerialName("area_tag") val areaTag: String? = null
)
