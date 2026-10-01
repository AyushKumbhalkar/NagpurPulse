// ui/screens/admin/AdminViewModel.kt
package com.nagpurpulse.ui.screens.admin


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.model.*
import com.nagpurpulse.data.repository.AdminRepository
import com.nagpurpulse.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State holders ─────────────────────────────────────────────────────────────

data class AdminDashboardState(
    val stats: AdminStats = AdminStats(),
    val weeklyActivity: List<Int> = List(7) { 0 },
    val recentQueue: List<QueueItem> = emptyList(),
    val adminRole: String = "",
    val adminProfile: com.nagpurpulse.data.model.Profile? = null,
    val isLoading: Boolean = true
)

data class AdminQueueState(
    val items: List<QueueItem> = emptyList(),
    val selectedFilter: String = "all",   // all | posts | comments
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AdminPostsState(
    val posts: List<AdminPost> = emptyList(),
    val selectedSort: String = "latest",
    val searchQuery: String = "",
    val limit: Int = 20,
    val selectedDate: String? = null, // yyyy-MM-dd
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AdminCommentsState(
    val comments: List<AdminComment> = emptyList(),
    val selectedSort: String = "latest",
    val searchQuery: String = "",
    val limit: Int = 20,
    val selectedDate: String? = null, // yyyy-MM-dd
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AdminUsersState(
    val searchQuery: String = "",
    val selectedFilter: String = "all", // all | verified
    val userResults: List<com.nagpurpulse.data.model.Profile> = emptyList(),
    val userDetail: AdminUserDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionSuccess: String? = null
)

data class AdminLogsState(
    val logs: List<AdminAction> = emptyList(),
    val selectedFilter: String = "all",   // all | deletions | bans | warnings
    val isLoading: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _dashboard = MutableStateFlow(AdminDashboardState())
    val dashboard: StateFlow<AdminDashboardState> = _dashboard.asStateFlow()

    private val _queue = MutableStateFlow(AdminQueueState())
    val queue: StateFlow<AdminQueueState> = _queue.asStateFlow()

    private val _posts = MutableStateFlow(AdminPostsState())
    val posts: StateFlow<AdminPostsState> = _posts.asStateFlow()

    private val _comments = MutableStateFlow(AdminCommentsState())
    val comments: StateFlow<AdminCommentsState> = _comments.asStateFlow()

    private val _users = MutableStateFlow(AdminUsersState())
    val users: StateFlow<AdminUsersState> = _users.asStateFlow()

    private val _logs = MutableStateFlow(AdminLogsState())
    val logs: StateFlow<AdminLogsState> = _logs.asStateFlow()

    // Track ongoing operations for individual items
    private val _loadingItemIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingItemIds: StateFlow<Set<String>> = _loadingItemIds.asStateFlow()

    init {
        loadDashboard()
    }

    // ── Dashboard ────────────────────────────────────────────────────────────────

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboard.value = _dashboard.value.copy(isLoading = true)

            val role = adminRepository.getAdminRole() ?: ""
            val profile = try { authRepository.getCurrentProfile().getOrNull() } catch (_: Exception) { null }

            adminRepository.getDashboardStats().fold(
                onSuccess = { stats ->
                    _dashboard.value = _dashboard.value.copy(stats = stats, adminRole = role, adminProfile = profile)
                },
                onFailure = {}
            )

            adminRepository.getWeeklyActivity().fold(
                onSuccess = { activity ->
                    _dashboard.value = _dashboard.value.copy(weeklyActivity = activity)
                },
                onFailure = {}
            )

            // Load first 3 queue items for dashboard preview
            adminRepository.getModerationQueue("all").fold(
                onSuccess = { items ->
                    _dashboard.value = _dashboard.value.copy(
                        recentQueue = items.take(3),
                        isLoading   = false
                    )
                },
                onFailure = {
                    _dashboard.value = _dashboard.value.copy(isLoading = false)
                }
            )
        }
    }

    // ── Queue ─────────────────────────────────────────────────────────────────────

    fun loadQueue(filter: String = "all") {
        viewModelScope.launch {
            _queue.value = _queue.value.copy(isLoading = true, selectedFilter = filter, error = null)
            adminRepository.getModerationQueue(filter).fold(
                onSuccess = { items ->
                    _queue.value = _queue.value.copy(items = items, isLoading = false)
                },
                onFailure = { e ->
                    _queue.value = _queue.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun resolveReport(item: QueueItem) {
        setItemLoading(item.id, true)
        viewModelScope.launch {
            adminRepository.resolveReport(item.id, item.type).fold(
                onSuccess = {
                    setItemLoading(item.id, false)
                    loadQueue(_queue.value.selectedFilter)
                    loadDashboard()
                },
                onFailure = { setItemLoading(item.id, false) }
            )
        }
    }

    fun dismissReport(item: QueueItem) {
        setItemLoading(item.id, true)
        viewModelScope.launch {
            adminRepository.dismissReport(item.id, item.type).fold(
                onSuccess = {
                    setItemLoading(item.id, false)
                    loadQueue(_queue.value.selectedFilter)
                    loadDashboard()
                },
                onFailure = { setItemLoading(item.id, false) }
            )
        }
    }

    fun deleteFromQueue(item: QueueItem, reason: String) {
        Log.d("ADMIN_DELETE", "Delete requested")
        Log.d("ADMIN_DELETE", "QueueItem ID = ${item.id}")
        Log.d("ADMIN_DELETE", "Target ID = ${item.targetId}")
        Log.d("ADMIN_DELETE", "Type = ${item.type}")
        Log.d("ADMIN_DELETE", "Reason = $reason")

        setItemLoading(item.id, true)

        viewModelScope.launch {

            Log.d("ADMIN_DELETE", "Calling repository...")

            val result =
                if (item.type == "post")
                    adminRepository.deletePost(item.targetId, reason)
                else
                    adminRepository.deleteComment(item.targetId, reason)

            result.fold(
                onSuccess = {
                    Log.d("ADMIN_DELETE", "Repository SUCCESS")

                    setItemLoading(item.id, false)
                    loadQueue(_queue.value.selectedFilter)
                    loadDashboard()
                },
                onFailure = { e ->
                    Log.e("ADMIN_DELETE", "Repository FAILED", e)

                    setItemLoading(item.id, false)
                }
            )
        }
    }

    fun warnUserFromQueue(item: QueueItem) {
        if (item.authorUserId.isBlank()) return
        viewModelScope.launch {
            adminRepository.warnUser(
                userId = item.authorUserId,
                reason = "Community guideline violation: ${item.topReason}"
            ).fold(
                onSuccess = { loadDashboard() },
                onFailure = {}
            )
        }
    }

    // ── Posts ─────────────────────────────────────────────────────────────────────

    fun loadPosts(
        sort: String = _posts.value.selectedSort,
        query: String = _posts.value.searchQuery,
        limit: Int = _posts.value.limit,
        date: String? = _posts.value.selectedDate
    ) {
        viewModelScope.launch {
            _posts.value = _posts.value.copy(
                isLoading = true,
                selectedSort = sort,
                searchQuery = query,
                limit = limit,
                selectedDate = date,
                error = null
            )
            Log.d(
                "ADMIN_DATE_DEBUG",
                "VIEWMODEL -> loadPosts(date=$date, sort=$sort, query='$query', limit=$limit)"
            )
            adminRepository.getAllPosts(
                sortBy = sort,
                query = query.takeIf { it.isNotBlank() },
                limit = limit,
                date = date
            ).fold(
                onSuccess = { posts ->

                    Log.d(
                        "ADMIN_DATE_DEBUG",
                        "VIEWMODEL -> SUCCESS, date=$date, received=${posts.size} posts"
                    )

                    posts.forEachIndexed { index, post ->
                        Log.d(
                            "ADMIN_DATE_DEBUG",
                            "VIEWMODEL -> post[$index] id=${post.post.id}, " +
                                    "createdAt=${post.post.createdAt}, " +
                                    "title='${post.post.title}'"
                        )
                    }

                    _posts.value = _posts.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                },
                onFailure = { e ->

                    Log.e(
                        "ADMIN_DATE_DEBUG",
                        "VIEWMODEL -> FAILED, date=$date",
                        e
                    )

                    _posts.value = _posts.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun deletePost(adminPost: AdminPost, reason: String) {
        setItemLoading(adminPost.post.id, true)
        viewModelScope.launch {
            adminRepository.deletePost(adminPost.post.id, reason).fold(
                onSuccess = {
                    setItemLoading(adminPost.post.id, false)
                    loadPosts()
                    loadDashboard()
                },
                onFailure = { setItemLoading(adminPost.post.id, false) }
            )
        }
    }

    fun togglePinPost(adminPost: AdminPost) {
        viewModelScope.launch {
            adminRepository.pinPost(adminPost.post.id, !adminPost.isPinned).fold(
                onSuccess = { loadPosts() },
                onFailure = {}
            )
        }
    }

    fun toggleLockPost(adminPost: AdminPost) {
        viewModelScope.launch {
            adminRepository.lockPost(adminPost.post.id, !adminPost.isLocked).fold(
                onSuccess = { loadPosts() },
                onFailure = {}
            )
        }
    }

    // ── Comments ──────────────────────────────────────────────────────────────────

    fun loadComments(
        sort: String = _comments.value.selectedSort,
        limit: Int = _comments.value.limit,
        date: String? = _comments.value.selectedDate
    ) {
        viewModelScope.launch {
            _comments.value = _comments.value.copy(
                isLoading = true,
                selectedSort = sort,
                limit = limit,
                selectedDate = date,
                error = null
            )

            adminRepository.getAllComments(
                sortBy = sort,
                limit = limit,
                date = date
            ).fold(
                onSuccess = { comments ->
                    _comments.value = _comments.value.copy(
                        comments = comments,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _comments.value = _comments.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun deleteComment(adminComment: AdminComment, reason: String) {
        setItemLoading(adminComment.comment.id, true)
        viewModelScope.launch {
            adminRepository.deleteComment(adminComment.comment.id, reason).fold(
                onSuccess = {
                    setItemLoading(adminComment.comment.id, false)
                    loadComments()
                },
                onFailure = { setItemLoading(adminComment.comment.id, false) }
            )
        }
    }

    fun warnUserFromComment(adminComment: AdminComment) {
        viewModelScope.launch {
            adminRepository.warnUser(
                userId = adminComment.comment.userId,
                reason = "Inappropriate comment"
            ).fold(
                onSuccess = {
                    _comments.value = _comments.value.copy(error = null)
                },
                onFailure = {}
            )
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────────

    fun loadUsers(filter: String = _users.value.selectedFilter) {
        viewModelScope.launch {
            _users.value = _users.value.copy(
                isLoading = true,
                selectedFilter = filter,
                userResults = emptyList(),
                userDetail = null,
                error = null,
                actionSuccess = null
            )
            adminRepository.getUsers(filter).fold(
                onSuccess = { profiles ->
                    _users.value = _users.value.copy(
                        isLoading = false,
                        userResults = profiles
                    )
                },
                onFailure = { e ->
                    _users.value = _users.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun openUser(userId: String) {
        viewModelScope.launch {
            _users.value = _users.value.copy(isLoading = true, error = null)
            adminRepository.getUserDetail(userId).fold(
                onSuccess = { detail -> _users.value = _users.value.copy(isLoading = false, userDetail = detail) },
                onFailure = { e -> _users.value = _users.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun clearUserDetail() {
        _users.value = _users.value.copy(userDetail = null, error = null)
    }

    fun searchUser(query: String) {
        if (query.isBlank()) {
            _users.value = _users.value.copy(userDetail = null, searchQuery = "", error = null)
            return
        }
        viewModelScope.launch {
            _users.value = _users.value.copy(isLoading = true, searchQuery = query, error = null, actionSuccess = null)
            adminRepository.searchUser(query).fold(
                onSuccess = { detail ->
                    _users.value = _users.value.copy(userDetail = detail, isLoading = false,
                        error = if (detail == null) "User not found" else null)
                },
                onFailure = { e ->
                    _users.value = _users.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun warnUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.warnUser(userId, reason).fold(
                onSuccess = {
                    _users.value = _users.value.copy(actionSuccess = "Warning sent to user")
                    loadLogs(_logs.value.selectedFilter)
                },
                onFailure = { e -> _users.value = _users.value.copy(error = e.message) }
            )
        }
    }

    fun suspendUser(userId: String, days: Int, reason: String) {
        viewModelScope.launch {
            _users.value = _users.value.copy(isLoading = true)
            adminRepository.suspendUser(userId, days, reason).fold(
                onSuccess = {
                    _users.value = _users.value.copy(
                        isLoading     = false,
                        actionSuccess = "User suspended for $days days"
                    )
                    // Refresh user detail
                    adminRepository.getUserDetail(userId).fold(
                        onSuccess = { detail -> _users.value = _users.value.copy(userDetail = detail) },
                        onFailure = {}
                    )
                    loadLogs(_logs.value.selectedFilter)
                },
                onFailure = { e -> _users.value = _users.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun permanentBan(userId: String, reason: String) {
        viewModelScope.launch {
            _users.value = _users.value.copy(isLoading = true)
            adminRepository.permanentBan(userId, reason).fold(
                onSuccess = {
                    _users.value = _users.value.copy(
                        isLoading     = false,
                        actionSuccess = "User permanently banned"
                    )
                    adminRepository.getUserDetail(userId).fold(
                        onSuccess = { detail -> _users.value = _users.value.copy(userDetail = detail) },
                        onFailure = {}
                    )
                    loadLogs(_logs.value.selectedFilter)
                },
                onFailure = { e -> _users.value = _users.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun clearUserActionMessage() {
        _users.value = _users.value.copy(actionSuccess = null, error = null)
    }

    // ── Logs ──────────────────────────────────────────────────────────────────────

    fun loadLogs(filter: String = "all") {
        viewModelScope.launch {
            _logs.value = _logs.value.copy(isLoading = true, selectedFilter = filter)
            adminRepository.getAdminLogs(filter).fold(
                onSuccess = { logs -> _logs.value = _logs.value.copy(logs = logs, isLoading = false) },
                onFailure = { _logs.value = _logs.value.copy(isLoading = false) }
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun setItemLoading(id: String, loading: Boolean) {
        _loadingItemIds.value = if (loading)
            _loadingItemIds.value + id
        else
            _loadingItemIds.value - id
    }
}
