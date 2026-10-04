// java/com/nagpurpulse/ui/screens/admin/AdminDashboardScreen.kt

package com.nagpurpulse.ui.screens.admin

import android.util.Log

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest

import com.nagpurpulse.data.model.*
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════

// 3. POSTS MANAGEMENT
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostsContent(
    viewModel: AdminViewModel,
    initialShowComments: Boolean = false
) {
    val postsState    by viewModel.posts.collectAsState()
    val commentsState by viewModel.comments.collectAsState()
    val loadingIds    by viewModel.loadingItemIds.collectAsState()
    var showComments  by remember(initialShowComments) { mutableStateOf(initialShowComments) }
    var pendingPost   by remember { mutableStateOf<AdminPost?>(null) }
    var pendingComment by remember { mutableStateOf<AdminComment?>(null) }
    var showLimitMenu        by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }
    var showCommentLimitMenu by remember { mutableStateOf(false) }
    var showCommentDatePicker by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) { if (postsState.posts.isEmpty()) viewModel.loadPosts() }
    LaunchedEffect(showComments) {
        if (showComments && commentsState.comments.isEmpty()) viewModel.loadComments()
    }

    if (pendingPost != null) {
        ReasonDialog(
            title     = "Delete Post",
            onDismiss = { pendingPost = null },
            onConfirm = { reason -> viewModel.deletePost(pendingPost!!, reason); pendingPost = null }
        )
    }
    if (pendingComment != null) {
        ReasonDialog(
            title     = "Delete Comment",
            onDismiss = { pendingComment = null },
            onConfirm = { reason -> viewModel.deleteComment(pendingComment!!, reason); pendingComment = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Posts | Comments toggle ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .border(1.dp, Divider, RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            ContentToggle("Posts",    !showComments, Modifier.weight(1f)) { showComments = false }
            ContentToggle("Comments", showComments,  Modifier.weight(1f)) { showComments = true  }
        }

        if (!showComments) {
            // ── Posts management ──────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = postsState.searchQuery,
                    onValueChange = { viewModel.loadPosts(postsState.selectedSort, it) },
                    modifier      = Modifier.weight(1f).height(48.dp),
                    placeholder   = { Text("Search posts...", color = TertiaryText, fontSize = 13.sp) },
                    leadingIcon   = { Icon(Icons.Filled.Search, null, tint = TertiaryText, modifier = Modifier.size(18.dp)) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = OrangePrimary,
                        unfocusedBorderColor = Divider,
                        focusedTextColor     = PrimaryText,
                        unfocusedTextColor   = PrimaryText,
                        cursorColor          = OrangePrimary
                    )
                )

                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Divider, RoundedCornerShape(12.dp))
                            .background(Surface)
                            .pressScale(onClick = { showDatePicker = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = "Filter by date",
                            tint = if (postsState.selectedDate != null)
                                OrangePrimary
                            else
                                SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showDatePicker) {
                        val initialMillis = postsState.selectedDate?.let {
                            runCatching {
                                java.time.LocalDate.parse(it)
                                    .atStartOfDay(java.time.ZoneOffset.UTC)
                                    .toInstant()
                                    .toEpochMilli()
                            }.getOrNull()
                        }

                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = initialMillis
                        )

                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            colors = DatePickerDefaults.colors(
                                containerColor = Color(0xFF171318),
                                selectedDayContainerColor = OrangePrimary,
                                selectedDayContentColor = Color.White,
                                todayDateBorderColor = OrangePrimary,
                                todayContentColor = OrangePrimary
                            ),
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val millis = datePickerState.selectedDateMillis

                                        if (millis != null) {
                                            val selectedDate = java.time.Instant
                                                .ofEpochMilli(millis)
                                                .atZone(java.time.ZoneOffset.UTC)
                                                .toLocalDate()
                                                .toString()

                                            Log.d(
                                                "ADMIN_DATE_DEBUG",
                                                "DATE PICKER -> selectedDate=$selectedDate, millis=$millis"
                                            )

                                            Log.d(
                                                "ADMIN_DATE_DEBUG",
                                                "DATE PICKER -> sort=${postsState.selectedSort}, " +
                                                        "query='${postsState.searchQuery}', " +
                                                        "limit=${postsState.limit}"
                                            )

                                            viewModel.loadPosts(
                                                sort = postsState.selectedSort,
                                                query = postsState.searchQuery,
                                                limit = postsState.limit,
                                                date = selectedDate
                                            )
                                        }

                                        showDatePicker = false
                                    }
                                ) {
                                    Text("Apply", color = OrangePrimary)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showDatePicker = false
                                        viewModel.loadPosts(
                                            sort = postsState.selectedSort,
                                            query = postsState.searchQuery,
                                            limit = postsState.limit,
                                            date = null
                                        )
                                    }
                                ) {
                                    Text("Clear", color = SecondaryText)
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Divider, RoundedCornerShape(12.dp))
                            .background(Surface)
                            .pressScale(onClick = { showLimitMenu = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Items per page",
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showLimitMenu,
                        onDismissRequest = { showLimitMenu = false }
                    ) {
                        listOf(20, 50, 100, 200, 500).forEach { count ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$count posts",
                                        color = PrimaryText
                                    )
                                },
                                onClick = {
                                    showLimitMenu = false
                                    viewModel.loadPosts(
                                        sort = postsState.selectedSort,
                                        query = postsState.searchQuery,
                                        limit = count,
                                        date = postsState.selectedDate
                                    )
                                },
                                trailingIcon = {
                                    if (postsState.limit == count) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            } // closes Posts management Row

            Row(
                modifier = Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminFilterChip("Latest", postsState.selectedSort == "latest") {
                    viewModel.loadPosts(
                        sort = "latest",
                        query = postsState.searchQuery,
                        limit = postsState.limit,
                        date = postsState.selectedDate
                    )
                }

                AdminFilterChip("Most Reported", postsState.selectedSort == "most_reported") {
                    viewModel.loadPosts(
                        sort = "most_reported",
                        query = postsState.searchQuery,
                        limit = postsState.limit,
                        date = postsState.selectedDate
                    )
                }

                AdminFilterChip("Most Upvoted", postsState.selectedSort == "most_upvoted") {
                    viewModel.loadPosts(
                        sort = "most_upvoted",
                        query = postsState.searchQuery,
                        limit = postsState.limit,
                        date = postsState.selectedDate
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            if (postsState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = OrangePrimary) }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(postsState.posts, key = { it.post.id }) { adminPost ->
                        AdminPostCard(
                            adminPost = adminPost,
                            isLoading = adminPost.post.id in loadingIds,
                            onDelete  = { pendingPost = adminPost },
                            onPin     = { viewModel.togglePinPost(adminPost) },
                            onLock    = { viewModel.toggleLockPost(adminPost) },
                            onView    = {}
                        )
                    }
                    if (postsState.posts.isEmpty()) {
                        item { Box(Modifier.fillParentMaxWidth().padding(40.dp), Alignment.Center) { Text("No posts found", color = TertiaryText) } }
                    }
                }
            }
        } else {
            // ── Comments management ───────────────────────────────────────────
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminFilterChip(
                    "Latest",
                    commentsState.selectedSort == "latest"
                ) {
                    viewModel.loadComments(
                        sort = "latest",
                        limit = commentsState.limit,
                        date = commentsState.selectedDate
                    )
                }

                AdminFilterChip(
                    "Most Reported",
                    commentsState.selectedSort == "most_reported"
                ) {
                    viewModel.loadComments(
                        sort = "most_reported",
                        limit = commentsState.limit,
                        date = commentsState.selectedDate
                    )
                }

                Spacer(Modifier.weight(1f))



                // Limit selector
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Divider, RoundedCornerShape(12.dp))
                            .background(Surface)
                            .pressScale(onClick = { showCommentLimitMenu = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Items per page",
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showCommentLimitMenu,
                        onDismissRequest = { showCommentLimitMenu = false }
                    ) {
                        listOf(20, 50, 100, 200, 500).forEach { count ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$count comments",
                                        color = PrimaryText
                                    )
                                },
                                onClick = {
                                    showCommentLimitMenu = false
                                    viewModel.loadComments(
                                        sort = commentsState.selectedSort,
                                        limit = count,
                                        date = commentsState.selectedDate
                                    )
                                },
                                trailingIcon = {
                                    if (commentsState.limit == count) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Date selector
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Divider, RoundedCornerShape(12.dp))
                            .background(Surface)
                            .pressScale(onClick = { showCommentDatePicker = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = "Filter comments by date",
                            tint = if (commentsState.selectedDate != null)
                                OrangePrimary
                            else
                                SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showCommentDatePicker) {
                        val initialMillis = commentsState.selectedDate?.let {
                            runCatching {
                                java.time.LocalDate.parse(it)
                                    .atStartOfDay(java.time.ZoneOffset.UTC)
                                    .toInstant()
                                    .toEpochMilli()
                            }.getOrNull()
                        }

                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = initialMillis
                        )

                        DatePickerDialog(
                            onDismissRequest = { showCommentDatePicker = false },
                            colors = DatePickerDefaults.colors(
                                containerColor = Color(0xFF171318),
                                selectedDayContainerColor = OrangePrimary,
                                selectedDayContentColor = Color.White,
                                todayDateBorderColor = OrangePrimary,
                                todayContentColor = OrangePrimary
                            ),
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val millis = datePickerState.selectedDateMillis

                                        if (millis != null) {
                                            val selectedDate = java.time.Instant
                                                .ofEpochMilli(millis)
                                                .atZone(java.time.ZoneOffset.UTC)
                                                .toLocalDate()
                                                .toString()

                                            viewModel.loadComments(
                                                sort = commentsState.selectedSort,
                                                limit = commentsState.limit,
                                                date = selectedDate
                                            )
                                        }

                                        showCommentDatePicker = false
                                    }
                                ) {
                                    Text("Apply", color = OrangePrimary)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showCommentDatePicker = false

                                        viewModel.loadComments(
                                            sort = commentsState.selectedSort,
                                            limit = commentsState.limit,
                                            date = null
                                        )
                                    }
                                ) {
                                    Text("Clear", color = SecondaryText)
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
            } // closes Comments management Row

            Spacer(Modifier.height(4.dp))

            if (commentsState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = OrangePrimary) }
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(commentsState.comments, key = { it.comment.id }) { adminComment ->
                        AdminCommentCard(
                            adminComment = adminComment,
                            isLoading    = adminComment.comment.id in loadingIds,
                            onDelete     = { pendingComment = adminComment },
                            onWarn       = { viewModel.warnUserFromComment(adminComment) }
                        )
                    }
                    if (commentsState.comments.isEmpty()) {
                        item { Box(Modifier.fillParentMaxWidth().padding(40.dp), Alignment.Center) { Text("No comments to show", color = TertiaryText) } }
                    }
                    item { Text("No more comments to show", color = TertiaryText, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
                }
            }
        }
    }
}

