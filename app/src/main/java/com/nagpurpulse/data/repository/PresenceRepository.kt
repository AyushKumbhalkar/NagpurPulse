package com.nagpurpulse.data.repository

import android.util.Log
import com.nagpurpulse.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide ephemeral online status. Users who disable show_online_status are
 * deliberately not tracked, so their presence is not exposed to other clients.
 */
@Serializable
data class PresencePayload(
    val userId: String,
    val visible: Boolean = true
)

@Singleton
class PresenceRepository @Inject constructor(
    private val client: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _onlineUserIds = MutableStateFlow<Set<String>>(emptySet())
    val onlineUserIds: StateFlow<Set<String>> = _onlineUserIds

    private var channelJob: Job? = null
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    @Synchronized
    fun start() {
        if (channelJob?.isActive == true) return
        val userId = authRepository.currentUserId ?: return
        channelJob = scope.launch {
            try {
                val profile = client.postgrest["profiles"].select {
                    filter { eq("id", userId) }
                }.decodeSingle<Profile>()
                val presenceChannel = client.channel("nagpurpulse:online")
                channel = presenceChannel
                val stateFlow = presenceChannel.presenceDataFlow<PresencePayload>()
                val collector = launch {
                    stateFlow.collect { payloads ->
                        _onlineUserIds.value = payloads
                            .filter { it.visible && it.userId.isNotBlank() }
                            .map { it.userId }
                            .toSet()
                    }
                }
                presenceChannel.subscribe(blockUntilSubscribed = true)
                if (profile.showOnlineStatus) {
                    presenceChannel.track(
                        buildJsonObject {
                            put("userId", userId)
                            put("visible", true)
                        }
                    )
                }
                collector.join()
            } catch (e: Exception) {
                Log.w("NP_PRESENCE", "Presence connection failed", e)
                _onlineUserIds.value = emptySet()
            }
        }
    }

    @Synchronized
    fun stop() {
        // Reset lifecycle state immediately. onStart() can happen very quickly
        // when the user returns from Android Recents; if we wait until the
        // asynchronous channel cleanup finishes, start() may see the old job
        // as active and skip reconnecting presence.
        val activeChannel = channel
        channel = null

        channelJob?.cancel()
        channelJob = null
        _onlineUserIds.value = emptySet()

        scope.launch {
            try {
                activeChannel?.untrack()
                activeChannel?.let { client.realtime.removeChannel(it) }
            } catch (e: Exception) {
                Log.w("NP_PRESENCE", "Presence disconnect failed", e)
            }
        }
    }
}
