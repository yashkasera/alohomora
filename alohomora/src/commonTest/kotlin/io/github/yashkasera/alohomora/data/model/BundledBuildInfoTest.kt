package io.github.yashkasera.alohomora.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Contract between `scripts/alohomora-build-info.sh` and the iOS runtime.
 *
 * The manifest below is copied from that script's real output — if a field is renamed on one side
 * only, `parsesTheGeneratorsOutput` is what catches it, because nothing else in the build connects
 * a shell script to Kotlin.
 */
class BundledBuildInfoTest {

    private val generatorOutput = """
        {
          "schemaVersion": 1,
          "branch": "main",
          "commitSha": "a67b984",
          "isDirty": true,
          "buildTimestampUtc": 1785617252000,
          "variantName": "Debug",
          "buildType": "debug",
          "flavorName": null,
          "slackWebhookUrl": null,
          "commits": [
            { "sha": "a67b984", "author": "Yash Kasera", "message": "fix: a \"quoted\" subject", "timestamp": 1785615773000 }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesTheGeneratorsOutput() {
        val info = assertNotNull(BundledBuildInfo.parse(generatorOutput))

        assertEquals("main", info.branch)
        assertEquals("a67b984", info.commitSha)
        assertTrue(info.isDirty)
        assertEquals(1785617252000L, info.buildTimestampUtc)
        assertEquals("Debug", info.variantName)
        assertEquals("debug", info.buildType)
        assertNull(info.flavorName)
        assertNull(info.slackWebhookUrl)
        assertEquals(1, info.commits.size)
        assertEquals("fix: a \"quoted\" subject", info.commits.single().message)
    }

    @Test
    fun commitTimestampsAreMilliseconds() {
        val info = assertNotNull(BundledBuildInfo.parse(generatorOutput))

        // Guards the `%ct * 1000` conversion in the generator. Seconds here would render every
        // commit in the GitHistory panel as a 1970 date.
        assertTrue(info.commits.single().timestamp > 1_000_000_000_000L)
    }

    @Test
    fun returnsNullForMalformedJsonInsteadOfThrowing() {
        assertNull(BundledBuildInfo.parse("{ this is not json"))
        assertNull(BundledBuildInfo.parse(""))
    }

    @Test
    fun toleratesAManifestFromADifferentGeneratorVersion() {
        // Unknown keys ignored, missing keys defaulted — so neither side of a script/library version
        // skew needs a coordinated release.
        val info = assertNotNull(
            BundledBuildInfo.parse("""{ "schemaVersion": 99, "commitSha": "deadbee", "somethingNew": true }"""),
        )

        assertEquals("deadbee", info.commitSha)
        assertEquals("unknown", info.branch)
        assertFalse(info.isDirty)
        assertEquals(emptyList(), info.commits)
    }

    @Test
    fun runtimeIdentityWinsOverTheManifest() {
        val config = AppIdentity(
            appName = "Showcase",
            packageName = "io.github.yashkasera.showcase",
            versionName = "2.1",
            versionCode = 47,
        ).toAlohomoraConfig(BundledBuildInfo.parse(generatorOutput))

        // Version and bundle id come from Info.plist, never from the manifest: the generator
        // deliberately does not emit them, so there is nothing to drift out of sync.
        assertEquals("Showcase", config.appName)
        assertEquals("io.github.yashkasera.showcase", config.packageName)
        assertEquals("2.1", config.versionName)
        assertEquals(47, config.versionCode)
        assertEquals("a67b984", config.commitSha)
        assertEquals(1, config.commits.size)
    }

    @Test
    fun missingManifestStillYieldsAUsableConfig() {
        val config = AppIdentity(
            appName = "Showcase",
            packageName = "io.github.yashkasera.showcase",
            versionName = "2.1",
            versionCode = 47,
        ).toAlohomoraConfig(buildInfo = null)

        assertEquals("2.1", config.versionName)
        assertEquals("unknown", config.branch)
        assertEquals("unknown", config.commitSha)
        assertEquals("unknown", config.variantName)
        assertFalse(config.isDirty)
        assertEquals(0L, config.buildTimestampUtc)
        assertEquals(emptyList(), config.commits)
    }

    @Test
    fun absentPlatformIdentityFallsBackToTheSameSentinelsAsAndroid() {
        val config = AppIdentity(null, null, null, null).toAlohomoraConfig(buildInfo = null)
        val noConfigAtAll: AlohomoraConfig? = null

        // `toBuildMetadata` substitutes "unknown"/-1 for a null config; a config full of nulls has
        // to read identically or the Config panel gains a second way of saying "no data".
        assertEquals(noConfigAtAll.toBuildMetadata(), config.toBuildMetadata())
    }

    @Test
    fun blankWebhookIsTreatedAsAbsent() {
        val config = AppIdentity(null, null, null, null)
            .toAlohomoraConfig(BundledBuildInfo.parse("""{ "slackWebhookUrl": "" }"""))

        assertNull(config.slackWebhookUrl)
    }
}
