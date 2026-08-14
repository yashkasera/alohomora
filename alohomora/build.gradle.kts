import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    id("alohomora.publish")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "io.github.yashkasera.alohomora"
        // Deliberately not the newest SDK. A published library's compileSdk becomes a hard floor for
        // every consumer via aar-metadata.properties: at 37 an app on AGP 8.13.2 fails
        // checkDebugAarMetadata outright, because 36 is that AGP's ceiling. So compiling this against
        // the newest SDK would silently require every consumer to be on AGP 9 — an absurd cost for a
        // debug tool whose whole value is being easy to drop into an existing app. Raise it only when
        // something here actually needs a newer API; nothing does today.
        compileSdk = 36
        minSdk = 24
        androidResources.enable = true
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        // Without this, `commonTest` compiles for iOS only and the Android half of every
        // expect/actual pair goes unexercised — the build warned about it instead of failing, so
        // the gap was invisible. `androidHostTest` is also the only place the crash-handler
        // chaining test can live: `Thread.setDefaultUncaughtExceptionHandler` has no iOS analogue.
        //
        // Also load-bearing for `withDeviceTest` below: AGP's device-test DSL info reads
        // `androidTestOnJvmOptions!!.enableCoverage`, so removing this NPEs at configuration time.
        withHostTest {}

        // The console UI can only be exercised on a device. `runComposeUiTest` on the Android
        // host reads `Build.FINGERPRINT` and NPEs inside Compose's Robolectric idling strategy —
        // the reason `ComposeTest` lives in `iosTest`. `androidDeviceTest` is the Android half.
        //
        // Note the source set does NOT see `commonTest`: AGP gives the device-test compilation a
        // `None` source-set tree, unlike host tests which sit on the `test` tree. Shared fixtures
        // must live in `androidDeviceTest` itself. Do not "fix" this by forcing the tree name —
        // that would drag every `commonTest` file into a device compilation it was never written
        // for. `internal` declarations from `commonMain` and `androidMain` are visible either
        // way: AGP puts the main compilation's classes.jar on `friendPaths`.
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            animationsDisabled = true
        }
    }

    // iosX64 is intentionally absent: Compose Multiplatform 1.11.x no longer publishes
    // iosX64 variants, so declaring it produces a target that can never be compiled.
    val xcf = XCFramework("AlohomoraKit")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // api, not implementation: these modules' types appear in Alohomora's public
            // surface (TraceEntry from -common, ImageVector/theme from -ui via
            // CustomScreenPlugin), so consumers need them on their compile classpath.
            api(project(":alohomora-common"))
            api(project(":alohomora-ui"))
            api(libs.compose.ui)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3)


            implementation(libs.kermit)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.kotlinx.datetime)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            // Ktor
            implementation(libs.ktor.client.websockets)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            // api: TraceInterceptor implements okhttp3.Interceptor publicly, so a consumer
            // constructing it needs okhttp on their own compile classpath.
            api(libs.okhttp)

            implementation(libs.androidx.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.startup.runtime)

            // compileOnly: syncFirebaseRemoteConfig() compiles against it but consumers who
            // don't use Firebase never see the transitive dependency. Consumers who do already
            // declare firebase-config themselves, so the class is present at runtime.
            compileOnly(libs.firebase.config)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // Declared separately from `commonTest` on purpose — see the `withDeviceTest` comment
        // above; the device-test compilation does not inherit it.
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.rules)
            implementation(libs.androidx.test.ext.junit)
        }

    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "AlohomoraKit"
                    isStatic = true
                    xcf.add(this)
                }
            }
        }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

room {
    schemaDirectory("kspAndroidMain", "$projectDir/schemas/kspAndroidMain")
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

// Repository + POM configuration comes from the `alohomora.publish` convention plugin.
