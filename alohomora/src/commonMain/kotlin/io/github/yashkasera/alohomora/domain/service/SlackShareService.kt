package io.github.yashkasera.alohomora.domain.service

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.TraceEntry
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SlackShareService(
    private val httpClient: HttpClient,
) {
    val webhookUrl: String? by lazy {
        Alohomora.config?.slackWebhookUrl
    }

    fun isConfigured(): Boolean = webhookUrl.isNullOrBlank().not()

    suspend fun shareCurl(
        trace: TraceEntry,
        recipientEmail: String,
    ): Result<Unit> {
        val curlCommand = trace.curlCommand
        val message = buildSlackMessage(
            trace = trace,
            recipientEmail = recipientEmail,
            content = "```bash\n$curlCommand\n```",
        )
        return postToWebhook(message)
    }

    suspend fun shareText(
        trace: TraceEntry,
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

    suspend fun shareContent(
        title: String,
        content: String,
        recipientEmail: String,
    ): Result<Unit> {
        val message = buildGenericMessage(
            title = title,
            content = content,
            recipientEmail = recipientEmail,
        )
        return postToWebhook(message)
    }

    private fun buildSlackMessage(
        trace: TraceEntry,
        recipientEmail: String,
        content: String,
    ): String {
        val summary = buildString {
            appendLine("🌐 API Request Shared")
            appendLine()
            appendLine("• Method: ${trace.method ?: "N/A"} ${trace.pathWithQuery}")
            appendLine("• Status: ${trace.status ?: "N/A"} ${trace.statusMessage}")
            appendLine("• Duration: ${trace.duration ?: 0}ms")
            appendLine("• Time: ${trace.time ?: "N/A"}")
            appendLine()
            appendLine(content)
        }
        return Json.encodeToString(SlackMessage(text = summary))
    }

    private fun buildGenericMessage(
        title: String,
        content: String,
        recipientEmail: String,
    ): String {
        val summary = buildString {
            appendLine(title)
            appendLine()
            appendLine(content)
        }
        return Json.encodeToString(SlackMessage(text = summary))
    }

    private suspend fun postToWebhook(message: String): Result<Unit> {
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
        val text: String,
    )
}
