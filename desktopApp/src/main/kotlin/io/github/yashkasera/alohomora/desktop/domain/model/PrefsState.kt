package io.github.yashkasera.alohomora.desktop.domain.model

data class PrefsState(
    val keys: List<String> = emptyList(),
    val values: Map<String, String?> = emptyMap(),
)
