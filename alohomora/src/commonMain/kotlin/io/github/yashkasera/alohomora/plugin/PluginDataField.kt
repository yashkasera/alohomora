package io.github.yashkasera.alohomora.plugin

fun interface PluginDataUpdateHandler {
    suspend fun onUpdate(newValue: String)
}

data class PluginDataField(
    val key: String,
    val label: String,
    val type: String = "string",
    val value: () -> String,
    val options: List<String>? = null,
    val readOnly: Boolean = false,
    val onUpdate: PluginDataUpdateHandler? = null,
)
