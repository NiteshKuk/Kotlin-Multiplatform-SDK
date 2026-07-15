package com.kmpsdk.data.rest

import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.repository.SyncableRepository
import com.kmpsdk.domain.usecase.RestMutationUseCase
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.Flow

/**
 * CRUD façade over a synced list repository + REST mutations.
 *
 * - [observeAll] / [refresh] → GET list sync (Path C local store)
 * - [create] / [update] / [patch] / [delete] → POST/PUT/PATCH/DELETE
 *
 * After a successful mutation, the list is refreshed by default so UI watching
 * [observeAll] picks up server changes (or queued-offline replay later).
 */
class RestResourceApi<TDomain>(
    @PublishedApi internal val repository: SyncableRepository<TDomain>,
    @PublishedApi internal val networkClient: KmpNetworkClient,
    val basePath: String,
    val createPath: String = basePath,
    val updatePathTemplate: String = joinRestPath(basePath, "{id}"),
    val deletePathTemplate: String = joinRestPath(basePath, "{id}"),
    val refreshAfterMutation: Boolean = true,
) {
    fun observeAll(): Flow<List<TDomain>> = repository.observeAll()

    suspend fun countLocal(): Long = repository.countLocal()

    suspend fun refresh(): KmpSdkResult<Unit> = repository.refresh()

    suspend inline fun <reified TBody : Any> create(
        body: TBody,
        path: String = createPath,
    ): KmpSdkResult<Unit> {
        val useCase = RestMutationUseCase.create<TBody>(
            networkClient = networkClient,
            path = path,
            method = HttpMethod.Post,
            onSuccess = { maybeRefresh() },
        )
        return useCase.execute(body)
    }

    suspend inline fun <reified TBody : Any> update(
        id: String,
        body: TBody,
        path: String = resolveRestPath(updatePathTemplate, id),
    ): KmpSdkResult<Unit> {
        val useCase = RestMutationUseCase.create<TBody>(
            networkClient = networkClient,
            path = path,
            method = HttpMethod.Put,
            onSuccess = { maybeRefresh() },
        )
        return useCase.execute(body)
    }

    suspend inline fun <reified TBody : Any> patch(
        id: String,
        body: TBody,
        path: String = resolveRestPath(updatePathTemplate, id),
    ): KmpSdkResult<Unit> {
        val useCase = RestMutationUseCase.create<TBody>(
            networkClient = networkClient,
            path = path,
            method = HttpMethod.Patch,
            onSuccess = { maybeRefresh() },
        )
        return useCase.execute(body)
    }

    suspend fun delete(
        id: String,
        path: String = resolveRestPath(deletePathTemplate, id),
    ): KmpSdkResult<Unit> {
        val useCase = RestMutationUseCase.createDelete(
            networkClient = networkClient,
            path = path,
            onSuccess = { maybeRefresh() },
        )
        return useCase.execute()
    }

    @PublishedApi
    internal suspend fun maybeRefresh() {
        if (refreshAfterMutation) {
            repository.refresh()
        }
    }
}

data class RestResourceFeatureConfig<TDomain, TDto>(
    val name: String,
    val path: String,
    val observeLocal: () -> Flow<List<TDomain>>,
    val countLocal: suspend () -> Long,
    val replaceLocal: suspend (List<TDto>) -> Unit,
    val createPath: String? = null,
    val updatePath: String? = null,
    val deletePath: String? = null,
    val refreshAfterMutation: Boolean = true,
)

/**
 * Installs GET list sync ([installRestListFeature]) plus a [RestResourceApi] for
 * POST / PUT / PATCH / DELETE — the List + Mutation Feature Kit entry point.
 */
inline fun <reified TDomain : Any, reified TDto : Any> KmpSdkRegistry.installRestResourceFeature(
    config: RestResourceFeatureConfig<TDomain, TDto>,
) {
    installRestListFeature(
        RestListFeatureConfig(
            name = config.name,
            path = config.path,
            observeLocal = config.observeLocal,
            countLocal = config.countLocal,
            replaceLocal = config.replaceLocal,
        ),
    )
    register<RestResourceApi<TDomain>> { ctx ->
        RestResourceApi(
            repository = resolve<RestListRepository<TDomain>>(),
            networkClient = ctx.networkClient,
            basePath = config.path,
            createPath = config.createPath ?: config.path,
            updatePathTemplate = config.updatePath ?: joinRestPath(config.path, "{id}"),
            deletePathTemplate = config.deletePath ?: joinRestPath(config.path, "{id}"),
            refreshAfterMutation = config.refreshAfterMutation,
        )
    }
}
