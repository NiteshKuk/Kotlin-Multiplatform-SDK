package com.kmpsdk.presentation.binding

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.presentation.state.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Headless sync-list binder — replaces repetitive observe + refresh ViewModel wiring.
 */
class SyncListController<T>(
    private val scope: CoroutineScope,
    private val observe: () -> Flow<List<T>>,
    private val refresh: suspend () -> KmpSdkResult<Unit>,
    private val countLocal: suspend () -> Long,
    private val onStateChange: (DataState<List<T>>) -> Unit,
    private val onError: ((KmpSdkError) -> Unit)? = null,
    private val autoRefreshOnStart: Boolean = true,
    private val config: KmpSdkConfig? = null,
    private val connectivityMonitor: ConnectivityMonitor? = null,
) {
    private var refreshCompleted = false
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            observe().collectLatest { items ->
                when {
                    items.isNotEmpty() -> {
                        onStateChange(DataState.Success(items))
                        maybeBackgroundRefresh()
                    }
                    refreshCompleted -> onStateChange(DataState.Success(items))
                }
            }
        }

        if (autoRefreshOnStart) {
            refreshNow(showLoading = true)
        }
    }

    fun refreshNow(showLoading: Boolean = false) {
        scope.launch {
            if (showLoading) onStateChange(DataState.Loading)
            refreshCompleted = false

            when (val result = refresh()) {
                is KmpSdkResult.Success -> finalizeAfterRefresh()
                is KmpSdkResult.Failure -> {
                    refreshCompleted = true
                    if (countLocal() > 0) {
                        finalizeAfterRefresh()
                    } else {
                        handleError(result.error)
                    }
                }
            }
        }
    }

    private fun maybeBackgroundRefresh() {
        if (config?.autoRefreshOnObserve != true) return
        if (connectivityMonitor?.isOnline() != true) return

        scope.launch {
            when (refresh()) {
                is KmpSdkResult.Success -> Unit
                is KmpSdkResult.Failure -> Unit
            }
        }
    }

    private fun finalizeAfterRefresh() {
        refreshCompleted = true
        scope.launch {
            if (countLocal() == 0L) {
                onStateChange(DataState.Success(emptyList()))
            }
        }
    }

    private fun handleError(error: KmpSdkError) {
        val dataState = when (error) {
            is KmpSdkError.Network -> DataState.NoNetwork
            else -> DataState.Failure(error)
        }
        onStateChange(dataState)
        onError?.invoke(error)
    }
}
