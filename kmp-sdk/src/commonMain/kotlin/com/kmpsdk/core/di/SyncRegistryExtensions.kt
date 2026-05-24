package com.kmpsdk.core.di

import com.kmpsdk.domain.repository.SyncableRepository

/**
 * Registers a [SyncableRepository] with the global [com.kmpsdk.data.sync.SyncCoordinator].
 */
fun KmpSdkRegistry.registerSyncTarget(
    name: String,
    repository: SyncableRepository<*>,
) {
    context.syncCoordinator.register(name) { repository.refresh() }
}

fun KmpSdkRegistry.registerSyncTarget(
    name: String,
    refresh: suspend () -> com.kmpsdk.domain.error.KmpSdkResult<Unit>,
) {
    context.syncCoordinator.register(name, refresh)
}
