package io.github.yashkasera.alohomora.plugin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement

/**
 * No-op implementation of CustomValueStore for release builds.
 *
 * All methods are empty and return default values.
 */
object CustomValueStore {

    @Suppress("UNUSED_PARAMETER")
    fun putString(key: String, value: String) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putInt(key: String, value: Int) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putLong(key: String, value: Long) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putBoolean(key: String, value: Boolean) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putFloat(key: String, value: Float) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putDouble(key: String, value: Double) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putJson(key: String, value: JsonElement) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun <reified T> putObject(key: String, value: T) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun getString(key: String, defaultValue: String? = null): String? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getInt(key: String, defaultValue: Int? = null): Int? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getLong(key: String, defaultValue: Long? = null): Long? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getFloat(key: String, defaultValue: Float? = null): Float? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getDouble(key: String, defaultValue: Double? = null): Double? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getJson(key: String): JsonElement? {
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun <reified T> getObject(key: String): T? {
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    fun contains(key: String): Boolean {
        return false
    }

    @Suppress("UNUSED_PARAMETER")
    fun remove(key: String): Boolean {
        return false
    }

    fun clear() {
        // No-op
    }

    fun getAllKeys(): Set<String> {
        return emptySet()
    }

    fun getAllEntries(): Map<String, CustomValue> {
        return emptyMap()
    }

    @Suppress("UNUSED_PARAMETER")
    fun observe(key: String): StateFlow<CustomValue?> {
        return MutableStateFlow(null)
    }

    @Suppress("UNUSED_PARAMETER")
    fun observeString(key: String): StateFlow<String?> {
        return MutableStateFlow(null)
    }

    @Suppress("UNUSED_PARAMETER")
    fun observeInt(key: String): StateFlow<Int?> {
        return MutableStateFlow(null)
    }

    @Suppress("UNUSED_PARAMETER")
    fun observeBoolean(key: String): StateFlow<Boolean?> {
        return MutableStateFlow(null)
    }
}

/**
 * No-op sealed class for CustomValue.
 */
sealed class CustomValue {
    abstract val timestamp: Long

    data class StringValue(
        val value: String,
        override val timestamp: Long = 0L
    ) : CustomValue()

    data class IntValue(
        val value: Int,
        override val timestamp: Long = 0L
    ) : CustomValue()

    data class LongValue(
        val value: Long,
        override val timestamp: Long = 0L
    ) : CustomValue()

    data class BooleanValue(
        val value: Boolean,
        override val timestamp: Long = 0L
    ) : CustomValue()

    data class FloatValue(
        val value: Float,
        override val timestamp: Long = 0L
    ) : CustomValue()

    data class DoubleValue(
        val value: Double,
        override val timestamp: Long = 0L
    ) : CustomValue()

    data class JsonValue(
        val value: JsonElement,
        override val timestamp: Long = 0L
    ) : CustomValue()

    fun asString(): String = ""
    fun getTypeName(): String = ""
}
