package com.kmpsdk.integrations.koin

import com.kmpsdk.KmpSdk
import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.config.RemoteConfigStore
import com.kmpsdk.core.di.KmpSdkDiBridge
import com.kmpsdk.core.routing.DeepLinkRouter
import com.kmpsdk.core.routing.PushPayloadRouter
import com.kmpsdk.data.draft.DraftStore
import com.kmpsdk.data.network.FileUploadHelper
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.data.query.QueryKit
import com.kmpsdk.data.realtime.RealtimeClient
import com.kmpsdk.data.sync.BackgroundWorkBridge
import com.kmpsdk.data.sync.SyncCoordinator
import com.kmpsdk.data.sync.SyncStatusStore
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.reflect.KClass

/**
 * Koin module exposing core KmpSdk singletons.
 *
 * Usage:
 * ```
 * startKoin { modules(kmpSdkKoinModule()) }
 * KmpSdk.init(this) { ... }
 * ```
 */
fun kmpSdkKoinModule(): Module = module {
    single { KmpSdk.networkClient }
    single { KmpSdk.sessionManager }
    single { KmpSdk.syncCoordinator }
    single { KmpSdk.syncStatus }
    single { KmpSdk.fileUpload }
    single { KmpSdk.drafts }
    single { KmpSdk.query }
    single { KmpSdk.realtime }
    single { KmpSdk.deepLinks }
    single { KmpSdk.push }
    single { KmpSdk.backgroundWork }
    single { KmpSdk.remoteConfig }
    single<KmpNetworkClient> { get() }
    single<SessionManager> { get() }
    single<SyncCoordinator> { get() }
    single<SyncStatusStore> { get() }
    single<FileUploadHelper> { get() }
    single<DraftStore> { get() }
    single<QueryKit> { get() }
    single<RealtimeClient> { get() }
    single<DeepLinkRouter> { get() }
    single<PushPayloadRouter> { get() }
    single<BackgroundWorkBridge> { get() }
    single<RemoteConfigStore> { get() }
}

/**
 * Resolve a host-registered feature type from the SDK registry via Koin factory.
 *
 * ```
 * factory { kmpSdkResolve<RestResourceApi<Product>>() }
 * ```
 */
inline fun <reified T : Any> kmpSdkResolve(): T = KmpSdkDiBridge.get()

fun <T : Any> kmpSdkResolve(type: KClass<T>): T = KmpSdkDiBridge.get(type)
