package com.kmpsdk.data.network

import com.kmpsdk.core.config.KmpSdkConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

actual fun configureCertificatePinning(config: KmpSdkConfig, platformClientBuilder: Any) {
    if (config.certificatePins.isEmpty()) return
    val builder = platformClientBuilder as? OkHttpClient.Builder ?: return
    val pinnerBuilder = CertificatePinner.Builder()
    config.certificatePins.forEach { pin ->
        val parts = pin.split("/", limit = 2)
        if (parts.size == 2) {
            pinnerBuilder.add(parts[0], "sha256/${parts[1]}")
        }
    }
    builder.certificatePinner(pinnerBuilder.build())
}
