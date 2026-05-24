package com.kmpsdk.core.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_status_unsatisfied
import platform.darwin.dispatch_queue_create

private class IosConnectivityMonitor : ConnectivityMonitor {
    private val _status = MutableStateFlow(ConnectivityStatus.Unknown)
    override val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    private val monitor = nw_path_monitor_create()

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val mapped = when (nw_path_get_status(path)) {
                nw_path_status_satisfied -> ConnectivityStatus.Online
                nw_path_status_unsatisfied -> ConnectivityStatus.Offline
                else -> ConnectivityStatus.Unknown
            }
            _status.value = mapped
        }
        val queue = dispatch_queue_create("com.kmpsdk.connectivity", null)
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }
}

actual fun createConnectivityMonitor(): ConnectivityMonitor = IosConnectivityMonitor()
