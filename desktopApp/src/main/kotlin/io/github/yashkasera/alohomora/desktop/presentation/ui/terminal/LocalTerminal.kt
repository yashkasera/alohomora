package io.github.yashkasera.alohomora.desktop.presentation.ui.terminal

import com.pty4j.PtyProcess
import com.pty4j.PtyProcessBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

class LocalTerminal {

    private val process: PtyProcess
    private val reader: BufferedReader
    private val writer: OutputStream

    init {
        val builder = PtyProcessBuilder(
            arrayOf("/bin/bash", "--noprofile", "--norc")
        ).setDirectory(System.getProperty("user.home"))

        process = builder.start()
        reader = BufferedReader(InputStreamReader(process.inputStream))
        writer = process.outputStream
    }

    fun write(input: String) {
        writer.write(input.toByteArray())
        writer.flush()
    }

    fun readLine(): String? {
        return reader.readLine()
    }

    fun isAlive(): Boolean = process.isAlive
}
