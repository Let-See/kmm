package io.github.letsee

import io.github.letsee.MockImplementations.MockDirectoryFilesFetcher
import io.github.letsee.MockImplementations.MockFileNameCleaner
import io.github.letsee.MockImplementations.MockFileNameProcessor
import io.github.letsee.MockImplementations.MockMockProcessor
import io.github.letsee.implementations.AcceptedRequest
import io.github.letsee.implementations.DefaultGlobalMockDirectoryConfiguration
import io.github.letsee.implementations.DefaultMocksDirectoryProcessor
import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.implementations.DefaultScenarioManager
import io.github.letsee.interfaces.RequestsManager
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.interfaces.ScenarioManager
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.DefaultRequest
import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.models.Request
import io.github.letsee.models.RequestStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CapturingRequestsManager : RequestsManager {
    private var _deferred = CompletableDeferred<List<CategorisedMocks>?>()
    var capturedMocks: List<CategorisedMocks>? = null

    override val onRequestAccepted: ((Request) -> Unit)? = null
    override val onRequestRemoved: ((Request) -> Unit)? = null
    override val scenarioManager: ScenarioManager = DefaultScenarioManager()
    private val _requestsStack = MutableSharedFlow<List<AcceptedRequest>>(replay = 1)
    override val requestsStack: SharedFlow<List<AcceptedRequest>> = _requestsStack.asSharedFlow()

    fun reset() {
        _deferred = CompletableDeferred()
        capturedMocks = null
    }

    suspend fun awaitAccept(): List<CategorisedMocks>? {
        return withContext(Dispatchers.Default) { withTimeout(2000) { _deferred.await() } }
    }

    override suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
        capturedMocks = mocks
        _deferred.complete(mocks)
    }

    override suspend fun respond(request: Request, withResponse: Response) {}
    override suspend fun respond(request: Request, withMockResponse: Mock) {}
    override suspend fun respond(request: Request) {}
    override suspend fun update(request: Request, status: RequestStatus) {}
    override suspend fun cancel(request: Request) {}
    override suspend fun finish(request: Request) {}
}

class DefaultLetSeeTest {
    private lateinit var capturingManager: CapturingRequestsManager
    private val successMockInfo = MockFileInformation(
        rawPath = "/mocks/api/users/success_200.json",
        statusCode = 200u,
        delay = null,
        status = MockFileInformation.MockStatus.SUCCESS,
        displayName = "success_200.json",
        relativePath = "/api/users/success_200.json"
    )
    private val usersKey = "/api/users/"
    private val usersMocks = listOf(
        Mock.SUCCESS(
            name = "success_200.json",
            response = DefaultResponse(200u, 200u, null, null, null, emptyMap()),
            fileInformation = successMockInfo
        )
    )

    @BeforeTest
    fun setUp() {
        capturingManager = CapturingRequestsManager()
    }

    private fun letSeeWith(mocks: Map<String, List<Mock>>): DefaultLetSee =
        DefaultLetSee(requestsManager = capturingManager, mocks = mocks)

