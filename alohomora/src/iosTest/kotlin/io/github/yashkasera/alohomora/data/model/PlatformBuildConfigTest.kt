package io.github.yashkasera.alohomora.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlatformBuildConfigTest {

    /**
     * The test binary's bundle has no `alohomora-build-info.json`, which is the state of every app
     * whose Xcode project has not added the script phase yet.
     *
     * Worth a test of its own because the miss goes through a cinterop call
     * (`stringWithContentsOfFile` with a null `NSError**`) that would trap rather than return, and
     * this runs inside `Alohomora.init()` — a crash here takes the host app down at launch.
     */
    @Test
    fun survivesAMissingManifest() {
        val config = assertNotNull(discoverPlatformBuildConfig())

        assertEquals("unknown", config.branch)
        assertEquals("unknown", config.commitSha)
        assertEquals(emptyList(), config.commits)
    }

    /** Repeat calls must be side-effect free; `init()` is guarded but callers are not. */
    @Test
    fun isIdempotent() {
        val first = assertNotNull(discoverPlatformBuildConfig())
        val second = assertNotNull(discoverPlatformBuildConfig())

        assertEquals(first.projectName, second.projectName)
        assertEquals(first.versionName, second.versionName)
    }
}
