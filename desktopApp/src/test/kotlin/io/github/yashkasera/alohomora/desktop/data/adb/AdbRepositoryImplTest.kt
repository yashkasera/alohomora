package io.github.yashkasera.alohomora.desktop.data.adb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Regression tests for multi-device port forwarding.
 *
 * The seam these use — `AdbRepositoryImpl(dataSource = …)` — already existed and was never
 * exercised. A single test here would have caught the bug where opening a window for device B
 * tore down device A's tunnel.
 */
class AdbRepositoryImplTest {

    /** Records forward/remove calls so tests can assert on tunnel lifecycle. */
    private class FakeAdbDataSource(
        private val devices: List<AdbDevice>,
    ) : AdbDataSource {
        val forwards = mutableListOf<Pair<String, Int>>()
        val removals = mutableListOf<Pair<String, Int>>()

        override suspend fun listDevices(): List<AdbDevice> = devices

        override suspend fun forwardDevToolsPort(deviceId: String, hostPort: Int, devicePort: Int) {
            forwards += deviceId to hostPort
        }

        override suspend fun removeForward(deviceId: String, hostPort: Int) {
            removals += deviceId to hostPort
        }

        override suspend fun enableTcpMode(deviceId: String, tcpPort: Int) = Unit
        override suspend fun connect(host: String, port: Int) = AdbCommandResult(0, "", "")
        override suspend fun disconnect(host: String, port: Int) = AdbCommandResult(0, "", "")
        override suspend fun restartServer() = AdbCommandResult(0, "", "")
        override suspend fun runCommand(deviceId: String?, args: List<String>) =
            AdbCommandResult(0, "", "")

        override suspend fun runDetached(deviceId: String?, args: List<String>): String? = null
    }

    private fun device(id: String) = AdbDevice(
        id = id,
        state = AdbDeviceState.DEVICE,
        model = null,
        product = null,
        transportId = null,
    )

    private suspend fun repoWith(fake: FakeAdbDataSource): AdbRepositoryImpl {
        // iosDataSource = null so these tests never touch the real usbmuxd socket or shell out
        // to simctl; discovery for iOS is covered separately.
        val repo = AdbRepositoryImpl(fake, iosDataSource = null)
        // activateDevice validates against the cached device list, so prime it.
        repo.refreshDevices()
        while (repo.devices.value.isEmpty()) {
            kotlinx.coroutines.yield()
        }
        return repo
    }

    @Test
    fun `activating a second device does not remove the first device's forward`() = runTest {
        val fake = FakeAdbDataSource(listOf(device("deviceA"), device("deviceB")))
        val repo = repoWith(fake)

        assertNull(repo.activateDevice("deviceA", hostPort = 53999, devicePort = 53999))
        assertNull(repo.activateDevice("deviceB", hostPort = 54000, devicePort = 53999))

        assertEquals(listOf("deviceA" to 53999, "deviceB" to 54000), fake.forwards)
        assertTrue(
            fake.removals.isEmpty(),
            "activating device B must not tear down device A's tunnel, but removed ${fake.removals}",
        )
    }

    @Test
    fun `deactivating one window leaves the other window's forward intact`() = runTest {
        val fake = FakeAdbDataSource(listOf(device("deviceA"), device("deviceB")))
        val repo = repoWith(fake)

        repo.activateDevice("deviceA", hostPort = 53999, devicePort = 53999)
        repo.activateDevice("deviceB", hostPort = 54000, devicePort = 53999)

        // Window A closes. It must remove only its own forward — resolving the device from the
        // global "selected device" (now B) is what used to kill B's tunnel instead.
        assertNull(repo.deactivateDevice("deviceA", hostPort = 53999))

        assertEquals(listOf("deviceA" to 53999), fake.removals)
    }

    @Test
    fun `deactivate falls back to the device owning the host port`() = runTest {
        val fake = FakeAdbDataSource(listOf(device("deviceA")))
        val repo = repoWith(fake)
        repo.activateDevice("deviceA", hostPort = 53999, devicePort = 53999)

        assertNull(repo.deactivateDevice(deviceId = null, hostPort = 53999))

        assertEquals(listOf("deviceA" to 53999), fake.removals)
    }

    @Test
    fun `reactivating the same device on a new host port replaces only its own forward`() =
        runTest {
            val fake = FakeAdbDataSource(listOf(device("deviceA")))
            val repo = repoWith(fake)

            repo.activateDevice("deviceA", hostPort = 53999, devicePort = 53999)
            repo.activateDevice("deviceA", hostPort = 54001, devicePort = 53999)

            assertEquals(listOf("deviceA" to 53999), fake.removals)
            assertEquals(listOf("deviceA" to 53999, "deviceA" to 54001), fake.forwards)
        }

    @Test
    fun `activating an unknown device reports an error and forwards nothing`() = runTest {
        val fake = FakeAdbDataSource(listOf(device("deviceA")))
        val repo = repoWith(fake)

        val error = repo.activateDevice("ghost", hostPort = 53999, devicePort = 53999)

        assertEquals("Device not found", error)
        assertTrue(fake.forwards.isEmpty())
    }
}
