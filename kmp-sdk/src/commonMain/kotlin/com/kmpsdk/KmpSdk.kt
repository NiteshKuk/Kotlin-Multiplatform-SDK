package com.kmpsdk

import com.kmpsdk.core.auth.InMemoryTokenStore
import com.kmpsdk.core.auth.PlatformTokenStore
import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.auth.TokenRefreshHandler
import com.kmpsdk.core.auth.TokenStore
import com.kmpsdk.core.config.EnvironmentVault
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.config.RemoteConfigStore
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.connectivity.createConnectivityMonitor
import com.kmpsdk.core.di.KmpSdkContext
import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.core.messaging.MessageEventBus
import com.kmpsdk.core.messaging.MessageNotifier
import com.kmpsdk.core.messaging.MessageNotifierAdapter
import com.kmpsdk.core.messaging.SharedMessageEventBus
import com.kmpsdk.core.routing.DeepLinkRouter
import com.kmpsdk.core.routing.PushPayloadRouter
import com.kmpsdk.core.telemetry.KmpSdkTelemetry
import com.kmpsdk.core.telemetry.TelemetryEvent
import com.kmpsdk.core.tenant.TenantManager
import com.kmpsdk.core.validation.KmpSdkValidator
import com.kmpsdk.data.cache.CacheStore
import com.kmpsdk.data.cache.TieredCacheStore
import com.kmpsdk.data.db.KmpSdkDatabase
import com.kmpsdk.data.db.createDatabaseDriverFactory
import com.kmpsdk.data.draft.DraftStore
import com.kmpsdk.data.network.FileUploadHelper
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.data.offline.OfflineActionManager
import com.kmpsdk.data.offline.OfflineAwareRequestExecutor
import com.kmpsdk.data.offline.OfflineQueueManager
import com.kmpsdk.data.query.QueryKit
import com.kmpsdk.data.realtime.RealtimeClient
import com.kmpsdk.data.sync.BackgroundSyncScheduler
import com.kmpsdk.data.sync.BackgroundWorkBridge
import com.kmpsdk.data.sync.ConnectivitySyncObserver
import com.kmpsdk.data.sync.DirtySyncCoordinator
import com.kmpsdk.data.sync.SyncCoordinator
import com.kmpsdk.data.sync.SyncStatusStore
import com.kmpsdk.debug.KmpSdkDebugger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

object KmpSdk {
    private var initialized = false

    lateinit var config: KmpSdkConfig
        private set
    lateinit var logger: Logger
        private set
    lateinit var tokenStore: TokenStore
        private set
    lateinit var sessionManager: SessionManager
        private set
    lateinit var networkClient: KmpNetworkClient
        private set
    lateinit var cacheStore: CacheStore
        private set
    lateinit var offlineQueue: OfflineQueueManager
        private set
    lateinit var offlineExecutor: OfflineAwareRequestExecutor
        private set
    lateinit var offlineActions: OfflineActionManager
        private set
    lateinit var syncCoordinator: SyncCoordinator
        private set
    val syncStatus: SyncStatusStore
        get() = requireInitialized().syncCoordinator.statusStore
    lateinit var fileUpload: FileUploadHelper
        private set
    lateinit var dirtySyncCoordinator: DirtySyncCoordinator
        private set
    lateinit var debugger: KmpSdkDebugger
        private set
    lateinit var connectivityMonitor: ConnectivityMonitor
        private set
    lateinit var messageEventBus: MessageEventBus
        private set
    lateinit var messageNotifier: MessageNotifier
        private set
    lateinit var database: KmpSdkDatabase
        private set
    lateinit var context: KmpSdkContext
        private set
    lateinit var registry: KmpSdkRegistry
        private set
    lateinit var tenantManager: TenantManager
        private set
    lateinit var remoteConfig: RemoteConfigStore
        private set
    lateinit var drafts: DraftStore
        private set
    lateinit var query: QueryKit
        private set
    lateinit var realtime: RealtimeClient
        private set
    lateinit var deepLinks: DeepLinkRouter
        private set
    lateinit var push: PushPayloadRouter
        private set
    lateinit var backgroundWork: BackgroundWorkBridge
        private set
    var environments: EnvironmentVault? = null
        private set
    val telemetry: KmpSdkTelemetry get() = KmpSdkTelemetry

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isInitialized: Boolean get() = initialized

