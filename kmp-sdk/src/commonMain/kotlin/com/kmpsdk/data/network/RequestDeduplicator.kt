package com.kmpsdk.data.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class RequestDeduplicator {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, CompletableDeferred<Result<String?>>>()

    suspend fun execute(key: String, block: suspend () -> String?): String? {
        val waiter = mutex.withLock {
            inFlight[key]?.let { return@withLock it }
            CompletableDeferred<Result<String?>>().also { inFlight[key] = it }
        }

        if (inFlight[key] !== waiter) {
            return waiter.await().getOrThrow()
        }

        return try {
            val value = block()
            waiter.complete(Result.success(value))
            value
        } catch (t: Throwable) {
            waiter.complete(Result.failure(t))
            throw t
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }
}
