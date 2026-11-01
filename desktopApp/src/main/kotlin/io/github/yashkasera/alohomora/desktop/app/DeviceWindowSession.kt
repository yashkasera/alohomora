package io.github.yashkasera.alohomora.desktop.app

import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

data class DeviceWindowSession(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val host: String,
    val hostPort: Int,
    val devicePort: Int,
    val composition: DesktopAppComposition,
    /**
     * Whether this session owns an `adb forward` on [hostPort]. Only Android sessions do; an iOS
     * session tunnels over usbmux or reaches the simulator directly, and running
     * `adb forward --remove` for one only fails and surfaces as an adb error in other windows.
     */
    val forwardsAdbPort: Boolean,
)

/** The launcher's notion of "local": a forward on this machine rather than `adb connect`. */
internal fun isLoopbackHost(host: String): Boolean = host == "127.0.0.1" || host == "localhost"

/**
 * Releases everything a session holds: its DevTools socket, its adb forward (plus the
 * `adb connect` for a remote host), and its [DesktopAppComposition] — a Ktor client and a dozen
 * view-model scopes.
 *
 * The single teardown for every way a session ends: the Close Window menu item, the title-bar
 * close button, app exit, and a launcher connection that is cancelled or fails. Only the menu item
 * used to do the adb half, so closing a window with its X, quitting, or abandoning a connection
 * left the forward behind — holding the host port, and after a restart pointing a reused port at
 * whichever device last claimed it.
 *
 * Suspends until adb has answered, because app exit must not tear the process down under it. The
 * adb steps go through the shared devices view model, whose callback never fires if its scope is
 * gone, so each is bounded and the composition is closed regardless.
 */
suspend fun DeviceWindowSession.release() {
    composition.devToolsViewModel.disconnect()
    try {
        if (forwardsAdbPort) {
            val devices = composition.devicesViewModel
            withTimeoutOrNull(ADB_TEARDOWN_TIMEOUT) {
                // A local session never ran `adb connect`; disconnecting 127.0.0.1:<hostPort>
                // would only fail and post an adb error to every other window.
                if (!isLoopbackHost(host)) {
                    suspendCancellableCoroutine { cont ->
                        devices.disconnectHost(host, hostPort) { cont.resume(Unit) }
                    }
                }
                suspendCancellableCoroutine { cont ->
                    devices.deactivateDevice(deviceId, hostPort) { cont.resume(Unit) }
                }
            }
        }
    } finally {
        composition.close()
    }
}

private val ADB_TEARDOWN_TIMEOUT = 5.seconds
