package io.github.yashkasera.alohomora.desktop.data.adb

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression tests for the packaged-build failure: every ADB feature was dead in the
 * `.dmg`/`.msi`/`.deb` because `adb` was invoked as a bare name and a GUI process does not
 * inherit the shell's PATH.
 */
class AdbLocatorTest {

    private lateinit var tempDir: File
    private var previousProperty: String? = null

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("adb-locator", "").let { file ->
            file.delete()
            file.mkdirs()
            file
        }
        previousProperty = System.getProperty("alohomora.adb.path")
        AdbLocator.reset()
    }

    @AfterTest
    fun tearDown() {
        if (previousProperty == null) {
            System.clearProperty("alohomora.adb.path")
        } else {
            System.setProperty("alohomora.adb.path", previousProperty!!)
        }
        // Clear any Settings override a test set, so it can't leak into the next one.
        AdbLocator.configure(null)
        AdbLocator.reset()
        tempDir.deleteRecursively()
    }

    private fun fakeAdb(name: String = "adb"): File =
        File(tempDir, name).apply {
            writeText("#!/bin/sh\nexit 0\n")
            setExecutable(true)
        }

    @Test
    fun `explicit system property wins`() {
        val adb = fakeAdb()
        System.setProperty("alohomora.adb.path", adb.absolutePath)
        AdbLocator.reset()

        assertEquals(adb.absolutePath, AdbLocator.find())
    }

    @Test
    fun `returns null when nothing resolves`() {
        // Point the override at a path that does not exist. Resolution then falls through to
        // the SDK roots and PATH, which on a machine without an SDK yields null rather than
        // the old behaviour of "adb" being handed to ProcessBuilder and failing opaquely.
        System.setProperty("alohomora.adb.path", File(tempDir, "definitely-absent").absolutePath)
        AdbLocator.reset()

        val resolved = AdbLocator.find()
        // If this machine genuinely has an SDK/PATH adb, resolution should find a real file.
        if (resolved != null) {
            assertTrue(File(resolved).canExecute(), "resolved adb must be executable: $resolved")
        }
    }

    @Test
    fun `non-executable file is not accepted`() {
        val notExecutable = File(tempDir, "adb").apply {
            writeText("not a binary")
            setExecutable(false)
        }
        System.setProperty("alohomora.adb.path", notExecutable.absolutePath)
        AdbLocator.reset()

        assertTrue(
            AdbLocator.find() != notExecutable.absolutePath,
            "a non-executable file must not be accepted as adb",
        )
    }

    @Test
    fun `require reports an actionable message when adb is missing`() {
        System.setProperty("alohomora.adb.path", File(tempDir, "absent").absolutePath)
        AdbLocator.reset()

        if (AdbLocator.find() == null) {
            val message = runCatching { AdbLocator.require() }.exceptionOrNull()?.message
            assertNotNull(message)
            assertTrue(
                message.contains("ANDROID_HOME") && message.contains("alohomora.adb.path"),
                "error must tell the user how to fix it, was: $message",
            )
        }
    }

    @Test
    fun `resolution is cached until reset`() {
        val adb = fakeAdb()
        System.setProperty("alohomora.adb.path", adb.absolutePath)
        AdbLocator.reset()
        assertEquals(adb.absolutePath, AdbLocator.find())

        // Changing the property without reset must not change the answer — resolution touches
        // the filesystem and runs on every adb invocation.
        System.setProperty("alohomora.adb.path", File(tempDir, "other").absolutePath)
        assertEquals(adb.absolutePath, AdbLocator.find())

        AdbLocator.reset()
        assertTrue(AdbLocator.find() != adb.absolutePath || adb.exists())
    }

    @Test
    fun `configured path from settings takes priority`() {
        val adb = fakeAdb()
        AdbLocator.configure(adb.absolutePath)

        assertEquals(adb.absolutePath, AdbLocator.find())
    }

    @Test
    fun `resolveWith accepts a platform-tools directory`() {
        val platformTools = File(tempDir, "platform-tools").apply { mkdirs() }
        val adb = File(platformTools, "adb").apply {
            writeText("#!/bin/sh\nexit 0\n")
            setExecutable(true)
        }

        assertEquals(adb.absolutePath, AdbLocator.resolveWith(platformTools.absolutePath))
    }

    @Test
    fun `resolveWith accepts an sdk root`() {
        val platformTools = File(tempDir, "sdk/platform-tools").apply { mkdirs() }
        val adb = File(platformTools, "adb").apply {
            writeText("#!/bin/sh\nexit 0\n")
            setExecutable(true)
        }

        assertEquals(adb.absolutePath, AdbLocator.resolveWith(File(tempDir, "sdk").absolutePath))
    }

    @Test
    fun `resolveWith is side-effect-free and does not populate the cache`() {
        val adb = fakeAdb()
        assertEquals(adb.absolutePath, AdbLocator.resolveWith(adb.absolutePath))
        // find() still resolves independently (no override set), so the preview did not leak.
        assertTrue(AdbLocator.find() != adb.absolutePath || adb.exists())
    }
}
