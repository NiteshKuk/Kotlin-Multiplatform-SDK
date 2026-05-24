package com.kmpsdk.domain.usecase

import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.data.network.setJsonBodyWithOfflineCapture
import com.kmpsdk.domain.error.KmpSdkResult
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class RestMutationUseCase private constructor(
    private val networkClient: KmpNetworkClient,
    private val path: String,
    private val method: HttpMethod,
    private val encodeBody: (Any) -> String,
    private val onSuccess: suspend () -> Unit,
) {
    suspend fun execute(body: Any? = null): KmpSdkResult<Unit> {
        val encoded = body?.let(encodeBody)
        val result = when (method) {
            HttpMethod.Post -> networkClient.post<Unit>(path, offlineBody = encoded) {
                encoded?.let { setJsonBodyWithOfflineCapture(it) }
            }
            HttpMethod.Put -> networkClient.put<Unit>(path, offlineBody = encoded) {
                encoded?.let { setJsonBodyWithOfflineCapture(it) }
            }
            HttpMethod.Patch -> networkClient.patch<Unit>(path, offlineBody = encoded) {
                encoded?.let { setJsonBodyWithOfflineCapture(it) }
            }
            HttpMethod.Delete -> networkClient.delete(path)
            else -> error("Unsupported mutation method")
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
    }
}
