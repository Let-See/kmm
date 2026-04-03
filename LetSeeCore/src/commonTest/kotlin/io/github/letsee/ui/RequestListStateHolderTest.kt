package io.github.letsee.ui

import io.github.letsee.Configuration
import io.github.letsee.MockResult
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeRequestsManager : RequestsManager {
    override val scenarioManager: ScenarioManager = DefaultScenarioManager()
    override val onRequestAccepted: ((Request) -> Unit)? = null
    override val onRequestRemoved: ((Request) -> Unit)? = null

    val requestsFlow = MutableSharedFlow<List<AcceptedRequest>>(replay = 1)
    override val requestsStack: SharedFlow<List<AcceptedRequest>> = requestsFlow

    var respondWithMockCalled = false
    var respondWithMockRequest: Request? = null
    var respondWithMockMock: Mock? = null

    var respondLiveCalled = false
    var respondLiveRequest: Request? = null

    override suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {}
    override suspend fun respond(request: Request, withResponse: Response) {}
    override suspend fun respond(request: Request, withMockResponse: Mock) {
        respondWithMockCalled = true
        respondWithMockRequest = request
        respondWithMockMock = withMockResponse
    }
    override suspend fun respond(request: Request) {
        respondLiveCalled = true
        respondLiveRequest = request
    }
    override suspend fun update(request: Request, status: RequestStatus) {}
    override suspend fun cancel(request: Request) {}
    override suspend fun finish(request: Request) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class RequestListStateHolderTest {

    private fun makeRequest(path: String): DefaultRequest =
        DefaultRequest(emptyMap(), "GET", "https://example.com$path", path = path)

    @Test
    fun `requests maps AcceptedRequests to RequestUIModel`() = runTest {
        val manager = FakeRequestsManager()
        val config = MutableStateFlow(Configuration.default)
        val scope = TestScope(UnconfinedTestDispatcher())
        val sut = RequestListStateHolder(manager, config, scope)

        val r1 = makeRequest("/api/users/")
        val r2 = makeRequest("/api/orders/")
        val accepted = listOf(
            AcceptedRequest(r1, MockResult(), RequestStatus.IDLE, null),
            AcceptedRequest(r2, MockResult(), RequestStatus.LOADING, null),
        )
        manager.requestsFlow.emit(accepted)

        val models = sut.requests.first { it.isNotEmpty() }

        assertEquals(2, models.size)
        assertEquals(r1.id, models[0].requestId)
        assertEquals(r2.id, models[1].requestId)
        assertEquals(RequestStatus.IDLE, models[0].status)
        assertEquals(RequestStatus.LOADING, models[1].status)
    }

    @Test
    fun `requests uses configuration for display names`() = runTest {
        val manager = FakeRequestsManager()
        val config = MutableStateFlow(
            Configuration(isMockEnabled = false, shouldCutBaseURLFromURLsTitle = true, baseURL = "https://example.com")
        )
        val scope = TestScope(UnconfinedTestDispatcher())
        val sut = RequestListStateHolder(manager, config, scope)

        val r = makeRequest("/api/users/")
        manager.requestsFlow.emit(
            listOf(AcceptedRequest(r, MockResult(), RequestStatus.IDLE, null))
        )

        val models = sut.requests.first { it.isNotEmpty() }
        assertEquals("/api/users/", models[0].displayName)
    }

    @Test
    fun `selectMock delegates to requestsManager respond with mock`() = runTest {
        val manager = FakeRequestsManager()
        val config = MutableStateFlow(Configuration.default)
        val scope = TestScope(UnconfinedTestDispatcher())
        val sut = RequestListStateHolder(manager, config, scope)

        val request = makeRequest("/api/test/")
        val mock = Mock.defaultSuccess("success_200.json", "{}".encodeToByteArray())

        sut.selectMock(request, mock)
        scope.advanceUntilIdle()

        assertTrue(manager.respondWithMockCalled)
        assertEquals(request, manager.respondWithMockRequest)
        assertEquals(mock, manager.respondWithMockMock)
    }

    @Test
    fun `respondLive delegates to requestsManager respond without mock`() = runTest {
        val manager = FakeRequestsManager()
        val config = MutableStateFlow(Configuration.default)
        val scope = TestScope(UnconfinedTestDispatcher())
        val sut = RequestListStateHolder(manager, config, scope)

        val request = makeRequest("/api/live/")

        sut.respondLive(request)
        scope.advanceUntilIdle()

        assertTrue(manager.respondLiveCalled)
        assertEquals(request, manager.respondLiveRequest)
    }

    @Test
    fun `requests emits empty list initially`() = runTest {
        val manager = FakeRequestsManager()
        val config = MutableStateFlow(Configuration.default)
        val scope = TestScope(UnconfinedTestDispatcher())
        val sut = RequestListStateHolder(manager, config, scope)

        assertEquals(emptyList(), sut.requests.value)
    }
}
