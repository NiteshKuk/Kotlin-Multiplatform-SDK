package com.kmpsdk.data.network

import com.kmpsdk.domain.error.KmpSdkResult

/**
 * Convenience wrapper over [KmpNetworkClient.uploadMultipart].
 *
 * Prefer [com.kmpsdk.KmpSdk.fileUpload] after init.
 */
class FileUploadHelper(
    @PublishedApi internal val networkClient: KmpNetworkClient,
) {
    /**
     * Upload a single file as multipart form-data.
     *
     * @param path relative API path (e.g. `/upload`)
     * @param fieldName form field name expected by the server
     * @param fileName file name sent in Content-Disposition
     * @param bytes file bytes
     * @param contentType MIME type
     * @param fields extra text fields alongside the file
     * @param onProgress optional Started / Completed / Failed callback
     */
    suspend inline fun <reified T> upload(
        path: String,
        fileName: String,
        bytes: ByteArray,
        fieldName: String = "file",
        contentType: String = "application/octet-stream",
        fields: Map<String, String> = emptyMap(),
        noinline onProgress: ((UploadProgress) -> Unit)? = null,
    ): KmpSdkResult<T> = uploadMultipart(
        MultipartUploadRequest(
            path = path,
            parts = listOf(
                FileUploadPart(
                    fieldName = fieldName,
                    fileName = fileName,
                    bytes = bytes,
                    contentType = contentType,
                ),
            ),
            fields = fields,
        ),
        onProgress = onProgress,
    )

    /**
     * Upload multiple file parts (+ optional text fields) in one multipart request.
     */
    suspend inline fun <reified T> uploadMultipart(
        request: MultipartUploadRequest,
        noinline onProgress: ((UploadProgress) -> Unit)? = null,
    ): KmpSdkResult<T> {
        val totalBytes = request.parts.sumOf { it.bytes.size.toLong() }
        val primaryName = request.parts.firstOrNull()?.fileName
        onProgress?.invoke(
            UploadProgress(
                stage = UploadStage.Started,
                bytesTotal = totalBytes,
                fileName = primaryName,
            ),
        )
        val result = networkClient.uploadMultipart<T>(request)
        when (result) {
            is KmpSdkResult.Success -> onProgress?.invoke(
                UploadProgress(
                    stage = UploadStage.Completed,
                    bytesTotal = totalBytes,
                    fileName = primaryName,
                ),
            )
            is KmpSdkResult.Failure -> onProgress?.invoke(
                UploadProgress(
                    stage = UploadStage.Failed,
                    bytesTotal = totalBytes,
                    fileName = primaryName,
                    errorMessage = result.error.message,
                ),
            )
        }
        return result
    }
}
