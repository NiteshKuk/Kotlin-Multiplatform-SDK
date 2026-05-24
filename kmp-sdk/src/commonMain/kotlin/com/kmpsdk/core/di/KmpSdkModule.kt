package com.kmpsdk.core.di

/**
 * Groups related registrations (User feature, Product feature, etc.).
 * Host apps implement this and pass instances to [KmpSdkRegistry.install].
 */
fun interface KmpSdkModule {
    fun register(registry: KmpSdkRegistry)
}
