package io.github.yashkasera.alohomora.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.github.yashkasera.alohomora.cache.SharedPreferencesOverrides
import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheSource
import io.github.yashkasera.alohomora.domain.model.CacheStore
import io.github.yashkasera.alohomora.domain.model.CacheType
import io.github.yashkasera.alohomora.domain.repository.CacheRepository
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class CacheRepositoryImpl(
    private val context: Context,
) : CacheRepository {

    private val _preferencesFlow = MutableStateFlow<List<CacheEntry>>(emptyList())
    private var cachedPreferences: List<CacheEntry> = emptyList()

    override fun observeAllPreferences(): Flow<List<CacheEntry>> = _preferencesFlow.asStateFlow()

    override suspend fun getAllPreferences(): List<CacheEntry> {
        if (cachedPreferences.isEmpty()) {
            cachedPreferences = scanAllPreferences()
            _preferencesFlow.value = cachedPreferences
        }
        return cachedPreferences
    }

    override suspend fun refresh(): List<CacheEntry> {
        cachedPreferences = scanAllPreferences()
        _preferencesFlow.value = cachedPreferences
        return cachedPreferences
    }

    override suspend fun getStores(): List<CacheStore> = withContext(Dispatchers.IO) {
        val stores = mutableListOf<CacheStore>()
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val overrides = SharedPreferencesOverrides.snapshot()

        if (prefsDir.exists() && prefsDir.isDirectory) {
            prefsDir.listFiles { file -> file.extension == "xml" }?.forEach { file ->
                val name = file.nameWithoutExtension
                val override = overrides[name]
                val isEncrypted = override != null || isLikelyEncrypted(name)
                val source = if (isEncrypted) {
                    CacheSource.ENCRYPTED_SHARED_PREFERENCES
                } else {
                    CacheSource.SHARED_PREFERENCES
                }

                val count = try {
                    if (override != null) {
                        override().filterNot { isKeysetMetadata(it.key) }.size
                    } else {
                        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                        prefs.all.filterNot { isKeysetMetadata(it.key) }.size
                    }
                } catch (_: Exception) {
                    0
                }

                stores.add(CacheStore(name, source, isEncrypted, count))
            }
        }

        stores.sortedBy { it.name }
    }

    override fun getTotalSize(entries: List<CacheEntry>): String {
        val totalBytes = entries.sumOf { entry ->
            entry.key.encodeToByteArray().size + entry.value.encodeToByteArray().size
        }

        return when {
            totalBytes < 1024 -> "$totalBytes B"
            totalBytes < 1024 * 1024 -> "${totalBytes / 1024} KB"
            else -> String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0))
        }
    }

    override suspend fun updateEntry(
        storeName: String,
        key: String,
        value: String?,
        type: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = resolvePrefs(storeName) ?: return@withContext false
            val editor = prefs.edit()
            when (type) {
                "STRING" -> editor.putString(key, value)
                "INT" -> editor.putInt(key, value!!.toInt())
                "BOOLEAN" -> editor.putBoolean(key, value!!.toBoolean())
                "LONG" -> editor.putLong(key, value!!.toLong())
                "FLOAT" -> editor.putFloat(key, value!!.toFloat())
                else -> return@withContext false
            }
            val success = editor.commit()
            if (success) invalidateCache()
            success
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteEntry(storeName: String, key: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val prefs = resolvePrefs(storeName) ?: return@withContext false
                val success = prefs.edit().remove(key).commit()
                if (success) invalidateCache()
                success
            } catch (_: Exception) {
                false
            }
        }

    private fun resolvePrefs(storeName: String): SharedPreferences? {
        val override = SharedPreferencesOverrides.snapshot()[storeName]
        return if (override != null) {
            // For registered stores, we need the actual SharedPreferences instance
            // to write. The override only provides a reader lambda — writing requires
            // the real instance, which getSharedPreferences returns for encrypted
            // stores when the consumer has already opened them (same backing file).
            context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
        } else {
            context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
        }
    }

    private fun invalidateCache() {
        cachedPreferences = emptyList()
    }

    private suspend fun scanAllPreferences(): List<CacheEntry> = withContext(Dispatchers.IO) {
        val allEntries = mutableListOf<CacheEntry>()
        allEntries.addAll(scanSharedPreferences())
        allEntries.sortedBy { it.key }
    }

    private fun scanSharedPreferences(): List<CacheEntry> {
        val entries = mutableListOf<CacheEntry>()
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")

        if (!prefsDir.exists() || !prefsDir.isDirectory) {
            return entries
        }

        val xmlFiles = prefsDir.listFiles { file -> file.extension == "xml" } ?: return entries
        val overrides = SharedPreferencesOverrides.snapshot()

        for (file in xmlFiles) {
            val storeName = file.nameWithoutExtension
            val override = overrides[storeName]
            val isEncrypted = override != null || isLikelyEncrypted(storeName)

            try {
                val all: Map<String, Any?> = if (override != null) {
                    override()
                } else {
                    context.getSharedPreferences(storeName, Context.MODE_PRIVATE).all
                }

                for ((key, value) in all) {
                    if (isKeysetMetadata(key)) continue

                    val stringValue = when (value) {
                        is Set<*> -> value.joinToString(prefix = "[", postfix = "]")
                        null -> "null"
                        else -> value.toString()
                    }

                    entries.add(
                        CacheEntry(
                            key = key,
                            value = stringValue,
                            type = detectType(value),
                            source = if (isEncrypted) {
                                CacheSource.ENCRYPTED_SHARED_PREFERENCES
                            } else {
                                CacheSource.SHARED_PREFERENCES
                            },
                            isEncrypted = isEncrypted,
                            storeName = storeName,
                        ),
                    )
                }
            } catch (e: Exception) {
                entries.add(
                    CacheEntry(
                        key = "[$storeName]",
                        value = "[encrypted store - ${e.message ?: "unreadable"}]",
                        type = CacheType.STRING,
                        source = CacheSource.ENCRYPTED_SHARED_PREFERENCES,
                        isEncrypted = true,
                        storeName = storeName,
                    ),
                )
            }
        }

        return entries
    }

    private fun detectType(value: Any?): CacheType = when (value) {
        is Boolean -> CacheType.BOOLEAN
        is Int -> CacheType.INT
        is Long -> CacheType.LONG
        is Float -> CacheType.FLOAT
        is String -> {
            val trimmed = value.trim()
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))
            ) {
                CacheType.JSON
            } else {
                CacheType.STRING
            }
        }
        is Set<*> -> CacheType.STRING_SET
        else -> CacheType.UNKNOWN
    }

    private fun isLikelyEncrypted(name: String): Boolean {
        val encryptedIndicators = listOf(
            "encrypted",
            "secure",
            "secret",
            "crypto",
            "vault",
            "master_key",
            "enc_",
            "_enc",
        )
        return encryptedIndicators.any { name.lowercase().contains(it) }
    }

    private fun isKeysetMetadata(key: String): Boolean =
        key.startsWith("__androidx_security_crypto_encrypted_prefs_")
}
