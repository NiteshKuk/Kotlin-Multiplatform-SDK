package com.kmpsdk.core.config

import com.kmpsdk.core.logger.LogLevel
import com.kmpsdk.data.offline.OfflineReplayStrategy
import com.kmpsdk.domain.sync.SyncPolicy

enum class SdkProfile {
    DEVELOPMENT,
    STAGING,
    PRODUCTION,
    ENTERPRISE,
}

fun KmpSdkConfigBuilder.applyProfile(profile: SdkProfile) {
    when (profile) {
        SdkProfile.DEVELOPMENT -> {
            logLevel = LogLevel.DEBUG
            enableRequestLogging = true
            enableCurlLogging = true
            enableResponseBodyLogging = true
            auth = AuthConfig(enabled = false, useSecureTokenStore = false)
        }
        SdkProfile.STAGING -> {
            logLevel = LogLevel.INFO
            enableRequestLogging = true
            enableCurlLogging = false
            enableResponseBodyLogging = false
            syncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE
        }
        SdkProfile.PRODUCTION -> {
            logLevel = LogLevel.WARN
            enableRequestLogging = false
            enableCurlLogging = false
            enableResponseBodyLogging = false
            auth = AuthConfig(enabled = true, useSecureTokenStore = true)
        }
        SdkProfile.ENTERPRISE -> {
            logLevel = LogLevel.INFO
            enableRequestLogging = true
            enableResponseBodyLogging = false
            enableCurlLogging = false
            auth = AuthConfig(enabled = true, useSecureTokenStore = true)
            syncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE
            offlineReplayStrategy = OfflineReplayStrategy.PRIORITY
            enableRequestDeduplication = true
            enableRateLimitBackoff = true
            autoSyncOnReconnect = true
            queueMutationsWhenOffline = true
        }
    }
}
