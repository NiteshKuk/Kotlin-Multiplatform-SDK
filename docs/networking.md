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

## Dedup / rate-limit / pinning

```kotlin
enableRequestDeduplication = true
enableRateLimitBackoff = true
maxRateLimitRetries = 3
certificatePins = listOf("api.example.com/abcdef1234567890=") // Android
```
