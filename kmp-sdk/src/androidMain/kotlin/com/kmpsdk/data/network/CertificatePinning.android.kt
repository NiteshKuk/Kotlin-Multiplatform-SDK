package com.kmpsdk.data.network

import com.kmpsdk.core.config.KmpSdkConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

actual fun configureCertificatePinning(config: KmpSdkConfig, platformClientBuilder: Any) {
    if (config.certificateBuilder.certificatePins.isEmpty()) return
    val builder = platformClientBuilder as? OkHttpClient.Builder ?: return
    val pinnerBuilder = CertificatePinner.Builder()
    config.certificateBuilder.certificatePins.forEach { pin ->
        pinnerBuilder.add(config.certificateBuilder.hostname, "sha256/${pin}")
    }
    builder.certificatePinner(pinnerBuilder.build())
}
