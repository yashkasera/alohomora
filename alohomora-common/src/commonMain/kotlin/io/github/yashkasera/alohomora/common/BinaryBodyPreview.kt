package io.github.yashkasera.alohomora.common

import kotlin.io.encoding.Base64

/**
 * How every console describes a binary request body.
 *
 * Such a body has no text to show, and decoding it as text is exactly the corruption capture works
 * to avoid, so the consoles show what it is instead: its size, its type, and its first bytes in
 * hex. Shared so the mobile console, the desktop and curl export say the same thing.
 */
object BinaryBodyPreview {

    /** Leading bytes shown in hex. Enough to recognise a file signature or a protobuf tag. */
    const val HEX_PREVIEW_BYTES: Int = 32

    /** Shown beside a body whose Content-Type carries a multipart boundary. */
    const val BOUNDARY_WARNING: String =
        "Content-Type carries the multipart boundary this body was written with. Editing that " +
            "header breaks the body."

    /** Shown in place of a body editor: a binary body is replayed as captured or not at all. */
    const val READ_ONLY_NOTE: String = "Binary body. Replayed exactly as captured; it cannot be edited."

    /**
     * The number of bytes [base64] decodes to, worked out from its length so a 1 MB body is not
     * decoded on every recomposition just to print its size.
     */
    fun decodedSize(base64: String): Long {
        val padding = base64.takeLastWhile { it == '=' }.length
        return base64.length.toLong() / 4 * 3 - padding
    }

    /**
     * The first [maxBytes] of the body [base64] encodes, as space-separated hex, with an ellipsis
     * when there is more. Only the prefix is decoded.
     */
    fun hex(base64: String, maxBytes: Int = HEX_PREVIEW_BYTES): String {
        // Four base64 characters per three bytes; a prefix cut on that boundary decodes on its own.
        val prefixChars = (maxBytes + 2) / 3 * 4
        val bytes = runCatching { Base64.decode(base64.take(prefixChars)) }.getOrNull() ?: return ""
        val shown = bytes.take(maxBytes)
        val more = decodedSize(base64) > shown.size
        return shown.joinToString(" ") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
        } + if (more) " …" else ""
    }

    /** [bytes] as B, KB or MB with one decimal, the way both consoles print a body size. */
    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> tenths(bytes, 1024) + " KB"
        else -> tenths(bytes, 1024 * 1024) + " MB"
    }

    /** One line describing the body, then its leading bytes in hex. */
    fun describe(base64: String, contentType: String?): String = buildString {
        append("Binary body, ")
        append(formatSize(decodedSize(base64)))
        contentType?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
        val hex = hex(base64)
        if (hex.isNotEmpty()) append("\n\n").append(hex)
    }

    /** True when [contentType] carries a `boundary` parameter, as every multipart type does. */
    fun hasMultipartBoundary(contentType: String?): Boolean =
        contentType?.split(';')?.drop(1)?.any { it.trim().startsWith("boundary=", ignoreCase = true) } == true

    // No String.format in common code; integer tenths keep it exact and locale-free.
    private fun tenths(bytes: Long, unit: Long): String {
        val tenths = bytes * 10 / unit
        return "${tenths / 10}.${tenths % 10}"
    }
}
