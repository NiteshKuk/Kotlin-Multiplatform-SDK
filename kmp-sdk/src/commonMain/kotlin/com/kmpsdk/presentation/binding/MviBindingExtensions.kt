package com.kmpsdk.presentation.binding

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.messaging.MessageNotifier
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.presentation.mvi.MviEffect
import com.kmpsdk.presentation.mvi.MviIntent
import com.kmpsdk.presentation.mvi.MviState
import com.kmpsdk.presentation.mvi.MviViewModel
import com.kmpsdk.presentation.state.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

fun <S : MviState, I : MviIntent, E : MviEffect, T> MviViewModel<S, I, E>.bindSyncList(
    scope: CoroutineScope,
    stateUpdater: (S, DataState<List<T>>) -> S,
    observe: () -> Flow<List<T>>,
    refresh: suspend () -> KmpSdkResult<Unit>,
    countLocal: suspend () -> Long,
    onError: ((KmpSdkError) -> Unit)? = null,
    messageNotifier: MessageNotifier? = null,
    config: KmpSdkConfig? = null,
    connectivityMonitor: ConnectivityMonitor? = null,
    autoRefreshOnStart: Boolean = true,
): SyncListController<T> = SyncListController(
    scope = scope,
    observe = observe,
    refresh = refresh,
    countLocal = countLocal,
    onStateChange = { dataState -> setState { stateUpdater(it, dataState) } },
    onError = { error ->
        onError?.invoke(error)
        messageNotifier?.let { emitMessage(it, error.message) }
    },
    autoRefreshOnStart = autoRefreshOnStart,
    config = config,
    connectivityMonitor = connectivityMonitor,
).also { it.start() }
