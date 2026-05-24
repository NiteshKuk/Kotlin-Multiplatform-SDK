package com.kmpsdk.data.network

data class FileUploadPart(
    val fieldName: String,
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream",
)

data class MultipartUploadRequest(
    val path: String,
    val parts: List<FileUploadPart>,
    val fields: Map<String, String> = emptyMap(),
)
