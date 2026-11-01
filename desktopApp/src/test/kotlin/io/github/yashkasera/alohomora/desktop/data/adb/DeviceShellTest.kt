package io.github.yashkasera.alohomora.desktop.data.adb

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The device's `sh` re-parses everything after `adb shell`, so the only proof that a quoted value
 * survives is to hand it to a POSIX shell and read back what it saw. Host `/bin/sh` and the device's
 * mksh agree on single-quote rules; the round-trip is skipped where there is no `/bin/sh`.
 */
class DeviceShellTest {

    private val nasty = listOf(
        "myapp://open?a=1&b=2&c=3",
        "myapp://open?x=1;reboot",
        "myapp://o'neil?name=it's",
        "myapp://search?q=hello world",
        "https://a.example/p?a=\$(reboot)&b=`id`|sh>out",
        "'",
        "",
    )

    @Test
    fun `quote wraps in single quotes and escapes embedded quotes`() {
        assertEquals("'a&b'", DeviceShell.quote("a&b"))
        assertEquals("'it'\\''s'", DeviceShell.quote("it's"))
        assertEquals("''", DeviceShell.quote(""))
    }

    @Test
    fun `a quoted value reaches the shell as one literal word`() {
        if (!File("/bin/sh").canExecute()) return
        nasty.forEach { value ->
            // printf prints one line per word, so a split or an executed fragment shows up as a diff.
            assertEquals("[$value]\n", sh("printf '[%s]\\n' ${DeviceShell.quote(value)}"), value)
        }
    }

    @Test
    fun `view intent args quote only the url`() {
        val args = DeviceShell.viewIntentArgs("myapp://open?a=1&b=2")
        assertEquals(
            listOf("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "'myapp://open?a=1&b=2'"),
            args,
        )
    }

    private fun sh(script: String): String {
        val process = ProcessBuilder("/bin/sh", "-c", script).redirectErrorStream(true).start()
        process.waitFor(10, TimeUnit.SECONDS)
        return process.inputStream.bufferedReader().readText()
    }
}
