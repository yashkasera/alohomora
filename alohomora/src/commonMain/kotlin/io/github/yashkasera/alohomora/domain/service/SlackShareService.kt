package io.github.yashkasera.alohomora.domain.service

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

internal class SlackShareService(
    private val httpClient: HttpClient,
) {
    private val webhookUrl: String? by lazy {
        AlohomoraImpl.config?.slackWebhookUrl
    }

    suspend fun shareCurl(
        trace: TrafficEntry,
        recipientEmail: String,
    ): Result<Unit> {
        val curlCommand = trace.curlCommand()
        val message = buildSlackMessage(
            trace = trace,
            recipientEmail = recipientEmail,
            content = "```bash\n$curlCommand\n```",
        )
        return postToWebhook(message)
    }

    suspend fun shareText(
        trace: TrafficEntry,
        recipientEmail: String,
    ): Result<Unit> {
        val rawText = trace.generateTransactionText()
        val message = buildSlackMessage(
            trace = trace,
            recipientEmail = recipientEmail,
            content = "```\n$rawText\n```",
        )
        return postToWebhook(message)
    }

    private fun buildSlackMessage(
        trace: TrafficEntry,
        recipientEmail: String,
        content: String,
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
            buildIdentifier = AlohomoraImpl.identifier,
        )
    }

    private suspend fun postToWebhook(message: SlackMessage): Result<Unit> {
        return try {
            val url = requireNotNull(webhookUrl) {
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
