package io.github.yashkasera.alohomora.desktop.presentation.model

data class DeveloperOptionsState(
    val showTaps: Boolean? = null,
    val showLayoutBounds: Boolean? = null,
    val animationsDisabled: Boolean? = null,
    val darkMode: DarkModeOption? = null,
    val dontKeepActivities: Boolean? = null,
    val stayAwake: Boolean? = null,
    val fontScale: Float? = null,
    val isLoading: Boolean = false,
)

enum class DarkModeOption(val label: String) {
    YES("On"),
    NO("Off"),
    AUTO("Auto"),
}
