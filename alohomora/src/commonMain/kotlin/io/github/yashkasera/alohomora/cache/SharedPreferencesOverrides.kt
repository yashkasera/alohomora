package io.github.yashkasera.alohomora.cache

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

internal object SharedPreferencesOverrides {
    private val lock = ReentrantLock()
    private val registered = LinkedHashMap<String, () -> Map<String, Any?>>()

    fun register(name: String, reader: () -> Map<String, Any?>) = lock.withLock {
        registered[name] = reader
    }

    fun unregister(name: String) = lock.withLock {
        registered.remove(name)
    }

    fun clear() = lock.withLock {
        registered.clear()
    }

    fun snapshot(): Map<String, () -> Map<String, Any?>> = lock.withLock {
        registered.toMap()
    }
}
