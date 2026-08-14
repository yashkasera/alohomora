import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    id("io.github.yashkasera.alohomora")
}

android {
    namespace = "io.github.yashkasera.alohomora.showcaseApp"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        applicationId = "io.github.yashkasera.alohomora.showcaseApp"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        animationsDisabled = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    debugImplementation(project(":alohomora"))
    releaseImplementation(project(":alohomora-noop"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    implementation(libs.kotlinx.coroutines.android)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // OpenTelemetry. Declared here and nowhere else — see the catalog entry. `implementation`, not
    // `debugImplementation`: the exporter adapter lives in src/main so it also compiles against
    // alohomora-noop, where recordSpan is a no-op that discards what it is handed.
    implementation(libs.opentelemetry.sdk.trace)

    ksp(libs.androidx.room.compiler)

    // Instrumentation tests. Deliberately no `androidTestImplementation(project(":alohomora"))`:
    // androidTest only exists for the testBuildType (debug), and AGP already extends the
    // androidTest compile classpath from the tested variant's implementation configurations, so
    // `debugImplementation(project(":alohomora"))` above is on it — along with :alohomora-ui
    // transitively. An explicit entry would additionally bind if a release androidTest variant
    // ever appeared, which is exactly where :alohomora-noop is the intended binding.
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    // debugImplementation, not androidTestImplementation: ui-test-manifest contributes the bare
    // ComponentActivity to the *app under test*'s manifest, not the test APK's.
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

alohomora {
    enabledVariants = setOf("debug")
    maxCommits = 50
    slackWebhookUrl = providers.gradleProperty("SLACK_TOKEN").orNull
    versionName = project.version.toString()
    versionCode = 1
}