    @Test
    fun `exact path match`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users/")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertEquals(usersMocks, received?.first()?.mocks)
    }

    @Test
    fun `case insensitive match`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/API/Users/")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertEquals(usersMocks, received?.first()?.mocks)
    }

    @Test
    fun `trailing slash normalization`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertEquals(usersMocks, received?.first()?.mocks)
    }

    @Test
    fun `leading slash normalization`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "api/users/")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertEquals(usersMocks, received?.first()?.mocks)
    }

    @Test
    fun `no false prefix match`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users-admin/")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertTrue(received?.first()?.mocks?.isEmpty() == true,
            "'/api/users-admin/' must NOT match '/api/users/' key")
    }

    @Test
    fun `query parameters are stripped before matching`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users?page=1")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertEquals(usersMocks, received?.first()?.mocks,
            "Query parameters should be stripped; '/api/users?page=1' should match '/api/users/'")
    }

    @Test
    fun `unknown path returns empty mocks list`() = runTest {
        val sut = letSeeWith(mapOf(usersKey to usersMocks))
        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/orders/")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()
        assertTrue(received?.first()?.mocks?.isEmpty() == true)
    }

    @Test
    fun `addRequest finds mocks when keys produced by processor normalization`() = runTest {
        val rootPath = "/mocks/"
        val usersDirPath = "${rootPath}api/Users/"
        val mockInfo = MockFileInformation(
            "${usersDirPath}success_200.json", 200u, null,
            MockFileInformation.MockStatus.SUCCESS, "success_200.json", null
        )
        val mockObj = Mock.SUCCESS(
            "success_200.json",
            DefaultResponse(200u, 200u, null, null, null, emptyMap()),
            mockInfo
        )
        val dummyGlobalInfo = MockFileInformation(
            "${rootPath}.ls.global.json", null, null,
            MockFileInformation.MockStatus.SUCCESS, ".ls.global.json", null
        )

        val files = mapOf(
            rootPath to listOf(".ls.global.json"),
            usersDirPath to listOf("success_200.json")
        )
        val cleaner = MockFileNameCleaner()
        val fnProcessor = MockFileNameProcessor(cleaner, result = listOf(dummyGlobalInfo, mockInfo))
        val dirFetcher = MockDirectoryFilesFetcher(result = files)
        val mockProcessor = MockMockProcessor(fnProcessor, results = listOf(mockObj))
        val config = DefaultGlobalMockDirectoryConfiguration(emptyList())
        val processor = DefaultMocksDirectoryProcessor(fnProcessor, mockProcessor, dirFetcher) { config }

        val processorMocks = processor.process(rootPath)
        val sut = DefaultLetSee(requestsManager = capturingManager, mocks = processorMocks)

        val request = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users")
        sut.addRequest(request, MockResult())
        val received = capturingManager.awaitAccept()

        assertTrue(received?.first()?.mocks?.isNotEmpty() == true,
            "addRequest normalization must agree with processor key format")
        assertEquals("success_200.json", received?.first()?.mocks?.first()?.name)
    }

    @Test
    fun `stress test - 50 concurrent addRequest calls all complete without crashes`() = runTest {
        val countingManager = CountingRequestsManager(expected = 50)
        val sut = DefaultLetSee(requestsManager = countingManager, mocks = mapOf(usersKey to usersMocks))
        repeat(50) {
            sut.addRequest(
                DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users/"),
                MockResult()
            )
        }
        countingManager.awaitAll()
        assertEquals(50, countingManager.acceptedCount)
    }

    @Test
    fun `exception in addRequest coroutine is captured by handler and does not crash`() = runTest {
        val capturedThrowable = CompletableDeferred<Throwable>()
        val throwingManager = ThrowingRequestsManager()
        val sut = DefaultLetSee(
            requestsManager = throwingManager,
            mocks = mapOf(usersKey to usersMocks),
            onCoroutineError = { capturedThrowable.complete(it) }
        )
        sut.addRequest(
            DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/users/"),
            MockResult()
        )
        val exception = withContext(Dispatchers.Default) { withTimeout(2000) { capturedThrowable.await() } }
        assertTrue(exception is IllegalStateException,
            "Expected IllegalStateException but got ${exception::class.simpleName}")
        assertEquals("Simulated failure in accept", exception.message)
    }
}

/** Counts how many times [accept] is called; used in the concurrent stress test. */
class CountingRequestsManager(private val expected: Int) : RequestsManager {
    private val _count = MutableStateFlow(0)
    val acceptedCount: Int get() = _count.value

    override val onRequestAccepted: ((Request) -> Unit)? = null
    override val onRequestRemoved: ((Request) -> Unit)? = null
    override val scenarioManager: ScenarioManager = DefaultScenarioManager()
    private val _requestsStack = MutableSharedFlow<List<AcceptedRequest>>(replay = 1)
    override val requestsStack: SharedFlow<List<AcceptedRequest>> = _requestsStack.asSharedFlow()

    override suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
        _count.update { it + 1 }
    }

    suspend fun awaitAll(): Unit = withContext(Dispatchers.Default) {
        withTimeout(5000) { _count.first { it >= expected } }
    }

    override suspend fun respond(request: Request, withResponse: Response) {}
    override suspend fun respond(request: Request, withMockResponse: Mock) {}
    override suspend fun respond(request: Request) {}
    override suspend fun update(request: Request, status: RequestStatus) {}
    override suspend fun cancel(request: Request) {}
    override suspend fun finish(request: Request) {}
}

/** Always throws [IllegalStateException] inside [accept]; used to verify exception handling. */
class ThrowingRequestsManager : RequestsManager {
    override val onRequestAccepted: ((Request) -> Unit)? = null
    override val onRequestRemoved: ((Request) -> Unit)? = null
    override val scenarioManager: ScenarioManager = DefaultScenarioManager()
    private val _requestsStack = MutableSharedFlow<List<AcceptedRequest>>(replay = 1)
    override val requestsStack: SharedFlow<List<AcceptedRequest>> = _requestsStack.asSharedFlow()

    override suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
        throw IllegalStateException("Simulated failure in accept")
    }

    override suspend fun respond(request: Request, withResponse: Response) {}
    override suspend fun respond(request: Request, withMockResponse: Mock) {}
    override suspend fun respond(request: Request) {}
    override suspend fun update(request: Request, status: RequestStatus) {}
    override suspend fun cancel(request: Request) {}
    override suspend fun finish(request: Request) {}
}
