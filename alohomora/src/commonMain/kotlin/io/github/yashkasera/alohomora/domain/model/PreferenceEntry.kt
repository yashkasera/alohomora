package io.github.yashkasera.alohomora.domain.model

/**
 * Represents a single preference entry from any storage type.
 *
 * @property key The preference key
 * @property value The string representation of the preference value
 * @property type The detected type of the value
 * @property source The storage source (SharedPreferences, DataStore, etc.)
 * @property isEncrypted Whether this entry comes from an encrypted store
 * @property storeName The name of the specific store file (e.g., "user_prefs" for user_prefs.xml)
 */
data class PreferenceEntry(
    val key: String,
    val value: String,
    val type: PreferenceType,
    val source: PreferenceSource,
    val isEncrypted: Boolean = false,
    val storeName: String? = null,
)

/**
 * The data type of preference value.
 */
enum class PreferenceType {
    STRING,
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    STRING_SET,
    JSON,
    UNKNOWN;

    companion object {
        /**
         * Detects the type of value based on its content.
         */
        fun detect(value: String): PreferenceType {
            // Check for boolean
            if (value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)) {
                return BOOLEAN
            }

            // Check for JSON (starts with { or [ and ends with } or ])
            val trimmed = value.trim()
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))
            ) {
                return JSON
            }

            // Check for int (no decimal point, fits in Int range)
            if (value.matches(Regex("^-?\\d+$"))) {
                return try {
                    value.toInt()
                    INT
                } catch (_: NumberFormatException) {
                    // Try Long
                    try {
                        value.toLong()
                        LONG
                    } catch (_: NumberFormatException) {
                        STRING
                    }
                }
            }

            // Check for float
            if (value.matches(Regex("^-?\\d+\\.\\d+$"))) {
                return FLOAT
            }

            // Check for string set format [item1, item2]
            if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.contains(",")) {
                return STRING_SET
            }

            return STRING
        }
    }
}

/**
 * The storage source of a preference.
 */
enum class PreferenceSource {
    SHARED_PREFERENCES,
    ENCRYPTED_SHARED_PREFERENCES,
    DATASTORE,
    NS_USER_DEFAULTS,
}

/**
 * Information about a preference store (a collection of entries).
 *
 * @property name The display name of the store
 * @property source The type of storage
 * @property isEncrypted Whether the store is encrypted
 * @property entryCount Number of entries in the store
 */
data class PreferenceStore(
    val name: String,
    val source: PreferenceSource,
    val isEncrypted: Boolean,
    val entryCount: Int,
)
