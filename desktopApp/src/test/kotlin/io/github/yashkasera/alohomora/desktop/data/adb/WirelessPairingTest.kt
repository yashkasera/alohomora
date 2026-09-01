package io.github.yashkasera.alohomora.desktop.data.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Wireless-debugging pairing/connect behaviour in the repository.
 *
 * Two gotchas are pinned here: `connectWireless` must NOT run `adb tcpip` (the device is already
 * in TCP mode, unlike a USB device flipped over the cable), and both `adb connect` and `adb pair`
 * can exit 0 while printing a failure line — so success is judged by output, not just exit code.
 */
class WirelessPairingTest {

    private class RecordingDataSource(
        var pairResult: AdbCommandResult = AdbCommandResult(0, "Successfully paired", ""),
        var connectResult: AdbCommandResult = AdbCommandResult(0, "connected to 10.0.0.9:5555", ""),
        var mdnsResult: AdbCommandResult = AdbCommandResult(0, "", ""),
    ) : AdbDataSource {
        val pairs = mutableListOf<Triple<String, Int, String>>()
        val connects = mutableListOf<Pair<String, Int>>()
        var tcpipCalls = 0

        override suspend fun listDevices(): List<AdbDevice> = emptyList()
        override suspend fun forwardDevToolsPort(deviceId: String, hostPort: Int, devicePort: Int) = Unit
        override suspend fun removeForward(deviceId: String, hostPort: Int) = Unit
        override suspend fun enableTcpMode(deviceId: String, tcpPort: Int) { tcpipCalls++ }
        override suspend fun connect(host: String, port: Int): AdbCommandResult {
            connects += host to port
            return connectResult
        }
        override suspend fun disconnect(host: String, port: Int) = AdbCommandResult(0, "", "")
        override suspend fun pair(host: String, port: Int, code: String): AdbCommandResult {
            pairs += Triple(host, port, code)
            return pairResult
        }
        override suspend fun listMdnsServices() = mdnsResult
        override suspend fun restartServer() = AdbCommandResult(0, "", "")
        override suspend fun runCommand(deviceId: String?, args: List<String>) = AdbCommandResult(0, "", "")
        override suspend fun runDetached(deviceId: String?, args: List<String>): String? = null
    }

    private fun repo(fake: RecordingDataSource) = AdbRepositoryImpl(fake, iosDataSource = null)

    @Test
    fun `pairDevice passes host port and code and succeeds`() = runTest {
        val fake = RecordingDataSource()
        val error = repo(fake).pairDevice("192.168.1.5", 37155, "123456")

        assertNull(error)
        assertEquals(Triple("192.168.1.5", 37155, "123456"), fake.pairs.single())
    }

    @Test
    fun `pairDevice surfaces the failure message`() = runTest {
        val fake = RecordingDataSource(pairResult = AdbCommandResult(1, "", "Failed: wrong code"))
        val error = repo(fake).pairDevice("192.168.1.5", 37155, "000000")

        assertEquals("Failed: wrong code", error)
    }

    @Test
    fun `connectWireless issues adb connect without enabling tcpip`() = runTest {
        val fake = RecordingDataSource()
        val error = repo(fake).connectWireless("10.0.0.9", 5555)

        assertNull(error)
        assertEquals("10.0.0.9" to 5555, fake.connects.single())
        assertEquals(0, fake.tcpipCalls)
    }

    @Test
    fun `connectWireless treats a zero-exit failure line as an error`() = runTest {
        // adb connect exits 0 even when it can't reach the device.
        val fake = RecordingDataSource(
            connectResult = AdbCommandResult(0, "failed to connect to '10.0.0.9:5555'", ""),
        )
        val error = repo(fake).connectWireless("10.0.0.9", 5555)

        assertNotNull(error)
        assertTrue(error.contains("failed", ignoreCase = true), "unexpected message: $error")
    }

    @Test
    fun `discoverWirelessEndpoints maps mdns output`() = runTest {
        val fake = RecordingDataSource(
            mdnsResult = AdbCommandResult(
                0,
                """
                    List of discovered mdns services
                    adb-x	_adb-tls-pairing._tcp.	192.168.1.5:37155
                    adb-x	_adb-tls-connect._tcp.	192.168.1.5:42159
                """.trimIndent(),
                "",
            ),
        )

        val discovery = repo(fake).discoverWirelessEndpoints()

        assertEquals("192.168.1.5:37155", discovery.pairing?.address)
        assertEquals("192.168.1.5:42159", discovery.connect?.address)
    }

    @Test
    fun `qr payload matches the adb wifi pairing format`() {
        assertEquals(
            "WIFI:T:ADB;S:alohomora-abc;P:secret123;;",
            WirelessQr.pairingPayload("alohomora-abc", "secret123"),
        )
    }

    @Test
    fun `findPairingEndpoint matches the pairing service by name`() = runTest {
        val fake = RecordingDataSource(
            mdnsResult = AdbCommandResult(
                0,
                """
                    List of discovered mdns services
                    other-device	_adb-tls-pairing._tcp.	192.168.1.9:11111
                    alohomora-xyz	_adb-tls-pairing._tcp.	192.168.1.5:37155
                    alohomora-xyz	_adb-tls-connect._tcp.	192.168.1.5:42159
                """.trimIndent(),
                "",
            ),
        )

        assertEquals("192.168.1.5:37155", repo(fake).findPairingEndpoint("alohomora-xyz")?.address)
    }

    @Test
    fun `findPairingEndpoint returns null when nothing matches`() = runTest {
        val fake = RecordingDataSource(
            mdnsResult = AdbCommandResult(0, "List of discovered mdns services", ""),
        )
        assertNull(repo(fake).findPairingEndpoint("alohomora-xyz"))
    }
}
