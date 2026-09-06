package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.common.journey.StepOutcome
import io.github.yashkasera.alohomora.desktop.domain.config.JourneyReporter
import kotlinx.serialization.json.Json

/** JSON, straight from the serializable report — the format the MCP tools reuse. */
object JsonJourneyReporter : JourneyReporter {
    override val fileExtension = "json"
    private val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
    override fun render(report: JourneyReport): String =
        json.encodeToString(JourneyReport.serializer(), report)
}

/**
 * JUnit XML: one `<testcase>` per step, a `<failure>` for any non-matched step. CI renders these
 * natively. A [io.github.yashkasera.alohomora.common.journey.JourneyStatus.NOT_EXERCISED] flow surfaces
 * as every step failing (missing), never as a silent pass.
 */
object JUnitJourneyReporter : JourneyReporter {
    override val fileExtension = "xml"
    override fun render(report: JourneyReport): String {
        val failures = report.steps.count { it.outcome != StepOutcome.MATCHED }
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine(
                """<testsuite name="${xml(report.name)}" tests="${report.steps.size}" failures="$failures">""",
            )
            report.steps.forEach { result ->
                val name = xml(result.step.eventName)
                if (result.outcome == StepOutcome.MATCHED) {
                    appendLine("""  <testcase name="$name"/>""")
                } else {
                    appendLine("""  <testcase name="$name">""")
                    appendLine("""    <failure message="${xml(result.outcome.name)}"/>""")
                    appendLine("""  </testcase>""")
                }
            }
            append("</testsuite>")
        }
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}

/** An annotated timeline for humans: the status, the expected steps, and every event tagged. */
object HtmlJourneyReporter : JourneyReporter {
    override val fileExtension = "html"
    override fun render(report: JourneyReport): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html><head><meta charset=\"utf-8\"><title>${html(report.name)}</title></head><body>")
        appendLine("<h1>${html(report.name)}</h1>")
        appendLine("<p><strong>Status:</strong> ${report.status}</p>")
        appendLine("<h2>Steps</h2>")
        appendLine("<ul>")
        report.steps.forEach { result ->
            appendLine("<li>${html(result.step.eventName)} — ${result.outcome}</li>")
        }
        appendLine("</ul>")
        appendLine("<h2>Timeline</h2>")
        appendLine("<ol>")
        report.timeline.forEach { entry ->
            appendLine("<li>${html(entry.event.name)} <em>[${entry.tag}]</em></li>")
        }
        appendLine("</ol>")
        appendLine("</body></html>")
    }

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
