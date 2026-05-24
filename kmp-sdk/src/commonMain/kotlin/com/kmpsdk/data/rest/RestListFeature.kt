package com.kmpsdk.data.rest

import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.core.di.registerSyncTarget
import com.kmpsdk.data.repository.BaseSyncRepository
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.repository.SyncableRepository
import kotlinx.coroutines.flow.Flow

class RestListRepository<TDomain, TDto>(
    private val featureName: String,
    private val path: String,
    private val observeLocal: () -> Flow<List<TDomain>>,
    private val countLocal: suspend () -> Long,
    private val replaceLocal: suspend (List<TDto>) -> Unit,
    ctx: com.kmpsdk.core.di.KmpSdkContext,
) : BaseSyncRepository<TDomain>(
    tag = featureName,
    observeLocal = observeLocal,
    countLocal = countLocal,
    syncRemote = {
        when (val result = ctx.networkClient.get<List<TDto>>(path)) {
            is KmpSdkResult.Success -> {
                replaceLocal(result.data)
                KmpSdkResult.Success(Unit)
            }
            is KmpSdkResult.Failure -> result
        }
    },
    connectivityMonitor = ctx.connectivityMonitor,
    syncPolicy = ctx.config.syncPolicy,
    logger = ctx.logger,
), SyncableRepository<TDomain>

data class RestListFeatureConfig<TDomain, TDto>(
    val name: String,
    val path: String,
    val observeLocal: () -> Flow<List<TDomain>>,
    val countLocal: suspend () -> Long,
    val replaceLocal: suspend (List<TDto>) -> Unit,
)

inline fun <reified TDomain : Any, reified TDto : Any> KmpSdkRegistry.installRestListFeature(
    config: RestListFeatureConfig<TDomain, TDto>,
) {
    register<RestListRepository<TDomain, TDto>> { ctx ->
        RestListRepository(
            featureName = config.name,
            path = config.path,
            observeLocal = config.observeLocal,
            countLocal = config.countLocal,
            replaceLocal = config.replaceLocal,
            ctx = ctx,
        )
    }
    registerSyncTarget(config.name) {
        resolve<RestListRepository<TDomain, TDto>>().refresh()
    }
}
