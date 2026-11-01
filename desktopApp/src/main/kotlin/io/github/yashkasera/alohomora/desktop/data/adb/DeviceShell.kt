package io.github.yashkasera.alohomora.desktop.data.adb

/**
 * Quoting for values that end up in the *device's* shell.
 *
 * `adb shell a b c` does not deliver an argv to the device: adb joins the arguments with spaces and
 * the device's `sh` re-parses the result. A process-builder argument list is therefore no protection
 * on its own — an unquoted `&` backgrounds the rest of a deep link (every link with two query params
 * arrived cut at the first one), and a `;` runs whatever follows as a separate command.
 */
internal object DeviceShell {

    /**
     * Wraps [value] in single quotes, the one form in which `sh` interprets nothing; an embedded `'`
     * closes the quote, emits an escaped quote and reopens (`'` → `'\''`).
     */
    fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    /** `adb` args that open [url] through a VIEW intent, with the url quoted for the device shell. */
    fun viewIntentArgs(url: String): List<String> =
        listOf("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", quote(url))
}
