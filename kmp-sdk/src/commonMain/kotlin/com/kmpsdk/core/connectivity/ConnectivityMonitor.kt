package com.kmpsdk.core.connectivity

import kotlinx.coroutines.flow.StateFlow

enum class ConnectivityStatus {
    Online,
    Offline,
    Unknown,
}

interface ConnectivityMonitor {
    val status: StateFlow<ConnectivityStatus>
    fun isOnline(): Boolean = status.value == ConnectivityStatus.Online
}

expect fun createConnectivityMonitor(): ConnectivityMonitor
