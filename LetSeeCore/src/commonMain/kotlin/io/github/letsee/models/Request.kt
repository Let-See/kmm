package io.github.letsee.models

import io.github.letsee.Configuration
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface Request {
    val headers: Map<String, String>
    val requestMethod: String
    val uri: String
    var path: String
    val id: Int
    val logId: String

    companion object {
        const val LOGGER_HEADER_KEY = "LETSEE-LOGGER-ID"
    }
}

data class DefaultRequest(
    override val headers: Map<String, String>,
    override val requestMethod: String,
    override val uri: String,
    override var path: String
) : Request {
    override val id = Random.nextInt()

    @OptIn(ExperimentalUuidApi::class)
    override val logId: String = Uuid.random().toString()
}

fun Request.displayName(configuration: Configuration): String {
    return if (configuration.shouldCutBaseURLFromURLsTitle) {
        uri.replaceFirst(Regex(Regex.escape(configuration.baseURL), RegexOption.IGNORE_CASE), "")
    } else {
        uri
    }
}