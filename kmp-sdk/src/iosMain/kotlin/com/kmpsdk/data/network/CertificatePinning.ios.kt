package com.kmpsdk.data.network

import com.kmpsdk.core.config.KmpSdkConfig

actual fun configureCertificatePinning(config: KmpSdkConfig, platformClientBuilder: Any) {
    // Darwin pinning requires NSURLSession delegate — host apps configure via platform hook if needed.
}
