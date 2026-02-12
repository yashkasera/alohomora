package io.github.yashkasera.alohomora.showcaseApp.di

import androidx.room.Room
import io.github.yashkasera.alohomora.showcaseApp.data.api.PostsApi
import io.github.yashkasera.alohomora.showcaseApp.data.db.AppDatabase
import io.github.yashkasera.alohomora.showcaseApp.data.preferences.PreferencesDataSource
import io.github.yashkasera.alohomora.showcaseApp.data.repository.PostRepositoryImpl
import io.github.yashkasera.alohomora.showcaseApp.data.repository.PreferencesRepositoryImpl
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PostRepository
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PreferencesRepository
import io.github.yashkasera.alohomora.showcaseApp.domain.usecase.GetPreferencesUseCase
import io.github.yashkasera.alohomora.showcaseApp.domain.usecase.ObservePostsUseCase
import io.github.yashkasera.alohomora.showcaseApp.domain.usecase.RefreshPostsUseCase
import io.github.yashkasera.alohomora.showcaseApp.domain.usecase.UpdatePreferencesUseCase
import io.github.yashkasera.alohomora.showcaseApp.presentation.PostsViewModel
import io.github.yashkasera.alohomora.network.AlohomoraInspector
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(get())
            }
            install(AlohomoraInspector)
        }
    }

    single {
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "android_sample.db"
        ).build()
    }

    single { get<AppDatabase>().postDao() }
    single { PreferencesDataSource(get()) }
    single { PostsApi(get()) }

    single<PostRepository> { PostRepositoryImpl(get(), get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }

    factory { ObservePostsUseCase(get()) }
    factory { RefreshPostsUseCase(get()) }
    factory { GetPreferencesUseCase(get()) }
    factory { UpdatePreferencesUseCase(get()) }

    viewModel { PostsViewModel(get(), get(), get(), get()) }
}
