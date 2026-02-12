package io.github.yashkasera.alohomora.desktop.domain.model

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)
