package io.github.yashkasera.alohomora.desktop.domain.model

data class CacheState(
    val keys: List<String> = emptyList(),
    val values: Map<String, String?> = emptyMap(),
    val stores: List<CacheStoreState> = emptyList(),
)

data class CacheStoreState(
    val name: String,
    val isEncrypted: Boolean,
    val entries: List<CacheEntryState>,
)

data class CacheEntryState(
    val key: String,
    val value: String?,
    val type: String,
)
