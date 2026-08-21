package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.ActionDescriptor
import io.github.yashkasera.alohomora.common.ActionParameter
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

fun interface DevToolsActionHandler {
    suspend fun execute(params: Map<String, String>): Map<String, String>
}

internal object DevToolsActionRegistry {

    private val lock = ReentrantLock()
    private val actions = linkedMapOf<String, Pair<ActionDescriptor, DevToolsActionHandler>>()

    fun register(
        id: String,
        label: String,
        description: String? = null,
        parameters: List<ActionParameter> = emptyList(),
        handler: DevToolsActionHandler,
    ) {
        val descriptor = ActionDescriptor(id, label, description, parameters)
        lock.withLock { actions[id] = descriptor to handler }
    }

    fun unregister(id: String): Boolean = lock.withLock { actions.remove(id) != null }

    fun getDescriptors(): List<ActionDescriptor> = lock.withLock { actions.values.map { it.first } }

    suspend fun execute(id: String, params: Map<String, String>): Map<String, String> {
        val handler = lock.withLock { actions[id]?.second }
            ?: error("No action registered with id '$id'")
        return handler.execute(params)
    }
}
