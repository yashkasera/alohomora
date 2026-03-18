package io.github.yashkasera.alohomora.devtools

import kotlinx.coroutines.sync.withLock

internal data class DevToolsDatabaseOverridesSnapshot(
    val includes: Map<String, String?>,
    val excludes: Set<String>,
)

internal object DevToolsDatabaseOverrides {
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private val includes = LinkedHashMap<String, String?>()
    private val excludes = LinkedHashSet<String>()

    fun include(name: String, path: String?) {
        withLock {
            includes[name] = path
            excludes.remove(name)
        }
    }

    fun exclude(name: String) {
        withLock {
            excludes.add(name)
            includes.remove(name)
        }
    }

    fun clear() {
        withLock {
            includes.clear()
            excludes.clear()
        }
    }

    fun snapshot(): DevToolsDatabaseOverridesSnapshot {
        return withLock {
            DevToolsDatabaseOverridesSnapshot(
                includes = includes.toMap(),
                excludes = excludes.toSet(),
            )
        }
    }

    private fun <T> withLock(block: () -> T): T {
        return kotlinx.coroutines.runBlocking {
            mutex.withLock { block() }
        }
    }
}