    fun init(
        config: KmpSdkConfig,
        tokenRefreshHandler: TokenRefreshHandler? = null,
        tokenStoreOverride: TokenStore? = null,
        configure: KmpSdkRegistry.() -> Unit = {},
    ) = initInternal(
        config = config,
        tokenRefreshHandler = tokenRefreshHandler,
        tokenStoreOverride = tokenStoreOverride,
        configure = configure,
        remoteConfigFetcher = null,
        builder = null,
    )

    fun init(block: KmpSdkInitBuilder.() -> Unit) {
        val builder = KmpSdkInitBuilder().apply(block)
        initInternal(
            config = builder.buildConfig(),
            tokenRefreshHandler = builder.tokenRefreshHandler,
            tokenStoreOverride = null,
            configure = { builder.modules().forEach(::install) },
            remoteConfigFetcher = builder.remoteConfigFetcherOrNull(),
            builder = builder,
        )
    }

    fun validate(): com.kmpsdk.core.validation.ValidationResult =
        KmpSdkValidator.validate(config, registry)

    private fun initInternal(
        config: KmpSdkConfig,
        tokenRefreshHandler: TokenRefreshHandler?,
        tokenStoreOverride: TokenStore?,
        configure: KmpSdkRegistry.() -> Unit,
        remoteConfigFetcher: (suspend () -> Map<String, String>)?,
        builder: KmpSdkInitBuilder?,
    ) {
        if (initialized) return

        this.config = config
        Logger.configure(config.logLevel)
        logger = Logger.create("KmpSdk")

        remoteConfig = RemoteConfigStore()
        tenantManager = TenantManager(config.baseUrl)

        tokenStore = tokenStoreOverride ?: createDefaultTokenStore(config)
        sessionManager = SessionManager(tokenStore, tokenRefreshHandler, logger)
        runBlocking { sessionManager.initialize() }

        val databaseDriverFactory = createDatabaseDriverFactory()
        database = KmpSdkDatabase(databaseDriverFactory.createDriver())

        connectivityMonitor = createConnectivityMonitor()
        messageEventBus = SharedMessageEventBus()
        messageNotifier = MessageNotifierAdapter(messageEventBus)

        cacheStore = TieredCacheStore(database, config)

        lateinit var offlineQueueRef: OfflineQueueManager
        networkClient = KmpNetworkClient(
            config = config,
            connectivityMonitor = connectivityMonitor,
            tokenStore = tokenStore,
            sessionManager = sessionManager,
            cacheStore = cacheStore,
            offlineQueueProvider = { offlineQueueRef },
            logger = logger,
        )
        tenantManager.onTenantSwitch { ctx -> networkClient.applyTenant(ctx) }
        networkClient.applyTenant(tenantManager.current.value)
        offlineQueue = OfflineQueueManager(
            database = database,
            networkClient = networkClient,
            connectivityMonitor = connectivityMonitor,
            config = config,
            scope = scope,
        ).also { offlineQueueRef = it }
        offlineExecutor = OfflineAwareRequestExecutor(
            networkClient = networkClient,
            connectivityMonitor = connectivityMonitor,
            offlineQueue = offlineQueue,
        )
        offlineActions = OfflineActionManager(database)
        syncCoordinator = SyncCoordinator(
            replayOfflineQueue = { offlineQueue.replayQueue() },
            logger = logger,
        )
        fileUpload = FileUploadHelper(networkClient)
        drafts = DraftStore(database, networkClient.json)
        query = QueryKit()
        realtime = RealtimeClient(networkClient, scope, logger)
        deepLinks = DeepLinkRouter(logger)
        push = PushPayloadRouter(logger = logger)
        backgroundWork = BackgroundWorkBridge(
            scope = scope,
            onSync = {
                val result = syncCoordinator.syncAll()
                offlineActions.replayPending()
                KmpSdkTelemetry.emit(
                    TelemetryEvent.SyncCompleted(
                        replayedOffline = result.replayedOfflineRequests,
                        refreshedRepos = result.refreshedRepositories,
                        failures = result.failures,
                    ),
                )
            },
            logger = logger,
        )
        dirtySyncCoordinator = DirtySyncCoordinator()
        debugger = KmpSdkDebugger(
            config = config,
            connectivityMonitor = connectivityMonitor,
            offlineQueue = offlineQueue,
            cacheStore = cacheStore,
            sessionManager = sessionManager,
            syncCoordinator = syncCoordinator,
        )

        context = KmpSdkContext(
            config = config,
            logger = logger,
            tokenStore = tokenStore,
            sessionManager = sessionManager,
            networkClient = networkClient,
            cacheStore = cacheStore,
            offlineQueue = offlineQueue,
            offlineExecutor = offlineExecutor,
            offlineActions = offlineActions,
            syncCoordinator = syncCoordinator,
            dirtySyncCoordinator = dirtySyncCoordinator,
            debugger = debugger,
            connectivityMonitor = connectivityMonitor,
            messageEventBus = messageEventBus,
            messageNotifier = messageNotifier,
            database = database,
            databaseDriverFactory = databaseDriverFactory,
            tenantManager = tenantManager,
            remoteConfig = remoteConfig,
            scope = scope,
            drafts = drafts,
            query = query,
            realtime = realtime,
            deepLinks = deepLinks,
            push = push,
            backgroundWork = backgroundWork,
        )
        registry = KmpSdkRegistry(context).apply(configure)

        runBlocking {
            remoteConfigFetcher?.let { fetcher ->
                remoteConfig.refresh(fetcher)
                this@KmpSdk.config = remoteConfig.apply(this@KmpSdk.config)
            }
        }

        if (config.validateOnStartup) {
            val validation = KmpSdkValidator.validate(config, registry)
            check(validation.isValid) {
                validation.issues.joinToString { it.message }
            }
        }

        ConnectivitySyncObserver(
            connectivityMonitor = connectivityMonitor,
            syncCoordinator = syncCoordinator,
            config = config,
            scope = scope,
        )

        BackgroundSyncScheduler(
            config = config,
            scope = scope,
            onSync = {
                val result = syncCoordinator.syncAll()
                offlineActions.replayPending()
                KmpSdkTelemetry.emit(
                    TelemetryEvent.SyncCompleted(
                        replayedOffline = result.replayedOfflineRequests,
                        refreshedRepos = result.refreshedRepositories,
                        failures = result.failures,
                    ),
                )
            },
        ).start()

        applyBuilderExtras(builder)

        initialized = true
        logger.i("KmpSdk initialized with baseUrl=${config.baseUrl}")
    }

