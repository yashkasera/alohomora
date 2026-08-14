package io.github.yashkasera.alohomora.domain.model

internal data class CacheEntry(
    val key: String,
    val value: String,
    val type: CacheType,
    val source: CacheSource,
    val isEncrypted: Boolean = false,
    val storeName: String? = null,
)

internal enum class CacheType {
    STRING,
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    STRING_SET,
    JSON,
    UNKNOWN;

    fun displayLabel(): String = when (this) {
        STRING -> "TEXT"
        BOOLEAN -> "BOOL"
        INT -> "INT"
        LONG -> "LONG"
        FLOAT -> "FLOAT"
        STRING_SET -> "SET"
        JSON -> "JSON"
        UNKNOWN -> "?"
    }

    companion object {
        fun detect(value: String): CacheType {
            if (value.equals("true", ignoreCase = true) || value.equals(
                    "false",
                    ignoreCase = true,
                )
            ) {
                return BOOLEAN
            }

            val trimmed = value.trim()
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))
            ) {
                return JSON
            }

            if (value.matches(Regex("^-?\\d+$"))) {
                return try {
                    value.toInt()
                    INT
                } catch (_: NumberFormatException) {
                    try {
                        value.toLong()
                        LONG
                    } catch (_: NumberFormatException) {
                        STRING
                    }
                }
            }

            if (value.matches(Regex("^-?\\d+\\.\\d+$"))) {
                return FLOAT
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.contains(",")) {
                return STRING_SET
            }

            return STRING
        }
    }
}

internal enum class CacheSource {
    SHARED_PREFERENCES,
    ENCRYPTED_SHARED_PREFERENCES,
    DATASTORE,
    NS_USER_DEFAULTS,
}

internal data class CacheStore(
    val name: String,
    val source: CacheSource,
    val isEncrypted: Boolean,
    val entryCount: Int,
)
