package io.github.letsee.implementations

import io.github.letsee.interfaces.RequestResponseLogger
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.models.Request

/**
 * A [Result] decorator that transparently logs request/response pairs.
 *
 * Mock type is derived from the response using Approach B:
 * - 200–299 status  → "SUCCESS"
 * - 400 with null body → "CANCEL"
 * - anything else   → "FAILURE"
 *
 * This avoids any changes to [io.github.letsee.implementations.DefaultRequestsManager].
 */
internal class LoggingResult(
    private val delegate: Result,
    private val logger: RequestResponseLogger,
    private val request: Request,
    private val requestTimestamp: Long
) : Result {

    override fun success(response: Response) {
        val elapsed = currentTimeMillis() - requestTimestamp
        val mockUsed = deriveMockName(response)
        logger.logResponse(request, response, mockUsed = mockUsed, isSuccess = true, elapsedMs = elapsed)
        delegate.success(response)
    }

    override fun failure(error: Response) {
        val elapsed = currentTimeMillis() - requestTimestamp
        val mockUsed = deriveMockName(error)
        logger.logResponse(request, error, mockUsed = mockUsed, isSuccess = false, elapsedMs = elapsed)
        delegate.failure(error)
    }

    private fun deriveMockName(response: Response): String {
        return when {
            response.responseCode in 200u..299u -> "SUCCESS"
            response.responseCode == 400u && response.byteResponse == null -> "CANCEL"
            else -> "FAILURE"
        }
    }
}
