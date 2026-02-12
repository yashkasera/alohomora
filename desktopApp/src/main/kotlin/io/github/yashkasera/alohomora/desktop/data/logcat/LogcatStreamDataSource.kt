package io.github.yashkasera.alohomora.desktop.data.logcat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class LogcatStreamDataSource {
    fun streamThreadtime(deviceId: String): Flow<String> = callbackFlow {
        val process = ProcessBuilder("adb", "-s", deviceId, "logcat", "-v", "threadtime")
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
            process.destroy()
        }
    }
}
