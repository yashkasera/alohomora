package io.github.yashkasera.alohomora.traffic

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

internal class ThrottledResponseBody(
    private val delegate: ResponseBody,
    private val bytesPerSecond: Long,
) : ResponseBody() {

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource =
        ThrottledSource(delegate.source(), bytesPerSecond).buffer()
}

private class ThrottledSource(
    delegate: Source,
    bytesPerSecond: Long,
) : ForwardingSource(delegate) {

    private val chunkSize = (bytesPerSecond / 10).coerceAtLeast(1)
    private val sleepPerChunkMs = (1000L * chunkSize / bytesPerSecond).coerceAtLeast(1)

    override fun read(sink: Buffer, byteCount: Long): Long {
        val toRead = byteCount.coerceAtMost(chunkSize)
        val bytesRead = super.read(sink, toRead)
        if (bytesRead > 0) {
            Thread.sleep(sleepPerChunkMs)
        }
        return bytesRead
    }
}
