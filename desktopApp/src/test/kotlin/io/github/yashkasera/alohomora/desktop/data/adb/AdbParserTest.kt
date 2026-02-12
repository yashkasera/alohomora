package io.github.yashkasera.alohomora.desktop.data.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdbParserTest {
    @Test
    fun parsesDevicesWithMetadata() {
        val output = """
            List of devices attached
            emulator-5554 device product:sdk_gphone model:sdk_gphone64_x86_64 transport_id:1
            0123456789ABCDEF device product:pixel model:Pixel_7 transport_id:2
        """.trimIndent()

        val devices = AdbParser.parseDevices(output)
        assertEquals(2, devices.size)
        assertEquals("emulator-5554", devices[0].id)
        assertEquals(AdbDeviceState.DEVICE, devices[0].state)
        assertEquals("sdk_gphone64_x86_64", devices[0].model)
        assertEquals("sdk_gphone", devices[0].product)
        assertEquals("1", devices[0].transportId)
    }

    @Test
    fun parsesUnauthorizedAndOfflineDevices() {
        val output = """
            List of devices attached
            1111111111111111 unauthorized
            2222222222222222 offline
        """.trimIndent()

        val devices = AdbParser.parseDevices(output)
        assertEquals(2, devices.size)
        assertEquals(AdbDeviceState.UNAUTHORIZED, devices[0].state)
        assertEquals(AdbDeviceState.OFFLINE, devices[1].state)
    }

    @Test
    fun listDevicesThrowsOnAdbFailure() {
        val service = AdbServiceImpl(FakeRunner(exitCode = 1, stdout = "", stderr = "adb missing"))
        val error = assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking { service.listDevices() }
        }
        assertEquals("adb missing", error.message)
    }
}

private class FakeRunner(
    private val exitCode: Int,
    private val stdout: String,
    private val stderr: String,
) : AdbCommandRunner {
    override fun run(args: List<String>): AdbCommandResult {
        return AdbCommandResult(exitCode, stdout, stderr)
    }
}
