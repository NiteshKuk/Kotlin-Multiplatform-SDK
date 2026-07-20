# Auth, remote config, environments, tenant

## Authentication

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    auth { enabled = true; useSecureTokenStore = true }
    tokenRefreshHandler = TokenRefreshHandler { refreshToken ->
        KmpSdkResult.Success(TokenPair(newAccessToken, refreshToken))
    }
}

KmpSdk.sessionManager.login("access", "refresh")
KmpSdk.sessionManager.events.collect { /* SessionExpired → login */ }
```

## Remote config (+ Firebase in the **host** app)

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    remoteConfig {
        val rc = Firebase.remoteConfig
        rc.fetchAndActivate()
        mapOf(
            "feature_x_enabled" to rc.getBoolean("feature_x_enabled").toString(),
            "default_cache_ttl_millis" to rc.getLong("default_cache_ttl_millis").toString(),
        )
    }
}

val enabled = KmpSdk.remoteConfig.getBoolean("feature_x_enabled", default = false)
```

Firebase stays in the app; the SDK only stores the map.

## Environment vault

```kotlin
KmpSdk.init(this) {
    environments {
        dev { baseUrl = "https://dev.api.example.com" }
        staging { baseUrl = "https://staging.api.example.com" }
        prod {
            baseUrl = "https://api.example.com"
            // Android SSL pins — see networking.md#ssl--certificate-pinning
            certificateBuilder = CertificateParams(
                hostname = "api.example.com",
                certificatePins = listOf(
                    "<leafPinBase64>",
                    "<backupPinBase64>",
                ),
            )
        }
    }
    environmentName = "staging"
}
KmpSdk.environments?.switchTo("prod")
```

Leave pins empty on `dev` / `staging` unless those hosts use the same public keys.

## Multi-tenant (still available)

```kotlin
KmpSdk.tenantManager.switchTenant(
    tenantId = "acme",
    baseUrl = "https://acme.api.example.com",
    headers = mapOf("X-Tenant" to "acme"),
)
```

## Profiles & validation

```kotlin
profile = SdkProfile.ENTERPRISE // or DEVELOPMENT / STAGING / PRODUCTION
validateOnStartup = true
val issues = KmpSdk.validate().issues
```
