package io.github.yashkasera.alohomora.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val UNKNOWN = "unknown"

/**
 * Build metadata written into the app bundle by an external generator, for platforms where
 * Alohomora's Gradle plugin is not part of the build.
 *
 * On Android the plugin code-generates an [AlohomoraConfig] and `AlohomoraInitializer` finds it via
 * `ServiceLoader`. An iOS app built from an `.xcodeproj` has no Gradle build to hook, so
 * `scripts/alohomora-build-info.sh` runs from an Xcode script phase and drops this JSON into the app
 * bundle instead. Same contract, different transport — which is why nothing here is public API and
 * `Alohomora.init()` keeps its zero-argument signature (the noop mirror must match it exactly).
 *
 * **Only fields that cannot be recovered at runtime belong here.** `versionName`, `versionCode`,
 * `packageName` and `appName` are all readable from `Bundle.main`, so injecting them would
 * create a second source of truth that drifts the moment someone edits `MARKETING_VERSION` without
 * rebuilding — see [AppIdentity].
 */
@Serializable
internal data class BundledBuildInfo(
    val schemaVersion: Int = SCHEMA_VERSION,
    val branch: String = UNKNOWN,
    val commitSha: String = UNKNOWN,
    val isDirty: Boolean = false,
    val buildTimestampUtc: Long = 0L,
    val variantName: String = UNKNOWN,
    val buildType: String? = null,
    val flavorName: String? = null,
    val slackWebhookUrl: String? = null,
    val commits: List<BundledCommit> = emptyList(),
) {
    companion object {
        /**
         * Bumped only when a field changes meaning. Added and removed fields need no bump: every
         * property defaults and the parser ignores unknown keys, so a manifest from either side of
         * the change still loads.
         */
        const val SCHEMA_VERSION = 1

        /** Base name of the bundled resource; the generator writes `$RESOURCE_NAME.$RESOURCE_EXTENSION`. */
        const val RESOURCE_NAME = "alohomora-build-info"
        const val RESOURCE_EXTENSION = "json"

        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Returns null rather than throwing on malformed input.
         *
         * A stale or hand-mangled manifest should cost you the Config and GitHistory panels, never
         * `Alohomora.init()` — the library is a debugging aid and must not be the reason an app
         * fails to start.
         */
        fun parse(raw: String): BundledBuildInfo? = try {
            json.decodeFromString<BundledBuildInfo>(raw).also { parsed ->
                if (parsed.schemaVersion > SCHEMA_VERSION) {
                    println(
                        "[Alohomora] $RESOURCE_NAME.$RESOURCE_EXTENSION declares schema " +
                            "v${parsed.schemaVersion} but this build understands v$SCHEMA_VERSION; " +
                            "some fields may be ignored. Update the Alohomora dependency.",
                    )
                }
            }
        } catch (e: Exception) {
            println("[Alohomora] ignoring unreadable $RESOURCE_NAME.$RESOURCE_EXTENSION: ${e.message}")
            null
        }
    }
}

/**
 * @param timestamp epoch **milliseconds**. `git log --pretty=%ct` emits seconds; the generator
 *   multiplies once on the way out so nothing downstream has to remember that commits are the odd
 *   one out. Getting this wrong renders every commit as a 1970 date, which is exactly what the
 *   Gradle task's equivalent conversion was added to fix.
 */
@Serializable
internal data class BundledCommit(
    val sha: String = "",
    val author: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
)

/**
 * The half of [AlohomoraConfig] that a platform can answer for itself at runtime.
 *
 * Nullable throughout: the caller passes whatever the platform reports and the adapter substitutes
 * the same `"unknown"` sentinels [toBuildMetadata] already uses, so a missing Info.plist key looks
 * identical to a missing manifest field.
 */
internal data class AppIdentity(
    val appName: String?,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Int?,
)

/**
 * Combines runtime app identity with build-time git metadata into an [AlohomoraConfig].
 *
 * [buildInfo] may be null — the generator not having run is the common case on a first build, and a
 * config carrying just the version and bundle id is strictly more useful than none at all.
 */
internal fun AppIdentity.toAlohomoraConfig(buildInfo: BundledBuildInfo?): AlohomoraConfig =
    BundledAlohomoraConfig(identity = this, buildInfo = buildInfo)

private class BundledAlohomoraConfig(
    identity: AppIdentity,
    buildInfo: BundledBuildInfo?,
) : AlohomoraConfig {
    override val appName: String? = identity.appName
    override val packageName: String? = identity.packageName
    override val versionName: String = identity.versionName ?: UNKNOWN
    override val versionCode: Int = identity.versionCode ?: -1
    override val variantName: String = buildInfo?.variantName ?: UNKNOWN
    override val flavorName: String? = buildInfo?.flavorName
    override val buildType: String? = buildInfo?.buildType
    override val branch: String = buildInfo?.branch ?: UNKNOWN
    override val commitSha: String = buildInfo?.commitSha ?: UNKNOWN
    override val isDirty: Boolean = buildInfo?.isDirty ?: false
    override val buildTimestampUtc: Long = buildInfo?.buildTimestampUtc ?: 0L
    override val slackWebhookUrl: String? = buildInfo?.slackWebhookUrl?.takeIf { it.isNotBlank() }
    override val commits: List<GitHistoryCommit> = buildInfo?.commits.orEmpty().map {
        GitHistoryCommit(
            sha = it.sha,
            author = it.author,
            message = it.message,
            timestamp = it.timestamp,
        )
    }
}
