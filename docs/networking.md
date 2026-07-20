# Networking extras

## HTTP cache (Path B)

```kotlin
enableHttpCache = true
networkClient.get<List<UserDto>>("/users")
networkClient.get<UserDto>("/users/1", useCache = false)
```

## Resilience (retry + circuit breaker)

```kotlin
resilience {
    retry { maxAttempts = 3; initialBackoffMillis = 200; maxBackoffMillis = 5_000 }
    circuitBreaker("payments") { failureThreshold = 5; openDurationMillis = 30_000 }
    protectPath("/payments", "payments")
}
```

## File upload

```kotlin
KmpSdk.fileUpload.upload<UploadResponse>(
    path = "/upload",
    fileName = "photo.jpg",
    bytes = imageBytes,
    contentType = "image/jpeg",
    onProgress = { /* Started / Completed / Failed */ },
)
```

## Realtime (WebSocket / SSE)

```kotlin
KmpSdk.realtime.connectWebSocket("orders", "wss://api.example.com/ws") {
    KmpSdk.syncCoordinator.refreshTarget("orders")
}
KmpSdk.realtime.connectSse("feeds", "/events/stream") { /* … */ }
KmpSdk.realtime.disconnect("orders")
```

## Dedup / rate-limit

```kotlin
enableRequestDeduplication = true
enableRateLimitBackoff = true
maxRateLimitRetries = 3
```

## SSL / certificate pinning

Pin **public-key SHA-256** hashes so the app only trusts known certs for your API host.

Use [`CertificateParams`](../kmp-sdk/src/commonMain/kotlin/com/kmpsdk/core/config/KmpSdkConfig.kt): one **hostname** + a list of **Base64 pin hashes** (no `host/` prefix on each pin).

### Init

```kotlin
import com.kmpsdk.core.config.CertificateParams

KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    certificateBuilder = CertificateParams(
        hostname = "api.example.com",
        certificatePins = listOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=", // backup
        ),
    )
}
```

The SDK prefixes `sha256/` for OkHttp. Do **not** include the `sha256/` prefix in the list entries.

Prefer **two or more pins** (current leaf + next/backup or intermediate) so a normal cert rotation does not lock users out.

### How to get a pin (OpenSSL)

```bash
openssl s_client -connect api.example.com:443 -servername api.example.com </dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform DER \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Put the printed Base64 into `certificatePins` and set `hostname` to the API host (e.g. `api.example.com`).

### Platform support

| Platform | Behavior |
|----------|----------|
| **Android** | Applied via OkHttp `CertificatePinner` (`hostname` + each pin) |
| **iOS** | Not applied in the SDK today — configure Darwin / `NSURLSession` in the host if you need pins |

### Prod environments

Set `certificateBuilder` on the **prod** env pack (see [auth-and-config.md](auth-and-config.md#environment-vault)). Switching to a prod-named environment with `validateOnStartup = true` expects pins to be present.

### When pinning is *not* the problem

`SSLPeerUnverifiedException: Hostname X not verified` with a **different** CN / SAN (e.g. captive portal, corporate proxy, `securelogin.*`) means the device never reached your API’s real certificate. Fix network / DNS / VPN first; pins only matter after the correct host cert is presented.
