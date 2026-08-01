package io.github.yashkasera.alohomora.desktop.data.adb

internal interface AdbCommandRunner {
    /** Runs a command to completion, subject to a timeout. */
    fun run(args: List<String>): AdbCommandResult

    /**
     * Starts a command without waiting for it to finish.
     *
     * Required for `shell screenrecord`, which runs until it is signalled. Routing it through
     * [run] meant it never returned: originally that parked an IO thread with an undrained
     * pipe for the whole recording, and once [run] gained a timeout it would instead kill the
     * recording partway through.
     *
     * @return null on success, or an error message.
     */
    fun runDetached(args: List<String>): String?
}
