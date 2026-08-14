package io.github.yashkasera.alohomora.desktop.data.logcat

import io.github.yashkasera.alohomora.desktop.data.adb.AdbLocator
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class LogcatStreamDataSource {
    fun streamThreadtime(deviceId: String): Flow<String> = callbackFlow {
        // Resolved via AdbLocator, not the bare PATH: a packaged app launched from Finder has
        // no shell PATH, so `ProcessBuilder("adb", …)` silently failed in every installed build.
        val adb = AdbLocator.find()
        if (adb == null) {
            close(IllegalStateException("adb not found; set ANDROID_HOME or -Dalohomora.adb.path"))
            return@callbackFlow
        }

        val process = ProcessBuilder(adb, "-s", deviceId, "logcat", "-v", "threadtime")
            .redirectErrorStream(true)
            .start()

        val readJob = launch(Dispatchers.IO) {
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line ->
                    trySend(line)
                }
            }
        }

        awaitClose {
            readJob.cancel()
            // destroy() is SIGTERM and the blocking readLine above will not observe
            // cancellation until the next line arrives — which on a quiet device may be never.
            // Escalate so the process and its pipe are actually released.
            process.destroy()
            if (!process.waitFor(DESTROY_GRACE_MILLIS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
            }
        }
    }

    private companion object {
        const val DESTROY_GRACE_MILLIS = 500L
    }
}
