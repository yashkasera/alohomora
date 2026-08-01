package io.github.yashkasera.alohomora.di

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.repository.IncidentRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.TelemetryRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.TraceRepositoryImpl
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import io.github.yashkasera.alohomora.domain.repository.IncidentRepository
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import io.github.yashkasera.alohomora.domain.repository.TraceRepository
import io.github.yashkasera.alohomora.domain.usecase.cache.GetPreferencesUseCase
import io.github.yashkasera.alohomora.domain.usecase.trace.GetTraceDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.trace.MarkTraceAsViewedUseCase
import io.github.yashkasera.alohomora.domain.usecase.incident.GetIncidentDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.incident.MarkIncidentAsViewedUseCase
import io.github.yashkasera.alohomora.domain.usecase.incident.ClearIncidentsUseCase
import io.github.yashkasera.alohomora.presentation.ui.screens.cache.CacheViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail.TraceDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.trace.list.TraceViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.telemetry.TelemetryViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.incident.detail.IncidentDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.incident.list.IncidentViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.vault.VaultViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.chronicle.ChronicleViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.overview.OverviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import io.github.yashkasera.alohomora.domain.service.SlackShareService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json as KJson
/**
 * Builds Alohomora's **isolated** Koin container.
 *
 * Deliberately `koinApplication`, not `startKoin`: `startKoin` writes to Koin's
 * `GlobalContext` and throws `KoinAppAlreadyStartedException` if it is already set. Since
 * `AlohomoraInitializer` runs from a ContentProvider — before `Application.onCreate` — the
 * library would always win that race and break every host app that starts its own Koin.
 * Nothing here touches `GlobalContext`; the instance is held privately by `Alohomora` and
 * handed to Compose via `KoinIsolatedContext`.
 */
internal fun initKoin(appDeclaration: KoinAppDeclaration = {}) = koinApplication {
    appDeclaration()
    modules(appModule, platformModule)
}

// Common modules
internal val appModule = module {
    single { get<AlohomoraDb>().traceDao() }
    single { get<AlohomoraDb>().telemetryDao() }
    single { get<AlohomoraDb>().incidentDao() }
    single { get<AlohomoraDb>().screenDao() }

    single<TraceRepository> { TraceRepositoryImpl(get()) }
    single<TelemetryRepository> { TelemetryRepositoryImpl(get()) }
    single<IncidentRepository> { IncidentRepositoryImpl(get()) }
    // PreferenceRepository is provided in platformModule

    single { DevToolsRuntime(get(), get(), get(), get(), get(), get()) }


    // UseCases
    factory { GetIncidentDetailsUseCase(get()) }
    factory { MarkIncidentAsViewedUseCase(get()) }
    factory { ClearIncidentsUseCase(get()) }
    factory { GetTraceDetailsUseCase(get()) }
    factory { MarkTraceAsViewedUseCase(get()) }
    factory { GetPreferencesUseCase(get()) }

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(KJson { ignoreUnknownKeys = true })
            }
        }
    }
    single {
        SlackShareService(httpClient = get())
    }

    // ViewModels
    viewModel { OverviewViewModel(get()) }
    viewModel { TraceViewModel(get()) }
    viewModel { (traceId: String) -> TraceDetailsViewModel(traceId, get(), get(), get(), get()) }
    viewModel { TelemetryViewModel(get()) }
    viewModel { VaultViewModel(get()) }
    viewModel { CacheViewModel(get()) }
    viewModel { ChronicleViewModel() }
    viewModel { IncidentViewModel(get()) }
    viewModel { (incidentId: Long) -> IncidentDetailsViewModel(incidentId, get(), get()) }
}


internal expect val platformModule: org.koin.core.module.Module
