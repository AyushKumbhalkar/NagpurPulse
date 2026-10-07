package com.nagpurpulse.ui.screens.auth

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** U8: live "is there internet" state. Starts as true so the banner never flashes on open. */
@Composable
internal fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current
    val online = remember { mutableStateOf(true) }
    DisposableEffect(context) {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        if (cm == null) {
            onDispose { }
        } else {
            val main = android.os.Handler(android.os.Looper.getMainLooper())
            online.value = cm.getNetworkCapabilities(cm.activeNetwork)
                ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    main.post { online.value = true }
                }
                override fun onLost(network: android.net.Network) {
                    main.post { online.value = false }
                }
                override fun onCapabilitiesChanged(
                    network: android.net.Network,
                    caps: android.net.NetworkCapabilities
                ) {
                    val hasInternet = caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    main.post { online.value = hasInternet }
                }
            }
            runCatching { cm.registerDefaultNetworkCallback(callback, main) }
            onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
        }
    }
    return online
}
