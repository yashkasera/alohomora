package io.github.yashkasera.alohomora.di

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.repository.ErrorRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.EventsRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.SpanRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.TrafficRepositoryImpl
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import io.github.yashkasera.alohomora.devtools.FeatureFlagStore
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import io.github.yashkasera.alohomora.domain.service.SlackShareService
import io.github.yashkasera.alohomora.domain.usecase.cache.GetCacheUseCase
import io.github.yashkasera.alohomora.domain.usecase.error.ClearErrorsUseCase
import io.github.yashkasera.alohomora.domain.usecase.error.GetErrorDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.error.MarkErrorAsViewedUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.GetTrafficDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.MarkTrafficAsViewedUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ObserveReplayResultUseCase
import io.github.yashkasera.alohomora.domain.usecase.traffic.ReplayTrafficUseCase
import io.github.yashkasera.alohomora.presentation.ui.screens.cache.CacheViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.database.DatabaseViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.error.detail.ErrorDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.error.list.ErrorViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.events.EventsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.featureflags.FeatureFlagsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.githistory.GitHistoryViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.overview.OverviewViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.traces.detail.TraceDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.traces.list.TracesViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.traffic.detail.TrafficDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.traffic.list.TrafficViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json as KJson
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinApplication
import org.koin.dsl.module

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
    single { get<AlohomoraDb>().trafficDao() }
    single { get<AlohomoraDb>().eventDao() }
    single { get<AlohomoraDb>().errorDao() }
    single { get<AlohomoraDb>().screenDao() }
    single { get<AlohomoraDb>().spanDao() }

    single<TrafficRepository> { TrafficRepositoryImpl(get()) }
    single<EventsRepository> { EventsRepositoryImpl(get()) }
    single<ErrorRepository> { ErrorRepositoryImpl(get()) }
    single<SpanRepository> { SpanRepositoryImpl(get()) }

    single { FeatureFlagStore() }
    single { DevToolsRuntime(get(), get(), get(), get(), get(), get(), get()) }


    // UseCases
    factory { GetErrorDetailsUseCase(get()) }
    factory { MarkErrorAsViewedUseCase(get()) }
    factory { ClearErrorsUseCase(get()) }
    factory { GetTrafficDetailsUseCase(get()) }
    factory { MarkTrafficAsViewedUseCase(get()) }
    factory { ReplayTrafficUseCase() }
    factory { ObserveReplayResultUseCase(get()) }
    factory { GetCacheUseCase(get()) }

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
    viewModel { TrafficViewModel(get()) }
    viewModel { (traceId: String) ->
        TrafficDetailsViewModel(traceId, get(), get(), get(), get(), get(), get())
    }
    viewModel { EventsViewModel(get()) }
    viewModel { DatabaseViewModel(get()) }
    viewModel { CacheViewModel(get()) }
    viewModel { FeatureFlagsViewModel(get()) }
    viewModel { GitHistoryViewModel() }
    viewModel { ErrorViewModel(get()) }
    viewModel { (errorId: Long) -> ErrorDetailsViewModel(errorId, get(), get()) }
    viewModel { TracesViewModel(get()) }
    viewModel { (traceId: String) -> TraceDetailsViewModel(traceId, get()) }
}


internal expect val platformModule: org.koin.core.module.Module
