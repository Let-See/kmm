package io.github.letsee

import io.github.letsee.implementations.DefaultRequestResponseLogger
import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.implementations.LoggingResult
import io.github.letsee.interfaces.LogStorage
import io.github.letsee.interfaces.RequestResponseLogger
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.models.DefaultRequest
import io.github.letsee.models.Request
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// ── In-memory LogStorage stub ─────────────────────────────────────────────────

class InMemoryLogStorage : LogStorage {
    override val filePath: String = "/test/in-memory.log"
    private val buffer = StringBuilder()

    val content: String get() = buffer.toString()

    override fun append(text: String) {
        buffer.append(text)
    }

    override fun clear() {
        buffer.clear()
    }
}

// ── Capturing Result stub ─────────────────────────────────────────────────────

class CapturingResult : Result {
    var successResponse: Response? = null
    var failureResponse: Response? = null

    override fun success(response: Response) {
        successResponse = response
    }

    override fun failure(error: Response) {
        failureResponse = error
    }
}

// ── Tests ─────────────────────────────────────────────────────────────────────

class RequestResponseLoggerTest {

    private val storage = InMemoryLogStorage()
    private val logger = DefaultRequestResponseLogger(storage = storage, maxBodyLength = 20)

    private val request = DefaultRequest(
        headers = mapOf("Content-Type" to "application/json"),
        requestMethod = "GET",
        uri = "https://api.example.com/users/123",
        path = "/users/123"
    )

    private val successResponse = DefaultResponse(
        responseCode = 200u,
        requestCode = 200u,
        byteResponse = """{"id":123}""".encodeToByteArray(),
        errorMessage = null,
        statusText = "OK",
        headers = mapOf("Content-Type" to listOf("application/json"))
    )

    private val cancelResponse = DefaultResponse.CANCEL

    private val failureResponse = DefaultResponse(
        responseCode = 500u,
        requestCode = 500u,
        byteResponse = """{"error":"server error"}""".encodeToByteArray(),
        errorMessage = "Internal Server Error",
        statusText = null,
        headers = emptyMap()
    )

    // 10.1 – log formatting: logRequest produces expected content
    @Test
    fun `logRequest writes method and URI to storage`() {
        logger.logRequest(request)
        val log = storage.content
        assertTrue(log.contains("GET"), "log should contain the HTTP method")
        assertTrue(log.contains("https://api.example.com/users/123"), "log should contain the URI")
        assertTrue(log.contains("Content-Type"), "log should contain request headers")
        assertTrue(log.contains("► REQUEST"), "log should contain REQUEST marker")
    }

    // 10.1 – log formatting: logResponse produces expected content
    @Test
    fun `logResponse writes status code and mock type to storage`() {
        logger.logResponse(request, successResponse, mockUsed = "SUCCESS", isSuccess = true, elapsedMs = 100L)
        val log = storage.content
        assertTrue(log.contains("200"), "log should contain the response code")
        assertTrue(log.contains("SUCCESS"), "log should contain the mock type")
        assertTrue(log.contains("100ms"), "log should contain elapsed time")
        assertTrue(log.contains("◄ RESPONSE"), "log should contain RESPONSE marker")
    }

    // 10.5 – custom storage injection
    @Test
    fun `DefaultRequestResponseLogger uses the provided custom storage`() {
        val customStorage = InMemoryLogStorage()
        val customLogger = DefaultRequestResponseLogger(storage = customStorage)
        customLogger.logRequest(request)
        assertTrue(customStorage.content.isNotEmpty(), "custom storage should receive the log entry")
        assertTrue(storage.content.isEmpty(), "default storage should remain untouched")
    }

    // 10.6 – body truncation
    @Test
    fun `logResponse truncates body longer than maxBodyLength`() {
        val longBody = "A".repeat(100)
        val bigResponse = DefaultResponse(
            responseCode = 200u,
            requestCode = 200u,
            byteResponse = longBody.encodeToByteArray(),
            errorMessage = null,
            statusText = null,
            headers = emptyMap()
        )
        logger.logResponse(request, bigResponse, mockUsed = "SUCCESS", isSuccess = true, elapsedMs = 50L)
        val log = storage.content
        assertTrue(log.contains("(truncated)"), "body exceeding maxBodyLength should show truncation marker")
        val bodySection = log.substringAfter("Body (")
        val displayedBody = bodySection.substringAfter("bytes):").trim()
        assertTrue(
            displayedBody.length <= 100,
            "displayed body length should respect maxBodyLength"
        )
    }

