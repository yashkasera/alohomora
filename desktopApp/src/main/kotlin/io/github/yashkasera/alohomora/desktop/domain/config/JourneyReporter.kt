package io.github.yashkasera.alohomora.desktop.domain.config

import io.github.yashkasera.alohomora.common.journey.JourneyReport

/**
 * Renders a [JourneyReport] to a concrete format. One report, pluggable reporters: JUnit XML for CI,
 * JSON for tools/MCP, HTML for humans.
 */
interface JourneyReporter {
    val fileExtension: String
    fun render(report: JourneyReport): String
}
