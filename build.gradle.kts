plugins {
    // Supplies the root `check` lifecycle task that consumerParity hooks into. The root applies no
    // Kotlin or Android plugin, so without this there is no root `check` at all and the wiring below
    // fails configuration outright.
    base
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.room).apply(false)
    alias(libs.plugins.ksp).apply(false)
}

val libVersion = providers.gradleProperty("alohomora.version").getOrElse("1.0.0")

allprojects {
    group = "io.github.yashkasera"
    version = libVersion
}

apiValidation {
    ignoredProjects += listOf("showcaseApp", "desktopApp", "alohomora-common")

    // Without this, :alohomora:apiCheck is a no-op: BCV only dumps JVM targets by default,
    // :alohomora has no jvm() target, and klib validation is disabled out of the box. The
    // repo previously claimed apiCheck guarded the public surface while enforcing nothing.
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

/**
 * Fails when `:alohomora` and `:alohomora-noop` no longer expose the same `Alohomora` object.
 *
 * `apiCheck` cannot catch this: BCV validates each module against its *own* golden file, so it
 * passes happily while the two objects drift apart. A consumer compiles one call site against
 * `:alohomora` (debugImplementation) and `:alohomora-noop` (releaseImplementation), so drift
 * surfaces only as a broken release build downstream.
 *
 * Only the members of the `Alohomora` object are compared. The two dumps differ at top level by
 * design: `:alohomora` re-exports `ReplayRequest`, `ReplayOutcome`, `TraceReplayHandler` and
 * `CustomScreenPlugin` from `:alohomora-common`, which is not API-validated, while the noop
 * declares its own copies — so those types appear as top-level declarations in the noop dump and
 * not in `:alohomora`'s. That asymmetry is expected and must not fail the build.
 *
 * Two things this does not cover, because the inputs do not carry them: `@JvmStatic`/`@JvmOverloads`
 * are absent from klib dumps (and `:alohomora` has no JVM dump to compare the noop's against), and
 * androidMain-only members are not in the klib dump either. Both still have to be mirrored by hand.
 */
abstract class ConsumerParityTask : DefaultTask() {

    @get:InputFile
    abstract val libraryDump: RegularFileProperty

    @get:InputFile
    abstract val noopDump: RegularFileProperty

    @get:Input
    abstract val objectFqName: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val fqName = objectFqName.get()
        val library = membersOf(libraryDump.get().asFile, fqName, ":alohomora")
        val noop = membersOf(noopDump.get().asFile, fqName, ":alohomora-noop")

        report.get().asFile.apply { parentFile.mkdirs() }.writeText(library.joinToString("\n"))

        val missing = library - noop
        val extra = noop - library
        if (missing.isEmpty() && extra.isEmpty()) return

        throw GradleException(
            buildString {
                appendLine("`$fqName` has diverged between :alohomora and :alohomora-noop.")
                appendLine("Consumers compile one call site against both, so this breaks release builds only.")
                if (missing.isNotEmpty()) {
                    appendLine()
                    appendLine("Missing from :alohomora-noop:")
                    missing.forEach { appendLine("    $it") }
                }
                if (extra.isNotEmpty()) {
                    appendLine()
                    appendLine("Extra in :alohomora-noop:")
                    extra.forEach { appendLine("    $it") }
                }
                appendLine()
                append("Mirror the member in the module that is behind, then run ./gradlew apiDump.")
            },
        )
    }

    /**
     * Pulls the member signatures out of the `final object <fqName> { ... }` block of a klib dump.
     *
     * Each line is stripped of its trailing ` // <signature id>` comment and of indentation, so a
     * property and its accessors compare as separate entries — that is deliberate, it catches a
     * `val` quietly becoming a `var`.
     */
    private fun membersOf(dump: File, fqName: String, module: String): Set<String> {
        val lines = dump.readLines()
        val header = "final object $fqName {"
        val start = lines.indexOfFirst { it.substringBefore(" // ").trimEnd() == header }
        if (start < 0) {
            throw GradleException(
                "No `$header` block in ${dump.path} (module $module). Either the object was renamed " +
                    "or the dump is stale — run ./gradlew apiDump and update objectFqName if needed.",
            )
        }
        val end = lines.subList(start + 1, lines.size).indexOfFirst { it == "}" }
        if (end < 0) throw GradleException("Unterminated `$header` block in ${dump.path} (module $module).")

        return lines.subList(start + 1, start + 1 + end)
            .map { it.substringBefore(" // ").trim() }
            .filter { it.isNotEmpty() }
            .toSortedSet()
    }
}

val consumerParity = tasks.register<ConsumerParityTask>("consumerParity") {
    group = "verification"
    description = "Fails when :alohomora and :alohomora-noop expose different Alohomora objects"

    // The golden files are only trustworthy once apiCheck has confirmed they match the sources;
    // without this the task would happily compare two stale dumps and report parity.
    dependsOn(":alohomora:apiCheck", ":alohomora-noop:apiCheck")

    libraryDump = layout.projectDirectory.file("alohomora/api/alohomora.klib.api")
    noopDump = layout.projectDirectory.file("alohomora-noop/api/alohomora-noop.klib.api")
    objectFqName = "io.github.yashkasera.alohomora/Alohomora"
    report = layout.buildDirectory.file("reports/consumerParity/alohomora-members.txt")
}

tasks.named("check") { dependsOn(consumerParity) }

// Instrumentation tests hang off `connectedCheck`/`deviceCheck`, neither of which `check` reaches
// — and the KMP device-test component is created without a Kotlin `testRegistry`, so `allTests`
// misses it too. Left alone that means `check` never even *compiles* the device tests, and a test
// broken by an unrelated refactor stays invisible until someone plugs in a phone. Assembling them
// is the compromise: it type-checks every device test on every `check` and still needs no device.
tasks.named("check") {
    dependsOn(":alohomora:assembleAndroidDeviceTest", ":showcaseApp:assembleDebugAndroidTest")
}

// All four modules must ship together: :alohomora's POM lists :alohomora-common and
// :alohomora-ui as runtime dependencies, so publishing only the first two leaves every
// external consumer with unresolvable dependencies.
val publishedProjects =
    listOf(":alohomora", ":alohomora-noop", ":alohomora-common", ":alohomora-ui")

// The Gradle plugin is the fifth published artifact and cannot go in the list above: it is an
// included build, not a subproject, so a `:alohomora-gradle-plugin:` task path does not resolve.
// `gradle.includedBuild(...)` is how the root reaches it, which keeps one publish entry point for
// all five rather than a second CI invocation that is easy to forget — the way the plugin went
// unpublished through v1.0.0 in the first place.
val pluginBuild = gradle.includedBuild("alohomora-gradle-plugin")

tasks.register("publishMavenCentral") {
    group = "publishing"
    description = "Publishes all Alohomora artifacts to Maven Central"
    dependsOn(publishedProjects.map { "$it:publishAllPublicationsToMavenCentralRepository" })
    dependsOn(pluginBuild.task(":publishAllPublicationsToMavenCentralRepository"))
}

tasks.register("publishToMavenLocal") {
    group = "publishing"
    description = "Publishes all Alohomora artifacts to Maven Local for verification"
    dependsOn(publishedProjects.map { "$it:publishToMavenLocal" })
    dependsOn(pluginBuild.task(":publishToMavenLocal"))
}
