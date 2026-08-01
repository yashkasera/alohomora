package io.github.yashkasera.alohomora.devtools

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

internal data class DevToolsDatabaseOverridesSnapshot(
    val includes: Map<String, String?>,
    val excludes: Set<String>,
)

internal object DevToolsDatabaseOverrides {
    private val lock = ReentrantLock()
    private val includes = LinkedHashMap<String, String?>()
    private val excludes = LinkedHashSet<String>()

    fun include(name: String, path: String?) = lock.withLock {
        includes[name] = path
        excludes.remove(name)
    }

    fun exclude(name: String) = lock.withLock {
        excludes.add(name)
        includes.remove(name)
    }

    fun clear() = lock.withLock {
        includes.clear()
        excludes.clear()
    }

    fun snapshot(): DevToolsDatabaseOverridesSnapshot = lock.withLock {
        DevToolsDatabaseOverridesSnapshot(
            includes = includes.toMap(),
            excludes = excludes.toSet(),
        )
    }
}
