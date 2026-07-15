package com.kmpsdk.core.resilience

import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

data class RetryPolicy(
    val maxAttempts: Int = 1,
    val initialBackoffMillis: Long = 200,
    val maxBackoffMillis: Long = 5_000,
    val retryOnNetwork: Boolean = true,
    val retryOnServerErrors: Boolean = true,
) {
    fun nextDelayMillis(attemptIndex: Int): Long {
        val exp = initialBackoffMillis * (1L shl attemptIndex.coerceAtMost(8))
        return exp.coerceAtMost(maxBackoffMillis)
    }
}

data class CircuitBreakerPolicy(
    val name: String,
    val failureThreshold: Int = 5,
    val openDurationMillis: Long = 30_000,
    val halfOpenSuccessesToClose: Int = 1,
)

enum class CircuitState {
    Closed,
    Open,
    HalfOpen,
}

class CircuitBreaker(
    private val policy: CircuitBreakerPolicy,
) {
    private var state: CircuitState = CircuitState.Closed
    private var failures: Int = 0
    private var openedAtMillis: Long = 0
    private var halfOpenSuccesses: Int = 0

    fun currentState(): CircuitState {
        maybeTransitionFromOpen()
        return state
    }

    fun beforeCall(): Boolean {
        maybeTransitionFromOpen()
        return when (state) {
            CircuitState.Closed, CircuitState.HalfOpen -> true
            CircuitState.Open -> false
        }
    }

    fun onSuccess() {
        when (state) {
            CircuitState.HalfOpen -> {
                halfOpenSuccesses++
                if (halfOpenSuccesses >= policy.halfOpenSuccessesToClose) {
                    state = CircuitState.Closed
                    failures = 0
                    halfOpenSuccesses = 0
                }
            }
            CircuitState.Closed -> failures = 0
            CircuitState.Open -> Unit
        }
    }

    fun onFailure() {
        when (state) {
            CircuitState.HalfOpen -> open()
            CircuitState.Closed -> {
                failures++
                if (failures >= policy.failureThreshold) open()
            }
            CircuitState.Open -> Unit
        }
    }

    private fun open() {
        state = CircuitState.Open
        openedAtMillis = Clock.System.now().toEpochMilliseconds()
        halfOpenSuccesses = 0
    }

    private fun maybeTransitionFromOpen() {
        if (state != CircuitState.Open) return
        val elapsed = Clock.System.now().toEpochMilliseconds() - openedAtMillis
        if (elapsed >= policy.openDurationMillis) {
            state = CircuitState.HalfOpen
            halfOpenSuccesses = 0
        }
    }
}

data class ResilienceConfig(
    val retry: RetryPolicy = RetryPolicy(),
    val circuitBreakers: Map<String, CircuitBreakerPolicy> = emptyMap(),
    /** Maps path prefix → circuit breaker name. */
    val pathCircuitBreaker: Map<String, String> = emptyMap(),
)

class ResilienceController(
    private val config: ResilienceConfig,
) {
    private val breakers: Map<String, CircuitBreaker> =
        config.circuitBreakers.mapValues { (_, policy) -> CircuitBreaker(policy) }

    fun breakerForPath(path: String): CircuitBreaker? {
        val name = config.pathCircuitBreaker.entries
            .firstOrNull { path.startsWith(it.key) }
            ?.value
            ?: return null
        return breakers[name]
    }

    suspend fun <T> execute(
        path: String,
        isRetryable: (Throwable) -> Boolean,
        block: suspend () -> T,
    ): T {
        val breaker = breakerForPath(path)
        if (breaker != null && !breaker.beforeCall()) {
            throw CircuitOpenException(breakerName = path)
        }

        var attempt = 0
        var lastError: Throwable? = null
        val maxAttempts = config.retry.maxAttempts.coerceAtLeast(1)

        while (attempt < maxAttempts) {
            try {
                val value = block()
                breaker?.onSuccess()
                return value
            } catch (ex: Throwable) {
                lastError = ex
                breaker?.onFailure()
                attempt++
                val canRetry = attempt < maxAttempts && isRetryable(ex)
                if (!canRetry) throw ex
                delay(config.retry.nextDelayMillis(attempt - 1))
            }
        }
        throw lastError ?: IllegalStateException("Retry attempts exhausted")
    }

    fun <T> mapCircuitOpen(error: CircuitOpenException): KmpSdkResult<T> =
        KmpSdkResult.Failure(
            KmpSdkError.Network("Circuit open for ${error.breakerName}"),
        )
}

class CircuitOpenException(val breakerName: String) : RuntimeException("Circuit open: $breakerName")

class ResilienceDsl {
    var retry: RetryPolicy = RetryPolicy()
    private val breakers = linkedMapOf<String, CircuitBreakerPolicy>()
    private val pathMap = linkedMapOf<String, String>()

    fun retry(block: RetryPolicyBuilder.() -> Unit) {
        retry = RetryPolicyBuilder().apply(block).build()
    }

    fun circuitBreaker(name: String, block: CircuitBreakerPolicyBuilder.() -> Unit = {}) {
        breakers[name] = CircuitBreakerPolicyBuilder(name).apply(block).build()
    }

    fun protectPath(pathPrefix: String, breakerName: String) {
        pathMap[pathPrefix] = breakerName
    }

    internal fun build(): ResilienceConfig = ResilienceConfig(
        retry = retry,
        circuitBreakers = breakers.toMap(),
        pathCircuitBreaker = pathMap.toMap(),
    )
}

class RetryPolicyBuilder {
    var maxAttempts: Int = 3
    var initialBackoffMillis: Long = 200
    var maxBackoffMillis: Long = 5_000
    var retryOnNetwork: Boolean = true
    var retryOnServerErrors: Boolean = true

    fun build() = RetryPolicy(
        maxAttempts = maxAttempts,
        initialBackoffMillis = initialBackoffMillis,
        maxBackoffMillis = maxBackoffMillis,
        retryOnNetwork = retryOnNetwork,
        retryOnServerErrors = retryOnServerErrors,
    )
}

class CircuitBreakerPolicyBuilder(private val name: String) {
    var failureThreshold: Int = 5
    var openDurationMillis: Long = 30_000
    var halfOpenSuccessesToClose: Int = 1

    fun build() = CircuitBreakerPolicy(
        name = name,
        failureThreshold = failureThreshold,
        openDurationMillis = openDurationMillis,
        halfOpenSuccessesToClose = halfOpenSuccessesToClose,
    )
}
