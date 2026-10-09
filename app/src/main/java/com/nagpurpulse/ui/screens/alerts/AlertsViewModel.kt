//java/com/nagpurpulse/ui/screens/alerts/AlertsViewModel.kt

package com.nagpurpulse.ui.screens.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.repository.AlertRepository
import com.nagpurpulse.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedFilter: String = "all",
    /** True only when the very first load failed and there is nothing to show. */
    val loadFailed: Boolean = false,
    /** Alerts from other people that arrived live; shown behind a "N new" pill. */
    val pendingNew: List<Post> = emptyList(),
    val confirmedIds: Set<String> = emptySet(),
    val lastUpdatedMs: Long? = null,
    /** One-shot message for the snackbar. */
    val message: String? = null,
    val currentUserId: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AlertsUiState(currentUserId = authRepository.currentUserId)
    )
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
        subscribeToRealtime()
    }

    fun loadAlerts(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh && it.alerts.isEmpty(),
                    isRefreshing = refresh,
                    loadFailed = false
                )
            }
            alertRepository.getAlerts().fold(
                onSuccess = { alerts ->
                    val confirmed = alertRepository.getMyConfirmedIds(alerts.map { it.id })
                    _uiState.update {
                        it.copy(
                            alerts = alerts,
                            pendingNew = emptyList(),
                            confirmedIds = confirmed,
                            isLoading = false,
                            isRefreshing = false,
                            loadFailed = false,
                            lastUpdatedMs = System.currentTimeMillis(),
                            currentUserId = authRepository.currentUserId
                        )
                    }
                },
                onFailure = {
                    _uiState.update { s ->
                        s.copy(
                            isLoading = false,
                            isRefreshing = false,
                            loadFailed = s.alerts.isEmpty(),
                            message = if (s.alerts.isNotEmpty())
                                "Couldn't refresh. Showing the last update."
                            else null
                        )
                    }
                }
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    /** Merge live alerts that were waiting behind the "N new" pill. */
    fun showPending() {
        _uiState.update { s ->
            if (s.pendingNew.isEmpty()) s
            else s.copy(
                alerts = s.pendingNew + s.alerts.filter { a -> s.pendingNew.none { it.id == a.id } },
                pendingNew = emptyList()
            )
        }
    }

    fun confirmAlert(postId: String) {
        val s = _uiState.value
        val target = s.alerts.firstOrNull { it.id == postId } ?: return
        if (postId in s.confirmedIds || target.userId == s.currentUserId) return

        _uiState.update {
            it.copy(
                confirmedIds = it.confirmedIds + postId,
                alerts = it.alerts.map { p ->
                    if (p.id == postId) p.copy(confirmCount = p.confirmCount + 1) else p
                }
            )
        }
        viewModelScope.launch {
            alertRepository.confirmAlert(postId).onFailure {
                _uiState.update { cur ->
                    cur.copy(
                        confirmedIds = cur.confirmedIds - postId,
                        alerts = cur.alerts.map { p ->
                            if (p.id == postId) p.copy(confirmCount = (p.confirmCount - 1).coerceAtLeast(0)) else p
                        },
                        message = "Couldn't confirm that. Please try again."
                    )
                }
            }
        }
    }

    fun resolveAlert(postId: String) {
        val target = _uiState.value.alerts.firstOrNull { it.id == postId } ?: return
        if (target.userId != _uiState.value.currentUserId || !target.resolvedAt.isNullOrBlank()) return

        val stamp = java.time.Instant.now().toString()
        _uiState.update {
            it.copy(alerts = it.alerts.map { p -> if (p.id == postId) p.copy(resolvedAt = stamp) else p })
        }
        viewModelScope.launch {
            alertRepository.resolveAlert(postId).onFailure {
                _uiState.update { cur ->
                    cur.copy(
                        alerts = cur.alerts.map { p -> if (p.id == postId) p.copy(resolvedAt = null) else p },
                        message = "Couldn't mark it resolved. Please try again."
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(message = "Marked resolved. Thanks for keeping Nagpur updated.") }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun subscribeToRealtime() {
        viewModelScope.launch {
            alertRepository.subscribeToAlerts().collect { incoming ->
                _uiState.update { s ->
                    when {
                        s.alerts.any { it.id == incoming.id } ||
                            s.pendingNew.any { it.id == incoming.id } -> s
                        // Your own alert, or an empty feed: show it straight away.
                        incoming.userId == authRepository.currentUserId || s.alerts.isEmpty() ->
                            s.copy(alerts = listOf(incoming) + s.alerts, loadFailed = false)
                        else -> s.copy(pendingNew = listOf(incoming) + s.pendingNew)
                    }
                }
            }
        }
    }
}
