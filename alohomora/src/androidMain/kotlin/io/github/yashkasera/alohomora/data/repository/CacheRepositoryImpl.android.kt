package io.github.yashkasera.alohomora.data.repository

import android.content.Context
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

/**
 * Android implementation of CacheRepository.
 *
 * Auto-discovers and reads preferences from:
 * - SharedPreferences files (*.xml in shared_prefs/)
 * - EncryptedSharedPreferences (marked as encrypted if decryption fails)
 * - DataStore files (future enhancement)
 */
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

        if (prefsDir.exists() && prefsDir.isDirectory) {
            prefsDir.listFiles { file -> file.extension == "xml" }?.forEach { file ->
                val name = file.nameWithoutExtension
                val isEncrypted = isLikelyEncrypted(name)
                val source = if (isEncrypted) {
                    CacheSource.ENCRYPTED_SHARED_PREFERENCES
                } else {
                    CacheSource.SHARED_PREFERENCES
                }

                // Count entries by reading the file
                val count = try {
                    val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    prefs.all.size
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

    /**
     * Scans all preference sources and returns a combined list of entries.
     */
    private suspend fun scanAllPreferences(): List<CacheEntry> = withContext(Dispatchers.IO) {
        val allEntries = mutableListOf<CacheEntry>()

        // Scan SharedPreferences
        allEntries.addAll(scanSharedPreferences())

        // Note: DataStore reading requires androidx.datastore dependency
        // This is a placeholder for future implementation
        // allEntries.addAll(scanDataStore())

        allEntries.sortedBy { it.key }
    }

    /**
     * Scans all SharedPreferences files in the app's shared_prefs directory.
     */
    private fun scanSharedPreferences(): List<CacheEntry> {
        val entries = mutableListOf<CacheEntry>()
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")

        if (!prefsDir.exists() || !prefsDir.isDirectory) {
            return entries
        }

        val xmlFiles = prefsDir.listFiles { file -> file.extension == "xml" } ?: return entries

        for (file in xmlFiles) {
            val storeName = file.nameWithoutExtension
            val isEncrypted = isLikelyEncrypted(storeName)

            try {
                val prefs = context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
                val all = prefs.all

                for ((key, value) in all) {
                    val stringValue = when (value) {
                        is Set<*> -> value.joinToString(prefix = "[", postfix = "]")
                        null -> "null"
                        else -> value.toString()
                    }

                    // For encrypted stores, values are already decrypted by the system
                    // We just mark them as coming from an encrypted source
                    entries.add(
                        CacheEntry(
                            key = key,
                            value = stringValue,
                            type = CacheType.detect(stringValue),
                            source = if (isEncrypted) {
                                CacheSource.ENCRYPTED_SHARED_PREFERENCES
                            } else {
                                CacheSource.SHARED_PREFERENCES
                            },
                            isEncrypted = isEncrypted,
                            storeName = storeName,
                        )
                    )
                }
            } catch (e: Exception) {
                // Likely an encrypted preference file we can't read
                // Add a placeholder entry indicating the encrypted store
                entries.add(
                    CacheEntry(
                        key = "[$storeName]",
                        value = "[encrypted store - ${e.message ?: "unreadable"}]",
                        type = CacheType.STRING,
                        source = CacheSource.ENCRYPTED_SHARED_PREFERENCES,
                        isEncrypted = true,
                        storeName = storeName,
                    )
                )
            }
        }

        return entries
    }

    /**
     * Heuristic to determine if a preference store is likely encrypted.
     * Checks for common encrypted preference naming patterns.
     */
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
}
