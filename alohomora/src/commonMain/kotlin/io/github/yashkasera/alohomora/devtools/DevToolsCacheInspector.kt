package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.CacheStoreSnapshot

internal interface DevToolsCacheInspector {
    suspend fun getAllKeys(): List<String>
    suspend fun getValue(key: String): String?
    suspend fun getStores(): List<CacheStoreSnapshot>
    suspend fun refreshStores(): List<CacheStoreSnapshot>
    suspend fun updateValue(storeName: String, key: String, newValue: String?, type: String): Boolean
    suspend fun deleteValue(storeName: String, key: String): Boolean
}
