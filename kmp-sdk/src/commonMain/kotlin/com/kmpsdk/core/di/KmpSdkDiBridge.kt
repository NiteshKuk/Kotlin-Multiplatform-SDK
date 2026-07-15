package com.kmpsdk.core.di

import com.kmpsdk.KmpSdk
import kotlin.reflect.KClass

/**
 * Framework-agnostic export of SDK resolve APIs for Hilt/Kodein/Koin bridges.
 */
object KmpSdkDiBridge {
    fun <T : Any> get(type: KClass<T>): T = KmpSdk.requireInitialized().registry.resolve(type)

    inline fun <reified T : Any> get(): T = get(T::class)

    fun registeredTypes(): Set<KClass<*>> = KmpSdk.requireInitialized().registry.registeredTypes()
}
