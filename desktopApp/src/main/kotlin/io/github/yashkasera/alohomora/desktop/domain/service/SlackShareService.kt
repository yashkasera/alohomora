package io.github.yashkasera.alohomora.desktop.domain.service

import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class SlackShareService(
    private val httpClient: HttpClient,
) {
    suspend fun shareCurl(
        trace: TrafficEntry,
        recipientEmail: String,
        buildInfo: BuildInfo?,
    ): Result<Unit> {
        val curlCommand = trace.curlCommand()
        val message = buildTraceMessage(
            trace = trace,
            recipientEmail = recipientEmail,
            content = "```bash\n$curlCommand\n```",
            buildInfo = buildInfo,
        )
        return postToWebhook(message, buildInfo?.slackWebhookUrl)
    }

    suspend fun shareText(
        trace: TrafficEntry,
        recipientEmail: String,
        buildInfo: BuildInfo?,
    ): Result<Unit> {
        val rawText = trace.generateTransactionText()
        val message = buildTraceMessage(
            trace = trace,
            recipientEmail = recipientEmail,
            content = "```\n$rawText\n```",
            buildInfo = buildInfo,
        )
        return postToWebhook(message, buildInfo?.slackWebhookUrl)
    }

    /**
     * Posts one telemetry event, name and payload.
     *
     * Its own method rather than a generalised [buildTraceMessage]: an event has no method, status or
     * duration, so the shared version would be four nullable fields wide to serve two callers.
     */
    suspend fun shareEvent(
        event: Event,
        recipientEmail: String,
        buildInfo: BuildInfo?,
    ): Result<Unit> {
        val summary = buildString {
            appendLine("📊 Event Shared")
            appendLine()
            appendLine("• Name: ${event.name}")
            appendLine("• Time: ${DateUtils.format(event.time, DateUtils.Format.ISO_DATE_TIME)}")
            appendLine()
            appendLine("```\n${event.prettyProperties()}\n```")
        }
        return postToWebhook(
            SlackMessage(
                content = summary,
                recipientEmail = recipientEmail,
                buildIdentifier = buildInfo.toBuildIdentifier(),
            ),
            buildInfo?.slackWebhookUrl,
        )
    }

    suspend fun shareError(
        error: Error,
        recipientEmail: String,
        buildInfo: BuildInfo?,
    ): Result<Unit> {
        val summary = buildString {
            appendLine("🚨 Error Shared")
            appendLine()
            appendLine("• Type: ${error.exceptionTypeName()}")
            error.reason?.takeIf(String::isNotBlank)?.let { appendLine("• Reason: $it") }
            appendLine("• Time: ${DateUtils.format(error.time, DateUtils.Format.ISO_DATE_TIME)}")
            error.place?.takeIf(String::isNotBlank)?.let { appendLine("• Place: $it") }
            error.stackTrace?.takeIf(String::isNotBlank)?.let {
                appendLine()
                appendLine("```\n$it\n```")
            }
        }
        return postToWebhook(
            SlackMessage(
                content = summary,
                recipientEmail = recipientEmail,
                buildIdentifier = buildInfo.toBuildIdentifier(),
            ),
            buildInfo?.slackWebhookUrl,
        )
    }

    private fun buildTraceMessage(
        trace: TrafficEntry,
        recipientEmail: String,
        content: String,
        buildInfo: BuildInfo?,
    ): SlackMessage {
        val summary = buildString {
            appendLine("🌐 API Request Shared")
            appendLine()
            appendLine("• Method: ${trace.method ?: "N/A"} ${trace.pathWithQuery()}")
            appendLine("• Status: ${trace.status ?: "N/A"} ${trace.statusMessage()}")
            appendLine("• Duration: ${trace.duration ?: 0}ms")
            appendLine("• Time: ${trace.time ?: "N/A"}")
            appendLine()
            appendLine(content)
        }
        return SlackMessage(
            content = summary,
            recipientEmail = recipientEmail,
            buildIdentifier = buildInfo.toBuildIdentifier(),
        )
    }

    private fun BuildInfo?.toBuildIdentifier(): String? {
        if (this == null) return null
        return "${appName}-${variantName}-${versionName}-${commitSha}"
    }

    private suspend fun postToWebhook(message: SlackMessage, webhookUrl: String?): Result<Unit> {
        return try {
            val url = requireNotNull(webhookUrl?.takeIf { it.isNotBlank() }) {
                return Result.failure(Exception("Slack webhook URL is not configured"))
            }
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(message)
            }
            if (response.status.value in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Slack webhook returned ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    private data class SlackMessage(
        val content: String,
        val recipientEmail: String,
        val buildIdentifier: String? = null,
    )
}