@Composable
private fun AdminPostCard(
    adminPost: AdminPost,
    isLoading: Boolean,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onLock: () -> Unit,
    onView: () -> Unit
) {
    val post = adminPost.post
    val categoryColor = when (post.category) {
        "food"          -> Color(0xFF34C759)
        "nightlife"     -> PurpleNight
        "jobs"          -> BlueInfo
        "college"       -> GreenSuccess
        "rants"         -> OrangePrimary
        "neighborhoods" -> Color(0xFF5AC8FA)
        "lost_found"    -> YellowWarn
        "events"        -> Color(0xFFFF375F)
        "traffic"       -> RedAlert
        "alerts"        -> RedAlert
        else            -> SecondaryText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier  = Modifier.fillMaxWidth().height(2.dp),
                color     = OrangePrimary,
                trackColor = Color.Transparent
            )
        }
        // Pinned ribbon
        if (adminPost.isPinned) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 10.dp))
                    .background(OrangePrimary)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PushPin, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text("Pinned", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(categoryColor.copy(0.15f))
                        .border(1.dp, categoryColor.copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(post.category.replaceFirstChar { it.uppercase() }, color = categoryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                Text(post.timeAgo(), color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.MoreVert, null, tint = TertiaryText, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(post.title, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${post.username ?: "user"}", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (!post.areaTag.isNullOrBlank()) {
                    Text("  •  ${post.areaTag}", color = TertiaryText, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.KeyboardArrowUp, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
                Text("${post.upvotes}", color = TertiaryText, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Chat, null, tint = TertiaryText, modifier = Modifier.size(12.dp))
                Text("  ${post.commentCount}", color = TertiaryText, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Visibility, null, tint = TertiaryText, modifier = Modifier.size(12.dp))
                Text("  ${post.viewCount}", color = TertiaryText, fontSize = 11.sp)
                if (adminPost.reportCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(RedAlert.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = RedAlert,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "${adminPost.reportCount} reports",
                                color = RedAlert,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Admin actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminIconAction(Icons.Filled.Delete,  RedAlert,                          Modifier.weight(1f)) { onDelete() }
                AdminIconAction(Icons.Filled.PushPin, if (adminPost.isPinned) OrangePrimary else SecondaryText, Modifier.weight(1f)) { onPin() }
                AdminIconAction(Icons.Filled.Lock,    if (adminPost.isLocked) BlueInfo else SecondaryText, Modifier.weight(1f)) { onLock() }
                AdminIconAction(Icons.Filled.Visibility, SecondaryText,                 Modifier.weight(1f)) { onView() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 4. USERS MANAGEMENT
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminUsersContent(viewModel: AdminViewModel) {
    val state          by viewModel.users.collectAsState()
    var query          by remember { mutableStateOf("") }
    var showBanDialog  by remember { mutableStateOf(false) }
    var banDays        by remember { mutableStateOf(0) }   // 0=warn, 7=suspend7, 30=suspend30, -1=perm

    val detail = state.userDetail

    LaunchedEffect(Unit) {
        if (state.userResults.isEmpty() && state.userDetail == null) viewModel.loadUsers()
    }

    if (showBanDialog) {
        ReasonDialog(
            title     = when (banDays) {
                0    -> "Warn User"
                7    -> "Suspend 7 Days"
                30   -> "Suspend 30 Days"
                else -> "Permanent Ban"
            },
            onDismiss = { showBanDialog = false },
            onConfirm = { reason ->
                detail?.profile?.id?.let { uid ->
                    when (banDays) {
                        0    -> viewModel.warnUser(uid, reason)
                        -1   -> viewModel.permanentBan(uid, reason)
                        else -> viewModel.suspendUser(uid, banDays, reason)
                    }
                }
                showBanDialog = false
            }
        )
    }

    // Show action success toast
    LaunchedEffect(state.actionSuccess) {
        if (state.actionSuccess != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearUserActionMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            modifier      = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            placeholder   = { Text("Search by username...", color = TertiaryText) },
            leadingIcon   = { Icon(Icons.Filled.Search, null, tint = TertiaryText) },
            singleLine    = true,
            shape         = RoundedCornerShape(14.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { if (query.isNotBlank()) viewModel.searchUser(query) }
            ),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = OrangePrimary,
                unfocusedBorderColor = Divider,
                focusedTextColor     = PrimaryText,
                unfocusedTextColor   = PrimaryText,
                cursorColor          = OrangePrimary
            ),
            trailingIcon  = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { viewModel.searchUser(query) }) {
                        Icon(Icons.Filled.Search, null, tint = OrangePrimary)
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminFilterChip("All Users", state.selectedFilter == "all") { viewModel.loadUsers("all") }
            AdminFilterChip("Verified Users", state.selectedFilter == "verified") { viewModel.loadUsers("verified") }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return@Column
        }

        // Success message
        AnimatedVisibility(visible = state.actionSuccess != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenSuccess.copy(0.15f))
                    .border(1.dp, GreenSuccess.copy(0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = GreenSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        state.actionSuccess ?: "",
                        color = GreenSuccess,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (detail == null) {
            if (state.userResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Group, null, tint = TertiaryText, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (state.selectedFilter == "verified") "No verified users found" else "No users found",
                            color = SecondaryText, fontSize = 15.sp
                        )
                        if (state.error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(state.error!!, color = RedAlert, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.userResults, key = { it.id }) { profile ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Surface)
                                .border(1.dp, Divider, RoundedCornerShape(14.dp))
                                .pressScale(onClick = { viewModel.openUser(profile.id) })
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(46.dp).clip(CircleShape).background(OrangePrimary.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profile.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current).data(profile.avatarUrl).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Filled.Person, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "@${profile.username}",
                                            color = PrimaryText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (state.selectedFilter == "verified") {
                                            Spacer(Modifier.width(5.dp))
                                            Icon(Icons.Filled.CheckCircle, null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "Joined ${profile.createdAt.take(10)}  •  Karma ${profile.karma}",
                                        color = TertiaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = TertiaryText, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TextButton(onClick = { viewModel.clearUserDetail() }) {
                        Icon(Icons.Filled.ArrowBack, null, tint = SecondaryText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back to users", color = SecondaryText)
                    }
                }
                // User card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface)
                            .border(1.dp, Divider, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Avatar
                                Box(
                                    modifier = Modifier.size(64.dp).clip(CircleShape).background(OrangePrimary.copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!detail.profile.avatarUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(detail.profile.avatarUrl).crossfade(true).build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("@${detail.profile.username}", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(OrangePrimary.copy(0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("User", color = OrangePrimary, fontSize = 11.sp)
                                    }
                                }
                                Icon(Icons.Filled.MoreVert, null, tint = TertiaryText, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            // Stats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UserStatBox("Posts",    detail.postCount.toString(),    Modifier.weight(1f))
                                UserStatBox("Comments", detail.commentCount.toString(), Modifier.weight(1f))
                                UserStatBox("Karma",    detail.profile.karma.toString(),Modifier.weight(1f))
                            }
                            // Reports received
                            if (detail.reportCount > 0) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(RedAlert.copy(0.1f))
                                        .border(1.dp, RedAlert.copy(0.25f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(24.dp).clip(CircleShape).background(RedAlert), Alignment.Center) {
                                        Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text("Reports Received:", color = RedAlert, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text(detail.reportCount.toString(), color = RedAlert, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                            // Suspension indicator
                            if (detail.suspension != null) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(YellowWarn.copy(0.1f))
                                        .border(1.dp, YellowWarn.copy(0.3f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Block, null, tint = YellowWarn, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (detail.suspension.isPermanent) "Permanently Banned"
                                        else "Suspended until ${detail.suspension.suspendedUntil?.take(10) ?: "?"}",
                                        color = YellowWarn, fontSize = 12.sp, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Divider.copy(0.5f))
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DateRange, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
                                Text("  Joined: ${detail.profile.createdAt.take(10)}", color = TertiaryText, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        UserActionBtn("Warn", OrangePrimary, false, Icons.Filled.Warning, Modifier.weight(1f)) {
                            banDays = 0; showBanDialog = true
                        }
                        UserActionBtn("Suspend 7 Days", RedAlert, false, Icons.Filled.Block, Modifier.weight(1f)) {
                            banDays = 7; showBanDialog = true
                        }
                    }
                }
                item {
                    UserActionBtn("Suspend 30 Days", RedAlert, false, Icons.Filled.AccessTime, Modifier.fillMaxWidth()) {
                        banDays = 30; showBanDialog = true
                    }
                }
                item {
                    UserActionBtn("Permanent Ban", RedAlert, true, Icons.Filled.Block, Modifier.fillMaxWidth()) {
                        banDays = -1; showBanDialog = true
                    }
                }
            }
        }
    }
}

@Composable
private fun UserStatBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceAlt)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TertiaryText, fontSize = 11.sp)
        Text(value, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun UserActionBtn(label: String, color: Color, filled: Boolean, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (filled) Modifier.background(color)
                else Modifier.border(1.dp, color.copy(0.5f), RoundedCornerShape(12.dp))
            )
            .pressScale(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (filled) Color.White else color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (filled) Color.White else color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 5. ADMIN LOGS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminLogsContent(viewModel: AdminViewModel) {
    val state by viewModel.logs.collectAsState()

    LaunchedEffect(Unit) { if (state.logs.isEmpty()) viewModel.loadLogs() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminFilterChip("All Actions",  state.selectedFilter == "all")       { viewModel.loadLogs("all")      }
            AdminFilterChip("Deletions",    state.selectedFilter == "deletions") { viewModel.loadLogs("deletions") }
            AdminFilterChip("Bans",         state.selectedFilter == "bans")      { viewModel.loadLogs("bans")     }
            AdminFilterChip("Warnings",     state.selectedFilter == "warnings")  { viewModel.loadLogs("warnings") }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (state.logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Assignment,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No admin actions yet", color = SecondaryText, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.logs, key = { it.id }) { action ->
                    AdminLogCard(action)
                }
                item { Text("No more logs to show", color = TertiaryText, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun AdminLogCard(action: AdminAction) {
    var showEditDetails by remember(action.id) { mutableStateOf(false) }
    val isCommentEdit = action.actionType == "edit_comment"
    val editHistory = if (isCommentEdit) {
        runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(action.reason.orEmpty()).jsonObject
        }.getOrNull()
    } else null
    val oldCommentBody = editHistory?.get("old_body")?.let {
        runCatching { it.jsonPrimitive.content }.getOrNull()
    }
    val newCommentBody = editHistory?.get("new_body")?.let {
        runCatching { it.jsonPrimitive.content }.getOrNull()
    }

    val (borderColor, verbColor, icon) = when (action.actionType) {
        "edit_comment"                  -> Triple(OrangePrimary, OrangePrimary, Icons.Filled.Edit)
        "delete_post", "delete_comment" -> Triple(RedAlert,     RedAlert,     Icons.Filled.Delete)
        "suspend_user"                  -> Triple(OrangePrimary, OrangePrimary, Icons.Filled.Block)
        "ban_user"                      -> Triple(RedAlert,     RedAlert,     Icons.Filled.Block)
        "warn_user"                     -> Triple(BlueInfo,     BlueInfo,     Icons.Filled.Warning)
        "resolve_report"                -> Triple(GreenSuccess, GreenSuccess, Icons.Filled.CheckCircle)
        "dismiss_report"                -> Triple(SecondaryText, SecondaryText, Icons.Filled.Cancel)
        "pin_post"                      -> Triple(OrangePrimary, OrangePrimary, Icons.Filled.PushPin)
        "lock_post"                     -> Triple(BlueInfo,     BlueInfo,     Icons.Filled.Lock)
        else                            -> Triple(TertiaryText, TertiaryText, Icons.Filled.Info)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .then(if (isCommentEdit) Modifier.pressScale(onClick = { showEditDetails = true }) else Modifier)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset.Zero,
                    size = size.copy(width = 3.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f)
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Admin avatar
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(OrangePrimary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (!action.adminAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(action.adminAvatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        action.adminUsername ?: "Admin",
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(action.timeAgo(), color = TertiaryText, fontSize = 11.sp, maxLines = 1)
                }
                Spacer(Modifier.height(3.dp))
                // Action description
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val actionLabel = if (isCommentEdit) "Edited comment" else action.actionLabel()
                    Text(
                        text = actionLabel.substringBefore(" "),
                        color = verbColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " " + actionLabel.substringAfter(" ") + " #${action.targetId.take(6)}",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (!isCommentEdit && !action.reason.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceAlt)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "Reason: ${action.reason}",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(borderColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = borderColor, modifier = Modifier.size(16.dp))
        }
    }

    if (showEditDetails) {
        AlertDialog(
            onDismissRequest = { showEditDetails = false },
            title = { Text("Comment edit history") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Admin", color = SecondaryText, fontSize = 12.sp)
                    Text(action.adminUsername ?: "Admin", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                    Text("Comment ID", color = SecondaryText, fontSize = 12.sp)
                    Text(action.targetId, color = PrimaryText, fontSize = 12.sp)
                    Text("Old comment", color = RedAlert, fontWeight = FontWeight.SemiBold)
                    Text(oldCommentBody ?: "Old comment text was not saved for this log.", color = PrimaryText)
                    Divider(color = Divider)
                    Text("New comment", color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                    Text(newCommentBody ?: "New comment text was not saved for this log.", color = PrimaryText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditDetails = false }) { Text("Close") }
            },
            containerColor = Color(0xFF171318),
            titleContentColor = Color(0xFFF7F3F5),
            textContentColor = Color(0xFFC7C0CA),
            shape = RoundedCornerShape(28.dp),)
    }
}


// ══════════════════════════════════════════════════════════════════════════════
