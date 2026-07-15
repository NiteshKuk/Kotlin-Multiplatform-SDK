package com.kmpsdk.integrations.hilt

import com.kmpsdk.KmpSdk
import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.di.KmpSdkDiBridge
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.data.sync.SyncCoordinator

/**
 * Hilt does not live inside the SDK (annotation processing stays in the host app).
 * Use these providers inside your app's `@Module`:
 *
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object AppKmpSdkModule {
 *   @Provides @Singleton fun network(): KmpNetworkClient = hiltKmpNetworkClient()
 *   @Provides @Singleton fun products(): RestResourceApi<Product> = hiltKmpGet()
 * }
 * ```
 */
fun hiltKmpNetworkClient(): KmpNetworkClient = KmpSdk.networkClient

fun hiltKmpSessionManager(): SessionManager = KmpSdk.sessionManager

fun hiltKmpSyncCoordinator(): SyncCoordinator = KmpSdk.syncCoordinator

inline fun <reified T : Any> hiltKmpGet(): T = KmpSdkDiBridge.get()
