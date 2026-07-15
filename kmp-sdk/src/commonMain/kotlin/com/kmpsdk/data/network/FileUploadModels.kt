package com.kmpsdk.data.network

data class FileUploadPart(
    val fieldName: String,
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as FileUploadPart
        return fieldName == other.fieldName &&
            fileName == other.fileName &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fieldName.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

data class MultipartUploadRequest(
    val path: String,
    val parts: List<FileUploadPart>,
    val fields: Map<String, String> = emptyMap(),
)

enum class UploadStage {
    Started,
    Completed,
    Failed,
}

/**
 * Coarse upload lifecycle (byte totals). Streaming percent progress depends on
 * the platform HTTP engine and is not guaranteed for multipart bodies.
 */
data class UploadProgress(
    val stage: UploadStage,
    val bytesTotal: Long,
    val fileName: String? = null,
    val errorMessage: String? = null,
)
