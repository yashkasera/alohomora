package io.github.yashkasera.alohomora.desktop.app

import io.github.yashkasera.alohomora.desktop.data.adb.AdbRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.devtools.DevToolsRemoteDataSource
import io.github.yashkasera.alohomora.desktop.data.devtools.DevToolsRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.local.BuildMetadataStore
import io.github.yashkasera.alohomora.desktop.data.local.CacheRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.local.CacheStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.ErrorStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.FeatureFlagStore
import io.github.yashkasera.alohomora.desktop.data.local.GitHistoryStore
import io.github.yashkasera.alohomora.desktop.data.local.ReplayStore
import io.github.yashkasera.alohomora.desktop.data.local.SpanStore
import io.github.yashkasera.alohomora.desktop.data.local.TrafficStore
import io.github.yashkasera.alohomora.desktop.data.logcat.LogcatRepositoryImpl
import io.github.yashkasera.alohomora.desktop.domain.service.SlackShareService
import io.github.yashkasera.alohomora.desktop.domain.usecase.ClearLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ConnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DeactivateDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DisconnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.InstallApkUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ObserveLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RefreshDevicesUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ReplayTrafficUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheValueUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestInitialStateUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RunAdbCommandUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SelectDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StartLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StopLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SwitchDevToolsDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.UninstallPackageUseCase
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TracesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TrafficViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.json.Json

