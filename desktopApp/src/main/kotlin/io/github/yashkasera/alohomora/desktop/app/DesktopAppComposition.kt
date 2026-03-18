package io.github.yashkasera.alohomora.desktop.app

import io.github.yashkasera.alohomora.desktop.data.adb.AdbRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.devtools.DevToolsRemoteDataSource
import io.github.yashkasera.alohomora.desktop.data.devtools.DevToolsRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.local.ApiLogStore
import io.github.yashkasera.alohomora.desktop.data.local.BuildInfoStore
import io.github.yashkasera.alohomora.desktop.data.local.ChronicleStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.PrefsRepositoryImpl
import io.github.yashkasera.alohomora.desktop.data.local.PrefsStore
import io.github.yashkasera.alohomora.desktop.data.logcat.LogcatRepositoryImpl
import io.github.yashkasera.alohomora.desktop.domain.service.SlackShareService
import io.github.yashkasera.alohomora.desktop.domain.usecase.ClearLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ConnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DeactivateDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DisconnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.InstallApkUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ObserveLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RefreshDevicesUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestInitialStateUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestPrefValueUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RunAdbCommandUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SelectDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StartLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StopLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SwitchDevToolsDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.UninstallPackageUseCase
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PrefsViewModel
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
    val prefsViewModel: PrefsViewModel

    init {
        val eventStore = EventStore()
        val apiLogStore = ApiLogStore()
        val databaseSnapshotStore = DatabaseSnapshotStore()
        val prefsStore = PrefsStore()
        val buildInfoStore = BuildInfoStore()
        val chronicleStore = ChronicleStore()

        val devToolsRepository = DevToolsRepositoryImpl(
            remoteDataSource = DevToolsRemoteDataSource(),
            eventStore = eventStore,
            apiLogStore = apiLogStore,
            databaseStore = databaseSnapshotStore,
            prefsStore = prefsStore,
            buildInfoStore = buildInfoStore,
            chronicleStore = chronicleStore,
        )

        val adbRepository = AdbRepositoryImpl()
        val logcatRepository = LogcatRepositoryImpl()
        val databaseRepository = DatabaseRepositoryImpl(databaseSnapshotStore)
        val prefsRepository = PrefsRepositoryImpl(prefsStore)

        val refreshDevicesUseCase = RefreshDevicesUseCase(adbRepository)
        val selectDeviceUseCase = SelectDeviceUseCase(adbRepository)
        val deactivateDeviceUseCase = DeactivateDeviceUseCase(adbRepository)
        val runAdbCommandUseCase = RunAdbCommandUseCase(adbRepository)
        val installApkUseCase = InstallApkUseCase(adbRepository)
        val uninstallPackageUseCase = UninstallPackageUseCase(adbRepository)

        val connectDevToolsUseCase = ConnectDevToolsUseCase(devToolsRepository)
        val disconnectDevToolsUseCase = DisconnectDevToolsUseCase(devToolsRepository)
        val switchDevToolsDeviceUseCase = SwitchDevToolsDeviceUseCase(devToolsRepository)
        val requestInitialStateUseCase = RequestInitialStateUseCase(devToolsRepository)
        val requestDatabaseSchemaUseCase = RequestDatabaseSchemaUseCase(devToolsRepository)
        val requestDatabaseTableUseCase = RequestDatabaseTableUseCase(devToolsRepository)
        val requestPrefValueUseCase = RequestPrefValueUseCase(devToolsRepository)

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
        val slackShareService = SlackShareService(slackHttpClient)

        devicesViewModel = sharedDevicesViewModel ?: DevicesViewModel(
            repository = adbRepository,
            refreshDevicesUseCase = refreshDevicesUseCase,
            selectDeviceUseCase = selectDeviceUseCase,
            deactivateDeviceUseCase = deactivateDeviceUseCase,
            runAdbCommandUseCase = runAdbCommandUseCase,
            installApkUseCase = installApkUseCase,
            uninstallPackageUseCase = uninstallPackageUseCase,
        )

        devToolsViewModel = DevToolsViewModel(
            repository = devToolsRepository,
            connectUseCase = connectDevToolsUseCase,
            disconnectUseCase = disconnectDevToolsUseCase,
            switchDeviceUseCase = switchDevToolsDeviceUseCase,
            requestInitialStateUseCase = requestInitialStateUseCase,
            requestDatabaseSchemaUseCase = requestDatabaseSchemaUseCase,
            requestDatabaseTableUseCase = requestDatabaseTableUseCase,
            requestPrefValueUseCase = requestPrefValueUseCase,
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

        prefsViewModel = PrefsViewModel(
            repository = prefsRepository,
            requestPrefValueUseCase = requestPrefValueUseCase,
        )
    }

    fun close() {
        devToolsViewModel.close()
    }
}
