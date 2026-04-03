package io.github.letsee.implementations

import io.github.letsee.interfaces.LogStorage
import io.github.letsee.interfaces.RequestResponseLogger
import io.github.letsee.interfaces.Response
import io.github.letsee.models.Request

private const val SEPARATOR = "──────────────────────────────────────"

/**
 * Default implementation of [RequestResponseLogger] that writes human-readable,
 * timestamped log entries to a [LogStorage].
 *
 * @param storage  backend storage; defaults to [FileLogStorage] (clears on init)
 * @param maxBodyLength maximum number of characters to include from the response body;
 *                      longer bodies are truncated and appended with `... (truncated)`
 */
class DefaultRequestResponseLogger(
    override val storage: LogStorage = FileLogStorage(),
    private val maxBodyLength: Int = 4096
) : RequestResponseLogger {

    override fun logRequest(request: Request) {
        val sb = StringBuilder()
        sb.appendLine(SEPARATOR)
        sb.appendLine("[${currentTimestamp()}] ► REQUEST")
        sb.appendLine("  Method: ${request.requestMethod}")
        sb.appendLine("  URI:    ${request.uri}")
        if (request.headers.isNotEmpty()) {
            sb.appendLine("  Headers:")
            request.headers.forEach { (key, value) ->
                sb.appendLine("    $key: $value")
            }
        }
        storage.append(sb.toString())
    }

    override fun logResponse(
        request: Request,
        response: Response,
        mockUsed: String,
        isSuccess: Boolean,
        elapsedMs: Long
    ) {
        val sb = StringBuilder()
        sb.appendLine("[${currentTimestamp()}] ◄ RESPONSE [Mock: $mockUsed] ${elapsedMs}ms")
        sb.appendLine("  Status: ${response.responseCode}")
        if (response.headers.isNotEmpty()) {
            sb.appendLine("  Headers:")
            response.headers.forEach { (key, values) ->
                sb.appendLine("    $key: ${values.joinToString(", ")}")
            }
        }
        val bodyText = response.byteResponse?.decodeToString()
        if (bodyText != null) {
            val truncated = bodyText.length > maxBodyLength
            val displayBody = if (truncated) bodyText.take(maxBodyLength) + "... (truncated)" else bodyText
            sb.appendLine("  Body (${bodyText.length} bytes):")
            sb.appendLine("    $displayBody")
        }
        sb.appendLine(SEPARATOR)
        storage.append(sb.toString())
    }

    override fun clear() {
        storage.clear()
    }
}