class DesktopAppComposition(
    sharedDevicesViewModel: DevicesViewModel? = null,
) {
    val devicesViewModel: DevicesViewModel
    val devToolsViewModel: DevToolsViewModel
    val logcatViewModel: LogcatViewModel
    val databaseViewModel: DatabaseViewModel
    val cacheViewModel: CacheViewModel
    val featureFlagsViewModel: io.github.yashkasera.alohomora.desktop.presentation.viewmodel.FeatureFlagViewModel
    val tracesViewModel: TracesViewModel
    val eventsViewModel: EventsViewModel
    val trafficViewModel: TrafficViewModel
    val networkRulesViewModel: NetworkRulesViewModel

    /** Everything [close] has to release. Held so per-window teardown is complete. */
    private val devToolsRepositoryRef: DevToolsRepositoryImpl
    private val slackHttpClientRef: HttpClient

    /** True when [devicesViewModel] is owned by another composition and must not be closed. */
    private val ownsDevicesViewModel: Boolean = sharedDevicesViewModel == null

    init {
        val eventStore = EventStore()
        val errorStore = ErrorStore()
        val spanStore = SpanStore()
        val trafficStore = TrafficStore()
        val databaseSnapshotStore = DatabaseSnapshotStore()
        val cacheStore = CacheStore()
        val featureFlagStore = FeatureFlagStore()
        val buildMetadataStore = BuildMetadataStore()
        val gitHistoryStore = GitHistoryStore()
        val replayStore = ReplayStore()

        val devToolsRepository = DevToolsRepositoryImpl(
            remoteDataSource = DevToolsRemoteDataSource(),
            eventStore = eventStore,
            errorStore = errorStore,
            spanStore = spanStore,
            trafficStore = trafficStore,
            databaseStore = databaseSnapshotStore,
            cacheStore = cacheStore,
            featureFlagStore = featureFlagStore,
            buildMetadataStore = buildMetadataStore,
            gitHistoryStore = gitHistoryStore,
            replayStore = replayStore,
        )

        devToolsRepositoryRef = devToolsRepository

        // Built only when this composition actually owns the DevicesViewModel. Previously
        // AdbRepositoryImpl (and its CoroutineScope) plus six use cases were constructed
        // unconditionally and then thrown away whenever a shared view model was supplied.
        val adbRepository = if (ownsDevicesViewModel) AdbRepositoryImpl() else null
        val logcatRepository = LogcatRepositoryImpl()
        val databaseRepository = io.github.yashkasera.alohomora.desktop.data.local.DatabaseRepositoryImpl(databaseSnapshotStore)
        val cacheRepository = CacheRepositoryImpl(cacheStore)

        val connectDevToolsUseCase = ConnectDevToolsUseCase(devToolsRepository)
        val disconnectDevToolsUseCase = DisconnectDevToolsUseCase(devToolsRepository)
        val switchDevToolsDeviceUseCase = SwitchDevToolsDeviceUseCase(devToolsRepository)
        val requestInitialStateUseCase = RequestInitialStateUseCase(devToolsRepository)
        val requestDatabaseSchemaUseCase = RequestDatabaseSchemaUseCase(devToolsRepository)
        val requestDatabaseTableUseCase = RequestDatabaseTableUseCase(devToolsRepository)
        val requestCacheValueUseCase = RequestCacheValueUseCase(devToolsRepository)
        val replayTrafficUseCase = ReplayTrafficUseCase(devToolsRepository)

        val observeLogcatUseCase = ObserveLogcatUseCase(logcatRepository)
        val startLogcatUseCase = StartLogcatUseCase(logcatRepository)
        val stopLogcatUseCase = StopLogcatUseCase()
        val clearLogcatUseCase = ClearLogcatUseCase(logcatRepository)

        val slackHttpClient = HttpClient(CIO) {
            engine {
                https {
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        slackHttpClientRef = slackHttpClient
        val slackShareService = SlackShareService(slackHttpClient)

        devicesViewModel = if (adbRepository == null) {
            requireNotNull(sharedDevicesViewModel)
        } else {
            DevicesViewModel(
                repository = adbRepository,
                refreshDevicesUseCase = RefreshDevicesUseCase(adbRepository),
                selectDeviceUseCase = SelectDeviceUseCase(adbRepository),
                deactivateDeviceUseCase = DeactivateDeviceUseCase(adbRepository),
                runAdbCommandUseCase = RunAdbCommandUseCase(adbRepository),
                installApkUseCase = InstallApkUseCase(adbRepository),
                uninstallPackageUseCase = UninstallPackageUseCase(adbRepository),
            )
        }

        devToolsViewModel = DevToolsViewModel(
            repository = devToolsRepository,
            connectUseCase = connectDevToolsUseCase,
            disconnectUseCase = disconnectDevToolsUseCase,
            switchDeviceUseCase = switchDevToolsDeviceUseCase,
            requestInitialStateUseCase = requestInitialStateUseCase,
            requestDatabaseSchemaUseCase = requestDatabaseSchemaUseCase,
            requestDatabaseTableUseCase = requestDatabaseTableUseCase,
            requestCacheValueUseCase = requestCacheValueUseCase,
            replayTrafficUseCase = replayTrafficUseCase,
            slackShareService = slackShareService,
        )

        logcatViewModel = LogcatViewModel(
            repository = logcatRepository,
            observeLogcatUseCase = observeLogcatUseCase,
            startLogcatUseCase = startLogcatUseCase,
            stopLogcatUseCase = stopLogcatUseCase,
            clearLogcatUseCase = clearLogcatUseCase,
        )

        databaseViewModel = DatabaseViewModel(
            repository = databaseRepository,
            requestDatabaseSchemaUseCase = requestDatabaseSchemaUseCase,
            requestDatabaseTableUseCase = requestDatabaseTableUseCase,
        )

        cacheViewModel = CacheViewModel(
            repository = cacheRepository,
            requestCacheValueUseCase = requestCacheValueUseCase,
        )

        featureFlagsViewModel = io.github.yashkasera.alohomora.desktop.presentation.viewmodel.FeatureFlagViewModel(
            repository = devToolsRepository,
        )
        tracesViewModel = TracesViewModel(repository = devToolsRepository)
        eventsViewModel = EventsViewModel(repository = devToolsRepository)
        trafficViewModel = TrafficViewModel(repository = devToolsRepository)
        networkRulesViewModel = NetworkRulesViewModel(repository = devToolsRepository)
    }

    /**
     * Releases everything this composition allocated. Call exactly once per window close.
     *
     * Previously this closed only [devToolsViewModel], leaking — per closed window — four
     * view-model scopes (one with an Eagerly-started `stateIn` collector), the DevTools
     * repository scope and socket, and the Ktor CIO [HttpClient] with its engine dispatcher
     * and selector threads. Open and close twenty device windows and twenty CIO engines stayed
     * alive for the life of the process.
     */
    fun close() {
        devToolsViewModel.close()
        logcatViewModel.close()
        databaseViewModel.close()
        cacheViewModel.close()
        featureFlagsViewModel.close()
        tracesViewModel.close()
        eventsViewModel.close()
        trafficViewModel.close()
        networkRulesViewModel.close()
        devToolsRepositoryRef.close()
        // Only if we built it — a shared view model outlives this window.
        if (ownsDevicesViewModel) devicesViewModel.close()
        slackHttpClientRef.close()
    }
}
