package io.github.letsee

import io.github.letsee.implementations.DefaultRequestsManager
import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.models.DefaultRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Verifies that the liveRequestHandler wiring between DefaultLetSee and
 * DefaultRequestsManager works correctly for the "Live" mock selection path.
 */
class LiveHandlerWiringTest {

    @Test
    fun `liveRequestHandler set on DefaultLetSee is available through requestsManager`() {
        val letSee = DefaultLetSee()
        assertNull(letSee.liveRequestHandler, "handler should be null initially")

        val fakeHandler: suspend (io.github.letsee.models.Request) -> Response = {
            DefaultResponse(200u, 0u, null, null, "OK", emptyMap())
        }
        letSee.liveRequestHandler = fakeHandler

        assertNotNull(letSee.liveRequestHandler, "handler should be set after assignment")
    }

    @Test
    fun `liveRequestHandlerProvider delegates to DefaultLetSee liveRequestHandler`() = runTest {
        val letSee = DefaultLetSee()
        val manager = letSee.requestsManager as DefaultRequestsManager

        assertNull(manager.liveRequestHandlerProvider(), "provider should return null when handler not set")

        letSee.liveRequestHandler = {
            DefaultResponse(200u, 0u, null, null, "OK", emptyMap())
        }
        assertNotNull(manager.liveRequestHandlerProvider(), "provider should return non-null when handler is set")
    }

    @Test
    fun `live handler invoked through respond returns correct response`() = runTest {
        val expectedBody = "hello".encodeToByteArray()
        val letSee = DefaultLetSee()
        letSee.liveRequestHandler = { request ->
            DefaultResponse(
                responseCode = 200u,
                requestCode = 0u,
                byteResponse = expectedBody,
                errorMessage = null,
                statusText = "OK",
                headers = mapOf("Content-Type" to listOf("text/plain"))
            )
        }

        var successResponse: Response? = null
        val resultListener = object : Result {
            override fun success(response: Response) { successResponse = response }
            override fun failure(error: Response) {}
        }
        val request = DefaultRequest(emptyMap(), "GET", "https://httpbin.org/get", path = "/get")

        val manager = letSee.requestsManager as DefaultRequestsManager
        manager.accept(request, resultListener, null)
        manager.respond(request)

        val resp = assertNotNull(successResponse, "success callback should have been called")
        assertEquals(200u, resp.responseCode)
        assertEquals("OK", resp.statusText)
        assertEquals(expectedBody.toList(), resp.byteResponse?.toList())
    }

    @Test
    fun `live handler network error results in 502 via respond`() = runTest {
        val letSee = DefaultLetSee()
        letSee.liveRequestHandler = {
            throw RuntimeException("Connection refused")
        }

        var failureResponse: Response? = null
        val resultListener = object : Result {
            override fun success(response: Response) {}
            override fun failure(error: Response) { failureResponse = error }
        }
        val request = DefaultRequest(emptyMap(), "GET", "https://unreachable.test/api", path = "/api")

        val manager = letSee.requestsManager as DefaultRequestsManager
        manager.accept(request, resultListener, null)
        manager.respond(request)

        val errResp = assertNotNull(failureResponse, "failure callback should have been called")
        assertEquals(502u, errResp.responseCode)
        assertEquals("Live request failed: Connection refused", errResp.errorMessage)
    }
}
