package com.kmpsdk.data.network

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

actual fun createPlatformHttpClient(config: KmpSdkConfig, logger: Logger): HttpClient {
    val okHttpBuilder = OkHttpClient.Builder()
    configureCertificatePinning(config, okHttpBuilder)
    return HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            preconfigured = okHttpBuilder.build()
        }
    }
}
