package io.github.letsee

import io.github.letsee.implementations.AcceptedRequest
import io.github.letsee.implementations.DefaultScenarioManager
import io.github.letsee.interfaces.RequestsManager
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.interfaces.ScenarioManager
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.DefaultRequest
import io.github.letsee.models.Mock
import io.github.letsee.models.Request
import io.github.letsee.models.RequestStatus
import io.github.letsee.models.displayName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestCapturingManager : RequestsManager {
    private var _deferred = CompletableDeferred<Request>()
    var capturedRequest: Request? = null

    override val onRequestAccepted: ((Request) -> Unit)? = null
    override val onRequestRemoved: ((Request) -> Unit)? = null
    override val scenarioManager: ScenarioManager = DefaultScenarioManager()
    private val _requestsStack = MutableSharedFlow<List<AcceptedRequest>>(replay = 1)
    override val requestsStack: SharedFlow<List<AcceptedRequest>> = _requestsStack.asSharedFlow()

    fun reset() {
        _deferred = CompletableDeferred()
        capturedRequest = null
    }

    suspend fun awaitAccept(): Request {
        return _deferred.await()
    }

    override suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
        capturedRequest = request
        _deferred.complete(request)
    }

    override suspend fun respond(request: Request, withResponse: Response) {}
    override suspend fun respond(request: Request, withMockResponse: Mock) {}
    override suspend fun respond(request: Request) {}
    override suspend fun update(request: Request, status: RequestStatus) {}
    override suspend fun cancel(request: Request) {}
    override suspend fun finish(request: Request) {}
}

class RequestTest {
    private lateinit var capturingManager: RequestCapturingManager

    @BeforeTest
    fun setUp() {
        capturingManager = RequestCapturingManager()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun letSeeWith(): DefaultLetSee =
        DefaultLetSee(requestsManager = capturingManager, mocks = emptyMap(), dispatcher = UnconfinedTestDispatcher())

    @Test
    fun `addRequest injects LETSEE-LOGGER-ID header when not present`() = runTest {
        val sut = letSeeWith()
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com/api/test", path = "/api/test")
        sut.addRequest(request, MockResult())
        val accepted = capturingManager.awaitAccept()
        assertTrue(
            accepted.headers.containsKey(Request.LOGGER_HEADER_KEY),
            "Expected LETSEE-LOGGER-ID header to be injected"
        )
        val logId = accepted.headers[Request.LOGGER_HEADER_KEY]
        assertNotNull(logId)
        assertTrue(logId.isNotEmpty(), "Expected LETSEE-LOGGER-ID to be non-empty")
        assertEquals(request.logId, logId, "Header value must be the original request's logId")
        assertEquals(request.id, accepted.id, "Injected request must preserve original id")
        assertEquals(request.logId, accepted.logId, "Injected request must preserve original logId")
    }

    @Test
    fun `addRequest does not overwrite existing LETSEE-LOGGER-ID`() = runTest {
        val sut = letSeeWith()
        val existingId = "existing-id"
        val request = DefaultRequest(
            headers = mapOf(Request.LOGGER_HEADER_KEY to existingId),
            requestMethod = "GET",
            uri = "https://example.com/api/test",
            path = "/api/test"
        )
        sut.addRequest(request, MockResult())
        val accepted = capturingManager.awaitAccept()
        assertEquals(
            existingId,
            accepted.headers[Request.LOGGER_HEADER_KEY],
            "Pre-existing LETSEE-LOGGER-ID must not be overwritten"
        )
    }

    @Test
    fun `displayName when shouldCutBaseURLFromURLsTitle is false returns full URI`() {
        val request = DefaultRequest(
            emptyMap(), "GET", "https://api.example.com/users/123", path = "/users/123"
        )
        val config = Configuration(false, false, "https://api.example.com")
        assertEquals("https://api.example.com/users/123", request.displayName(config))
    }

    @Test
    fun `displayName when shouldCutBaseURLFromURLsTitle is true removes baseURL`() {
        val request = DefaultRequest(
            emptyMap(), "GET", "https://api.example.com/users/123", path = "/users/123"
        )
        val config = Configuration(false, true, "https://api.example.com")
        assertEquals("/users/123", request.displayName(config))
    }

    @Test
    fun `displayName preserves original path casing after baseURL removal`() {
        val request = DefaultRequest(
            emptyMap(), "GET", "https://api.example.com/Users/MyPath", path = "/Users/MyPath"
        )
        val config = Configuration(false, true, "https://api.example.com")
        assertEquals("/Users/MyPath", request.displayName(config))
    }

    @Test
    fun `displayName removes only first occurrence of baseURL`() {
        val request = DefaultRequest(
            emptyMap(), "GET", "https://api.example.com/redirect/https://api.example.com/target",
            path = "/redirect/https://api.example.com/target"
        )
        val config = Configuration(false, true, "https://api.example.com")
        assertEquals("/redirect/https://api.example.com/target", request.displayName(config))
    }

    @Test
    fun `displayName when baseURL not in URI returns URI unchanged`() {
        val request = DefaultRequest(
            emptyMap(), "GET", "https://other.service.com/data", path = "/data"
        )
        val config = Configuration(false, true, "https://api.example.com")
        assertEquals("https://other.service.com/data", request.displayName(config))
    }
}
