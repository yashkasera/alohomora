package io.github.yashkasera.alohomora.desktop.presentation.model

data class CustomCommandResult(
    val command: String,
    val output: String,
    val isError: Boolean,
)
