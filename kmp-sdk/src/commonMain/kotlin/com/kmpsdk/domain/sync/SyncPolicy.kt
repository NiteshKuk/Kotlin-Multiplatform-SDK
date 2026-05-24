package com.kmpsdk.domain.sync

/**
 * Controls how repositories combine local SQL data with remote API calls.
 */
enum class SyncPolicy {
    /** Read local DB first; refresh from network when online. */
    CACHE_FIRST,

    /** Require a successful network call; fail when offline. */
    NETWORK_FIRST,

    /** Show cached data immediately, refresh in background when online. */
    STALE_WHILE_REVALIDATE,
}
