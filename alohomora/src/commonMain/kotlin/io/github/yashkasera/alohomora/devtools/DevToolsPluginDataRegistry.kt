package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.PluginDataFieldDescriptor
import io.github.yashkasera.alohomora.common.PluginDataSnapshot
import io.github.yashkasera.alohomora.plugin.PluginDataField
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal object DevToolsPluginDataRegistry {

    private val lock = ReentrantLock()
    private val plugins = linkedMapOf<String, List<PluginDataField>>()

    private val _changes = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val changes: SharedFlow<String> = _changes.asSharedFlow()

    fun register(pluginId: String, fields: List<PluginDataField>) {
        lock.withLock { plugins[pluginId] = fields }
    }

    fun unregister(pluginId: String): Boolean = lock.withLock {
        plugins.remove(pluginId) != null
    }

    fun getSnapshots(): List<PluginDataSnapshot> = lock.withLock {
        plugins.map { (pluginId, fields) ->
            PluginDataSnapshot(pluginId, fields.map { it.toDescriptor(pluginId) })
        }
    }

    fun getSnapshot(pluginId: String): PluginDataSnapshot? = lock.withLock {
        plugins[pluginId]?.let { fields ->
            PluginDataSnapshot(pluginId, fields.map { it.toDescriptor(pluginId) })
        }
    }

    fun getField(pluginId: String, key: String): PluginDataField? = lock.withLock {
        plugins[pluginId]?.firstOrNull { it.key == key }
    }

    fun notifyChanged(pluginId: String) {
        _changes.tryEmit(pluginId)
    }

    private fun PluginDataField.toDescriptor(pluginId: String) = PluginDataFieldDescriptor(
        pluginId = pluginId,
        key = key,
        label = label,
        type = type,
        value = value(),
        options = options,
        readOnly = readOnly || onUpdate == null,
    )
}
