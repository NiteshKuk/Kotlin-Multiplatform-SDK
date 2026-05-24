package com.kmpsdk.core.validation

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.core.telemetry.KmpSdkTelemetry
import com.kmpsdk.core.telemetry.TelemetryEvent

data class ValidationIssue(
    val level: Level,
    val message: String,
) {
    enum class Level { ERROR, WARNING }
}

data class ValidationResult(
    val issues: List<ValidationIssue>,
) {
    val isValid: Boolean get() = issues.none { it.level == ValidationIssue.Level.ERROR }
}

object KmpSdkValidator {
    fun validate(
        config: KmpSdkConfig,
        registry: KmpSdkRegistry? = null,
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()

        if (config.baseUrl.isBlank()) {
            issues += ValidationIssue(ValidationIssue.Level.ERROR, "baseUrl is blank")
        } else if (!config.baseUrl.startsWith("http")) {
            issues += ValidationIssue(ValidationIssue.Level.ERROR, "baseUrl must start with http/https")
        }

        if (config.auth.enabled && config.auth.useSecureTokenStore) {
            issues += ValidationIssue(
                ValidationIssue.Level.WARNING,
                "Secure token store enabled — ensure KmpSdkAndroid.init(context) was called on Android",
            )
        }

        if (config.enableResponseBodyLogging) {
            issues += ValidationIssue(
                ValidationIssue.Level.WARNING,
                "Response body logging enabled — disable in production for PII safety",
            )
        }

        registry?.let {
            if (it.registeredCount() == 0) {
                issues += ValidationIssue(
                    ValidationIssue.Level.WARNING,
                    "No feature modules registered — call install() in KmpSdk.init",
                )
            }
        }

        issues.filter { it.level == ValidationIssue.Level.WARNING }.forEach {
            KmpSdkTelemetry.emit(TelemetryEvent.ValidationWarning(it.message))
        }

        return ValidationResult(issues)
    }
}
