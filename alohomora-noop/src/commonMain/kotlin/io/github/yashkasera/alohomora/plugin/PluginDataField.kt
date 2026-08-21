package io.github.yashkasera.alohomora.plugin

@Suppress("unused")
fun interface PluginDataUpdateHandler {
    suspend fun onUpdate(newValue: String)
}

@Suppress("unused")
data class PluginDataField(
    val key: String,
    val label: String,
    val type: String = "string",
    val value: () -> String,
    val options: List<String>? = null,
    val readOnly: Boolean = false,
    val onUpdate: PluginDataUpdateHandler? = null,
)
