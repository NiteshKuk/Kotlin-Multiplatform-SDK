package com.kmpsdk.domain.usecase

import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.data.network.setJsonBodyWithOfflineCapture
import com.kmpsdk.domain.error.KmpSdkResult
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Standard POST / PUT / PATCH / DELETE helper with optional offline body capture.
 *
 * Prefer [com.kmpsdk.data.rest.RestResourceApi] when you also need list sync (GET).
 */
class RestMutationUseCase private constructor(
    private val networkClient: KmpNetworkClient,
    private val path: String,
    private val method: HttpMethod,
    private val encodeBody: (Any) -> String,
    private val onSuccess: suspend () -> Unit,
) {
    /**
     * @param body request body (ignored for DELETE)
     * @param pathOverride temporary path (supports calling same use case with different ids)
     */
    suspend fun execute(body: Any? = null, pathOverride: String? = null): KmpSdkResult<Unit> {
        val targetPath = pathOverride ?: path
        val encoded = body?.let(encodeBody)
        val result = when (method) {
            HttpMethod.Post -> networkClient.post<Unit>(targetPath, offlineBody = encoded) {
                encoded?.let { setJsonBodyWithOfflineCapture(it) }
            }
            HttpMethod.Put -> networkClient.put<Unit>(targetPath, offlineBody = encoded) {
                encoded?.let { setJsonBodyWithOfflineCapture(it) }
            }
            HttpMethod.Patch -> networkClient.patch<Unit>(targetPath, offlineBody = encoded) {
                encoded?.let { setJsonBodyWithOfflineCapture(it) }
            }
            HttpMethod.Delete -> networkClient.delete<Unit>(targetPath)
            else -> error("Unsupported mutation method: $method")
        }
        return when (result) {
            is KmpSdkResult.Success -> {
                onSuccess()
                KmpSdkResult.Success(Unit)
            }
            is KmpSdkResult.Failure -> result
        }
    }

    companion object {
        inline fun <reified TBody> create(
            networkClient: KmpNetworkClient,
            path: String,
            method: HttpMethod,
            json: Json = networkClient.json,
            noinline onSuccess: suspend () -> Unit = {},
        ): RestMutationUseCase = create(
            networkClient = networkClient,
            path = path,
            method = method,
            encodeBody = { body -> json.encodeToString(serializer<TBody>(), body as TBody) },
            onSuccess = onSuccess,
        )

        fun create(
            networkClient: KmpNetworkClient,
            path: String,
            method: HttpMethod,
            encodeBody: (Any) -> String,
            onSuccess: suspend () -> Unit = {},
        ): RestMutationUseCase = RestMutationUseCase(
            networkClient = networkClient,
            path = path,
            method = method,
            encodeBody = encodeBody,
            onSuccess = onSuccess,
        )

        /** DELETE has no body — encodeBody is unused. */
        fun createDelete(
            networkClient: KmpNetworkClient,
            path: String,
            onSuccess: suspend () -> Unit = {},
        ): RestMutationUseCase = RestMutationUseCase(
            networkClient = networkClient,
            path = path,
            method = HttpMethod.Delete,
            encodeBody = { error("DELETE has no body") },
            onSuccess = onSuccess,
        )
    }
}
