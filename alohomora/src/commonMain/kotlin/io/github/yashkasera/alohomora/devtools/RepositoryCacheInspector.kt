package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.CacheEntrySnapshot
import io.github.yashkasera.alohomora.common.CacheStoreSnapshot
import io.github.yashkasera.alohomora.domain.model.CacheType
import io.github.yashkasera.alohomora.domain.repository.CacheRepository

internal class RepositoryCacheInspector(
    private val repository: CacheRepository,
) : DevToolsCacheInspector {

    override suspend fun getAllKeys(): List<String> =
        repository.refresh().map { it.key }.distinct()

    override suspend fun getValue(key: String): String? =
        repository.getAllPreferences().firstOrNull { it.key == key }?.value

    override suspend fun getStores(): List<CacheStoreSnapshot> =
        buildStoreSnapshots()

    override suspend fun refreshStores(): List<CacheStoreSnapshot> {
        repository.refresh()
        return buildStoreSnapshots()
    }

    override suspend fun updateValue(
        storeName: String,
        key: String,
        newValue: String?,
        type: String,
    ): Boolean {
        val success = repository.updateEntry(storeName, key, newValue, type)
        if (success) repository.refresh()
        return success
    }

    override suspend fun deleteValue(storeName: String, key: String): Boolean {
        val success = repository.deleteEntry(storeName, key)
        if (success) repository.refresh()
        return success
    }

    private suspend fun buildStoreSnapshots(): List<CacheStoreSnapshot> {
        val entries = repository.getAllPreferences()
        return entries
            .groupBy { it.storeName ?: "default" }
            .map { (storeName, storeEntries) ->
                CacheStoreSnapshot(
                    name = storeName,
                    isEncrypted = storeEntries.any { it.isEncrypted },
                    entries = storeEntries.map { entry ->
                        CacheEntrySnapshot(
                            key = entry.key,
                            value = entry.value,
                            type = entry.type.toWireType(),
                        )
                    },
                )
            }
    }
}

private fun CacheType.toWireType(): String = when (this) {
    CacheType.STRING -> "STRING"
    CacheType.BOOLEAN -> "BOOLEAN"
    CacheType.INT -> "INT"
    CacheType.LONG -> "LONG"
    CacheType.FLOAT -> "FLOAT"
    CacheType.STRING_SET -> "STRING_SET"
    CacheType.JSON -> "STRING"
    CacheType.UNKNOWN -> "STRING"
}
