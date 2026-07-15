package com.kmpsdk.integrations.kodein

import com.kmpsdk.KmpSdk
import com.kmpsdk.core.di.KmpSdkDiBridge
import kotlin.reflect.KClass

/**
 * Kodein is not a hard dependency of the SDK.
 * In the host app:
 *
 * ```
 * val di = DI {
 *   bindSingleton { KmpSdk.networkClient }
 *   bindSingleton { kmpSdkKodeinGet<RestResourceApi<Product>>() }
 * }
 * ```
 */
inline fun <reified T : Any> kmpSdkKodeinGet(): T = KmpSdkDiBridge.get()

fun <T : Any> kmpSdkKodeinGet(type: KClass<T>): T = KmpSdkDiBridge.get(type)

fun kmpSdkKodeinNetworkClient() = KmpSdk.networkClient

fun kmpSdkKodeinSessionManager() = KmpSdk.sessionManager
