package com.kmpsdk.core.di

import kotlin.reflect.KClass

/**
 * Lightweight service locator for host-app dependencies.
 *
 * Example:
 * ```
 * KmpSdk.init(config) {
 *     register<UserRepository> { ctx -> UserRepositoryImpl(..., ctx) }
 *     register<GetUsersUseCase> { GetUsersUseCase(resolve()) }
 * }
 * val repo = KmpSdk.get<UserRepository>()
 * ```
 */
class KmpSdkRegistry internal constructor(
    internal val context: KmpSdkContext,
) {
    private val factories = mutableMapOf<KClass<*>, (KmpSdkContext) -> Any>()
    private val singletons = mutableMapOf<KClass<*>, Any>()

    fun <T : Any> register(type: KClass<T>, factory: (KmpSdkContext) -> T) {
        factories[type] = factory
        singletons.remove(type)
    }

    inline fun <reified T : Any> register(noinline factory: (KmpSdkContext) -> T) {
        register(T::class, factory)
    }

    inline fun <reified T : Any> resolve(): T = resolve(T::class)

    fun <T : Any> resolve(type: KClass<T>): T {
        singletons[type]?.let { @Suppress("UNCHECKED_CAST") return it as T }
        val factory = factories[type]
            ?: error("${type.simpleName} is not registered. Call register() in KmpSdk.init { ... }")
        @Suppress("UNCHECKED_CAST")
        return factory(context).also { singletons[type] = it } as T
    }

    inline fun <reified T : Any> resolveOrNull(): T? =
        runCatching { resolve<T>() }.getOrNull()

    fun install(module: KmpSdkModule) {
        module.register(this)
    }

    fun install(vararg modules: KmpSdkModule) {
        modules.forEach { install(it) }
    }

    internal fun registeredCount(): Int = factories.size

    fun registeredTypes(): Set<KClass<*>> = factories.keys.toSet()
}
