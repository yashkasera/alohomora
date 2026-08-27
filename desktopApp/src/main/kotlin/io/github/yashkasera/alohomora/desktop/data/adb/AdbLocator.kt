package io.github.yashkasera.alohomora.desktop.data.adb

import java.io.File

/**
 * Resolves the `adb` executable on the host machine.
 *
 * Every ADB call used to be `ProcessBuilder("adb", …)`, relying on the bare `PATH`. A GUI app
 * launched from Finder (or the Windows Start menu) does **not** inherit the shell's `PATH`, so
 * in the packaged `.dmg`/`.msi`/`.deb` — the only build users actually install — every ADB
 * feature failed with no usable diagnostic. Developers never saw it because `./gradlew run`
 * inherits the terminal environment.
 *
 * `adb` is deliberately *not* bundled: `platform-tools` redistribution carries licence
 * obligations, and a pinned copy fights the user's already-running adb server (mismatched
 * client/server versions trigger `adb kill-server` churn across every tool on the machine).
 *
 * Resolution order, first hit wins:
 *  1. User-configured path from Settings ([configure])
 *  2. `alohomora.adb.path` system property / `ALOHOMORA_ADB_PATH` env var (explicit override)
 *  3. `ANDROID_HOME` / `ANDROID_SDK_ROOT` + `platform-tools/adb`
 *  4. Conventional SDK locations for the current OS
 *  5. `PATH`
 *
 * A configured/override value may point at the `adb` binary, its `platform-tools` directory, or
 * an SDK root — all three are tried.
 */
internal object AdbLocator {

    private const val OVERRIDE_PROPERTY = "alohomora.adb.path"
    private const val OVERRIDE_ENV = "ALOHOMORA_ADB_PATH"

    /** Cached because resolution touches the filesystem and is called per ADB invocation. */
    @Volatile
    private var cached: String? = null

    /** User-configured override from Settings; highest priority. Null/blank means "not set". */
    @Volatile
    private var configuredPath: String? = null

    /** Sets (or clears) the configured path and drops the cache so the next call re-resolves. */
    fun configure(path: String?) {
        configuredPath = path?.trim()?.takeIf { it.isNotEmpty() }
        reset()
    }

    /** Absolute path to `adb`, or null when it cannot be found. */
    fun find(): String? {
        cached?.let { return it }
        val resolved = resolve(configuredPath)
        cached = resolved
        return resolved
    }

    /**
     * Resolves against [configured] without touching the cache or the stored override — lets the
     * Settings UI preview whether a path the user is typing yields a working `adb`.
     */
    fun resolveWith(configured: String?): String? =
        resolve(configured?.trim()?.takeIf { it.isNotEmpty() })

    private fun resolve(configured: String?): String? =
        candidates(configured).firstOrNull { it.isExecutableFile() }?.absolutePath ?: findOnPath()

    /**
     * Absolute path to `adb`, or throws with a message the user can act on.
     *
     * Prefer this over silently degrading: "adb not found, set ANDROID_HOME" is actionable,
     * whereas the old behaviour surfaced as every device list being mysteriously empty.
     */
    fun require(): String = find() ?: error(
        "adb not found. Set a custom path in Settings → ADB, install Android platform-tools and " +
            "set ANDROID_HOME, or pass -D$OVERRIDE_PROPERTY=/path/to/adb (env: $OVERRIDE_ENV).",
    )

    /** Clears the cache. For tests, and for re-resolving after the user configures the SDK. */
    fun reset() {
        cached = null
    }

    private fun candidates(configured: String?): List<File> {
        val explicit = configured
            ?: System.getProperty(OVERRIDE_PROPERTY)
            ?: System.getenv(OVERRIDE_ENV)
        val sdkRoots = listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
        ) + conventionalSdkRoots()

        return buildList {
            explicit?.takeIf { it.isNotBlank() }?.let { path ->
                add(File(path))
                add(File(path, executableName))
                add(File(path, "platform-tools/$executableName"))
            }
            sdkRoots.forEach { root -> add(File(root, "platform-tools/$executableName")) }
        }
    }

    private fun conventionalSdkRoots(): List<String> {
        val home = System.getProperty("user.home") ?: return emptyList()
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            os.contains("mac") -> listOf("$home/Library/Android/sdk")
            os.contains("win") -> listOfNotNull(
                System.getenv("LOCALAPPDATA")?.let { "$it\\Android\\Sdk" },
                "$home\\AppData\\Local\\Android\\Sdk",
            )

            else -> listOf("$home/Android/Sdk", "$home/android-sdk", "/usr/lib/android-sdk")
        }
    }

    private fun findOnPath(): String? {
        val path = System.getenv("PATH") ?: return null
        return path.split(File.pathSeparatorChar)
            .asSequence()
            .filter { it.isNotBlank() }
            .map { File(it, executableName) }
            .firstOrNull { it.isExecutableFile() }
            ?.absolutePath
    }

    private val executableName: String
        get() = if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) {
            "adb.exe"
        } else {
            "adb"
        }

    private fun File.isExecutableFile(): Boolean = isFile && canExecute()
}
