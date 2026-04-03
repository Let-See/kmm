package io.github.letsee.models

import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.interfaces.Response
import kotlinx.serialization.json.Json

sealed class Mock(
    open val name: String,
    open val response: Response? = null,
    open val fileInformation: MockFileInformation? = null
) : Comparable<Mock> {

    private val typeOrder: Int
        get() = when (this) {
            is FAILURE -> 0
            is ERROR -> 1
            is SUCCESS -> 2
            is LIVE -> 3
            is CANCEL -> 4
        }

    override fun compareTo(other: Mock): Int {
        val typeCompare = typeOrder.compareTo(other.typeOrder)
        return if (typeCompare != 0) typeCompare else name.lowercase().compareTo(other.name.lowercase())
    }

    val displayName: String
        get() = when (this) {
            is LIVE -> "Live"
            is CANCEL -> "Cancel"
            else -> name
        }

    val formatted: String?
        get() = when (this) {
            is SUCCESS, is FAILURE -> response?.byteResponse?.let { bytes ->
                val rawStr = bytes.decodeToString()
                try {
                    prettyJson.encodeToString(Json.parseToJsonElement(rawStr))
                } catch (_: Exception) {
                    rawStr
                }
            }
            else -> null
        }

    class FAILURE(
        override val name: String,
        override val response: Response,
        override val fileInformation: MockFileInformation
    ) : Mock(name, response, fileInformation) {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is FAILURE -> this.name == other.name && this.response == other.response && this.fileInformation == other.fileInformation
                else -> super.equals(other)
            }
        }
    }

    class SUCCESS(
        override val name: String,
        override val response: Response,
        override val fileInformation: MockFileInformation
    ) : Mock(name, response, fileInformation) {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is SUCCESS -> this.name == other.name && this.response == other.response && this.fileInformation == other.fileInformation
                else -> super.equals(other)
            }
        }
    }

    class ERROR(override val name: String, val error: Throwable) : Mock(name) {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is ERROR -> this.name == other.name && this.error == other.error
                else -> super.equals(other)
            }
        }
    }

    object LIVE : Mock(name = "LIVE") {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is LIVE -> true
                else -> super.equals(other)
            }
        }
    }

    object CANCEL : Mock("CANCEL") {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is CANCEL -> true
                else -> super.equals(other)
            }
        }
    }

    companion object {
        private val prettyJson = Json { prettyPrint = true }

        fun defaultSuccess(name: String, data: ByteArray): SUCCESS {
            val response = DefaultResponse(
                responseCode = 200u,
                requestCode = 200u,
                byteResponse = data,
                errorMessage = null,
                statusText = null,
                headers = mapOf("Content-Type" to listOf("application/json"))
            )
            val fileInfo = MockFileInformation(
                statusCode = 200u,
                status = MockFileInformation.MockStatus.SUCCESS,
                displayName = name,
                rawPath = "",
                delay = null,
                relativePath = null
            )
            return SUCCESS(name, response, fileInfo)
        }

        fun defaultFailure(name: String, data: ByteArray): FAILURE {
            val response = DefaultResponse(
                responseCode = 400u,
                requestCode = 400u,
                byteResponse = data,
                errorMessage = null,
                statusText = null,
                headers = emptyMap()
            )
            val fileInfo = MockFileInformation(
                statusCode = 400u,
                status = MockFileInformation.MockStatus.FAILURE,
                displayName = name,
                rawPath = "",
                delay = null,
                relativePath = null
            )
            return FAILURE(name, response, fileInfo)
        }
    }
}
