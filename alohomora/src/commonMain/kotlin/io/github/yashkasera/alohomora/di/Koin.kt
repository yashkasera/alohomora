package io.github.yashkasera.alohomora.di

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.repository.CrashRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.EventRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.LogRepositoryImpl
import io.github.yashkasera.alohomora.data.repository.NetworkRepositoryImpl
import io.github.yashkasera.alohomora.domain.repository.CrashRepository
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import io.github.yashkasera.alohomora.domain.repository.LogRepository
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import io.github.yashkasera.alohomora.domain.usecase.ClearCrashesUseCase
import io.github.yashkasera.alohomora.domain.usecase.GetApiLogDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.GetCrashDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.GetCrashesUseCase
import io.github.yashkasera.alohomora.domain.usecase.GetEventsUseCase
import io.github.yashkasera.alohomora.domain.usecase.GetLogsUseCase
import io.github.yashkasera.alohomora.domain.usecase.GetNetworkCallsUseCase
import io.github.yashkasera.alohomora.domain.usecase.MarkCrashAsViewedUseCase
import io.github.yashkasera.alohomora.presentation.ui.screens.apilog.detail.ApiLogDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.apilog.list.ApiLogsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.commithistory.CommitHistoryViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.crashes.details.CrashDetailsViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.crashes.list.CrashListViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.dashboard.DashboardViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.database.DatabaseViewModel
import io.github.yashkasera.alohomora.presentation.ui.screens.events.EventsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule, platformModule)
}

// Common modules
internal val appModule = module {
//    single { get<AlohomoraDb>().logDao() }
    single { get<AlohomoraDb>().networkDao() }
    single { get<AlohomoraDb>().eventDao() }
    single { get<AlohomoraDb>().crashDao() }
    single { get<AlohomoraDb>().screenDao() }

    single<LogRepository> { LogRepositoryImpl(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<EventRepository> { EventRepositoryImpl(get()) }
    single<CrashRepository> { CrashRepositoryImpl(get()) }

//    single { SyncService(get(), get()) }

    // UseCases
    factory { GetLogsUseCase(get()) }
    factory { GetNetworkCallsUseCase(get()) }
    factory { GetEventsUseCase(get()) }
    factory { GetCrashesUseCase(get()) }
    factory { GetCrashDetailsUseCase(get()) }
    factory { ClearCrashesUseCase(get()) }
    factory { MarkCrashAsViewedUseCase(get()) }
    factory { GetApiLogDetailsUseCase(get()) }
//    factory { ConnectToRemoteUseCase(get()) }

    // ViewModels
    viewModel {
        DashboardViewModel(
            get(),
//        get()
        )
    }
    viewModel { ApiLogsViewModel(get()) }
    viewModel { (logId: String) -> ApiLogDetailsViewModel(logId, get()) }
    viewModel { EventsViewModel(get()) }
    viewModel { DatabaseViewModel() }
    viewModel { CommitHistoryViewModel() }
    viewModel { CrashListViewModel(get(), get()) }
    viewModel { (crashId: Long) -> CrashDetailsViewModel(crashId, get(), get()) }
}


expect val platformModule: org.koin.core.module.Module
