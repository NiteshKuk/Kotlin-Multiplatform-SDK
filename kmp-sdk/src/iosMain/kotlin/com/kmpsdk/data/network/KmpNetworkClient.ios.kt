package com.kmpsdk.data.network

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(config: KmpSdkConfig, logger: Logger): HttpClient =
    HttpClient(Darwin) {
        expectSuccess = false
    }
