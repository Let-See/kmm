package io.github.letsee.interfaces

import io.github.letsee.models.Request

interface RequestResponseLogger {
    fun logRequest(request: Request)
    fun logResponse(
        request: Request,
        response: Response,
        mockUsed: String,
        isSuccess: Boolean,
        elapsedMs: Long
    )
    fun clear()
    val storage: LogStorage
}
