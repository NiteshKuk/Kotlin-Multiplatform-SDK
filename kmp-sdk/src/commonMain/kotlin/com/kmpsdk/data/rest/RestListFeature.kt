package com.kmpsdk.data.rest

import com.kmpsdk.core.di.KmpSdkContext
import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.core.di.registerSyncTarget
import com.kmpsdk.data.repository.BaseSyncRepository
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.repository.SyncableRepository
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first list repository backed by GET [path] + host-provided local store hooks.
 *
 * Prefer [installRestListFeature] (or [installRestResourceFeature]) so the network GET
 * uses a reified DTO type correctly.
 */
class RestListRepository<TDomain>(
    featureName: String,
    observeLocal: () -> Flow<List<TDomain>>,
    countLocal: suspend () -> Long,
    syncRemote: suspend () -> KmpSdkResult<Unit>,
    ctx: KmpSdkContext,
) : BaseSyncRepository<TDomain>(
    tag = featureName,
    observeLocal = observeLocal,
    countLocal = countLocal,
    syncRemote = syncRemote,
    connectivityMonitor = ctx.connectivityMonitor,
    syncPolicy = ctx.config.syncPolicy,
    logger = ctx.logger,
    syncStatusStore = ctx.syncCoordinator.statusStore,
    syncTargetName = featureName,
), SyncableRepository<TDomain>

data class RestListFeatureConfig<TDomain, TDto>(
    val name: String,
    val path: String,
    val observeLocal: () -> Flow<List<TDomain>>,
    val countLocal: suspend () -> Long,
    val replaceLocal: suspend (List<TDto>) -> Unit,
)

/**
 * Registers a [RestListRepository] for GET-list sync and a named sync target.
 *
 * Must be called from an `inline` reified context (this function is inline) so
 * `networkClient.get<List<TDto>>` can serialize/deserialize correctly.
 */
inline fun <reified TDomain : Any, reified TDto : Any> KmpSdkRegistry.installRestListFeature(
    config: RestListFeatureConfig<TDomain, TDto>,
) {
    register<RestListRepository<TDomain>> { ctx ->
        RestListRepository(
            featureName = config.name,
            observeLocal = config.observeLocal,
            countLocal = config.countLocal,
            syncRemote = {
                when (val result = ctx.networkClient.get<List<TDto>>(config.path)) {
                    is KmpSdkResult.Success -> {
                        config.replaceLocal(result.data)
                        KmpSdkResult.Success(Unit)
                    }
                    is KmpSdkResult.Failure -> result
                }
            },
            ctx = ctx,
        )
    }
    registerSyncTarget(config.name) {
        resolve<RestListRepository<TDomain>>().refresh()
    }
}
