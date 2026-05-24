package com.kmpsdk.data.network

import io.ktor.http.HttpMethod

internal fun buildHttpCacheKey(method: HttpMethod, path: String): String =
    "${method.value}:$path"
