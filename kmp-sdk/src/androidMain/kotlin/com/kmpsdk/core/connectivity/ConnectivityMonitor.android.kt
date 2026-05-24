package com.kmpsdk.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.kmpsdk.KmpSdkAndroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class AndroidConnectivityMonitor(
    context: Context,
) : ConnectivityMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _status = MutableStateFlow(currentStatus())
    override val status: StateFlow<ConnectivityStatus> = _status.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _status.value = ConnectivityStatus.Online
        }

        override fun onLost(network: Network) {
            _status.value = ConnectivityStatus.Offline
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        _status.value = currentStatus()
    }

    private fun currentStatus(): ConnectivityStatus {
        val network = connectivityManager.activeNetwork ?: return ConnectivityStatus.Offline
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectivityStatus.Offline
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            ConnectivityStatus.Online
        } else {
            ConnectivityStatus.Offline
        }
    }
}

actual fun createConnectivityMonitor(): ConnectivityMonitor =
    AndroidConnectivityMonitor(KmpSdkAndroid.requireContext())
