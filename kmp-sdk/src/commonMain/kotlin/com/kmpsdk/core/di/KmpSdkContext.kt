package com.kmpsdk.core.di

import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.auth.TokenStore
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.config.RemoteConfigStore
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.core.messaging.MessageEventBus
import com.kmpsdk.core.messaging.MessageNotifier
import com.kmpsdk.core.routing.DeepLinkRouter
import com.kmpsdk.core.routing.PushPayloadRouter
import com.kmpsdk.core.tenant.TenantManager
import com.kmpsdk.data.cache.CacheStore
import com.kmpsdk.data.db.DatabaseDriverFactory
import com.kmpsdk.data.db.KmpSdkDatabase
import com.kmpsdk.data.draft.DraftStore
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.data.offline.OfflineActionManager
import com.kmpsdk.data.offline.OfflineAwareRequestExecutor
import com.kmpsdk.data.offline.OfflineQueueManager
import com.kmpsdk.data.query.QueryKit
import com.kmpsdk.data.realtime.RealtimeClient
import com.kmpsdk.data.sync.BackgroundWorkBridge
import com.kmpsdk.data.sync.DirtySyncCoordinator
import com.kmpsdk.data.sync.SyncCoordinator
import com.kmpsdk.debug.KmpSdkDebugger
import kotlinx.coroutines.CoroutineScope

class KmpSdkContext internal constructor(
    val config: KmpSdkConfig,
    val logger: Logger,
    val tokenStore: TokenStore,
    val sessionManager: SessionManager,
    val networkClient: KmpNetworkClient,
    val cacheStore: CacheStore,
    val offlineQueue: OfflineQueueManager,
    val offlineExecutor: OfflineAwareRequestExecutor,
    val offlineActions: OfflineActionManager,
    val syncCoordinator: SyncCoordinator,
    val dirtySyncCoordinator: DirtySyncCoordinator,
    val debugger: KmpSdkDebugger,
    val connectivityMonitor: ConnectivityMonitor,
    val messageEventBus: MessageEventBus,
    val messageNotifier: MessageNotifier,
    val database: KmpSdkDatabase,
    val databaseDriverFactory: DatabaseDriverFactory,
    val tenantManager: TenantManager,
    val remoteConfig: RemoteConfigStore,
    val scope: CoroutineScope,
    val drafts: DraftStore,
    val query: QueryKit,
    val realtime: RealtimeClient,
    val deepLinks: DeepLinkRouter,
    val push: PushPayloadRouter,
    val backgroundWork: BackgroundWorkBridge,
)