    private fun applyBuilderExtras(builder: KmpSdkInitBuilder?) {
        if (builder == null) return
        builder.deepLinkBlockOrNull()?.let { deepLinks.routes(it) }
        builder.pushBlockOrNull()?.let { push.routes(it) }
        builder.backgroundWorkBlockOrNull()?.let { block ->
            backgroundWork.configure(com.kmpsdk.data.sync.BackgroundWorkDsl().apply(block).build())
        }
        val envBlocks = builder.environmentBlocksOrEmpty()
        if (envBlocks.isNotEmpty()) {
            val initial = builder.environmentName ?: envBlocks.keys.first()
            environments = EnvironmentVault(
                environmentBlocks = envBlocks,
                fallback = {
                    baseUrl = config.baseUrl
                    logLevel = config.logLevel
                },
                initialName = initial,
                onConfigRebuilt = { rebuilt -> this.config = rebuilt },
                tenantManager = tenantManager,
                networkClient = networkClient,
                logger = logger,
            )
        }
    }

    private fun createDefaultTokenStore(config: KmpSdkConfig): TokenStore =
        if (config.auth.useSecureTokenStore) PlatformTokenStore() else InMemoryTokenStore()

    inline fun <reified T : Any> get(): T = requireInitialized().registry.resolve()

    inline fun <reified T : Any> getOrNull(): T? = registry.resolveOrNull()

    fun requireInitialized(): KmpSdk {
        check(initialized) { "Call KmpSdk.init { ... } before using the SDK." }
        return this
    }
}