    // 10.6 – body within limit: no truncation marker
    @Test
    fun `logResponse does not truncate body within maxBodyLength`() {
        val shortBody = "short"
        val smallResponse = DefaultResponse(
            responseCode = 200u,
            requestCode = 200u,
            byteResponse = shortBody.encodeToByteArray(),
            errorMessage = null,
            statusText = null,
            headers = emptyMap()
        )
        logger.logResponse(request, smallResponse, mockUsed = "SUCCESS", isSuccess = true, elapsedMs = 10L)
        val log = storage.content
        assertTrue(!log.contains("(truncated)"), "short body should not be truncated")
        assertTrue(log.contains(shortBody), "log should contain the full body")
    }

    // 10.2 – LoggingResult delegation: success path
    @Test
    fun `LoggingResult calls delegate success and logs response`() {
        val delegate = CapturingResult()
        val loggingResult = LoggingResult(delegate, logger, request, 0L)

        loggingResult.success(successResponse)

        assertNotNull(delegate.successResponse, "delegate.success() should be called")
        assertEquals(successResponse, delegate.successResponse)
        val log = storage.content
        assertTrue(log.contains("SUCCESS"), "log should record mock type SUCCESS for 200 response")
    }

    // 10.3 – LoggingResult delegation: failure path
    @Test
    fun `LoggingResult calls delegate failure and logs response`() {
        val delegate = CapturingResult()
        val loggingResult = LoggingResult(delegate, logger, request, 0L)

        loggingResult.failure(failureResponse)

        assertNotNull(delegate.failureResponse, "delegate.failure() should be called")
        assertEquals(failureResponse, delegate.failureResponse)
        val log = storage.content
        assertTrue(log.contains("FAILURE"), "log should record mock type FAILURE for 500 response")
    }

    // Approach B: cancel response derives CANCEL mock name
    @Test
    fun `LoggingResult derives CANCEL mock name for 400 response with null body`() {
        val delegate = CapturingResult()
        val loggingResult = LoggingResult(delegate, logger, request, 0L)

        loggingResult.failure(cancelResponse)

        assertNotNull(delegate.failureResponse)
        val log = storage.content
        assertTrue(log.contains("CANCEL"), "log should record mock type CANCEL for 400+null-body response")
    }

    // clear() delegates to storage
    @Test
    fun `logger clear() clears the underlying storage`() {
        logger.logRequest(request)
        assertTrue(storage.content.isNotEmpty())
        logger.clear()
        assertTrue(storage.content.isEmpty(), "storage should be empty after clear()")
    }

    // 10.7 – Integration test: DefaultLetSee with logger
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `DefaultLetSee with logger records request and response in storage`() = runTest {
        val integrationStorage = InMemoryLogStorage()
        val integrationLogger = DefaultRequestResponseLogger(storage = integrationStorage)

        val sut = DefaultLetSee(
            logger = integrationLogger,
            dispatcher = UnconfinedTestDispatcher()
        )
        val capturingResult = CapturingResult()
        val testRequest = DefaultRequest(emptyMap(), "POST", "https://api.example.com/orders", "/orders")

        sut.addRequest(testRequest, capturingResult)

        // Retrieve the header-injected request from the requests stack
        val acceptedRequest = sut.requestsManager.requestsStack.replayCache.first().first().request
        sut.requestsManager.respond(acceptedRequest, successResponse)

        sut.close()

        val log = integrationStorage.content
        assertTrue(log.contains("POST"), "integration log should contain the HTTP method")
        assertTrue(log.contains("https://api.example.com/orders"), "integration log should contain the URI")
        assertTrue(log.contains("200"), "integration log should contain the response code")
    }
}
