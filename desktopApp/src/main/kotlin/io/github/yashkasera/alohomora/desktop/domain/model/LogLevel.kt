package io.github.yashkasera.alohomora.desktop.domain.model

enum class LogLevel(val shortName: String) {
    VERBOSE("V"),
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E"),
    FATAL("F");

    companion object {
        fun fromShortName(value: String): LogLevel? = when (value) {
            "V" -> VERBOSE
            "D" -> DEBUG
            "I" -> INFO
            "W" -> WARN
            "E" -> ERROR
            "F" -> FATAL
            else -> null
        }
    }
}
