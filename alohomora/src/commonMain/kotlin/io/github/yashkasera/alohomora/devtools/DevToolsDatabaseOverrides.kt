package io.github.yashkasera.alohomora.devtools

internal data class DevToolsDatabaseOverridesSnapshot(
    val includes: Map<String, String?>,
    val excludes: Set<String>,
)

internal object DevToolsDatabaseOverrides {
    private val lock = Any()
    private val includes = LinkedHashMap<String, String?>()
    private val excludes = LinkedHashSet<String>()

    fun include(name: String, path: String?) = synchronized(lock) {
        includes[name] = path
        excludes.remove(name)
    }

    fun exclude(name: String) = synchronized(lock) {
        excludes.add(name)
        includes.remove(name)
    }

    fun clear() = synchronized(lock) {
        includes.clear()
        excludes.clear()
    }

    fun snapshot(): DevToolsDatabaseOverridesSnapshot = synchronized(lock) {
        DevToolsDatabaseOverridesSnapshot(
            includes = includes.toMap(),
            excludes = excludes.toSet(),
        )
    }
}
