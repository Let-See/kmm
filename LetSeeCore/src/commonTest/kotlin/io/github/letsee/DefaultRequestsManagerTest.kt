package io.github.letsee
import io.github.letsee.implementations.AcceptedRequest
import io.github.letsee.implementations.DefaultRequestsManager
import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.models.DefaultRequest
import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.models.Request
import io.github.letsee.models.RequestStatus
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MockResult: Result {
    override fun success(response: Response) {

    }

    override fun failure(error: Response) {

    }
}

class DefaultRequestsManagerTest {
    private lateinit var sut: DefaultRequestsManager
    @BeforeTest
    fun setup() {
        sut = DefaultRequestsManager()
    }

    @Test
    fun testAccept() = runTest {
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/arrangements")
        sut.accept(givenRequest,
            MockResult(), null
        )

        sut.requestsStack
            .take(1)
            .onEach{
                assertEquals(1, it.size)
                assertEquals(givenRequest, it.first().request)
            }
            .collect()
    }

    @Test
    fun testRespond() = runTest {
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/arrangements")

        val requestsStackStatsResult :MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            sut.requestsStack
                .take(3)
                .collect{
                    requestsStackStatsResult.add(it)
                }
        }
        yield()

        sut.accept(givenRequest, MockResult(), null)
        sut.respond(request = givenRequest, DefaultResponse.CANCEL)

        collectorJob.join()
        val expected: List<List<AcceptedRequest>> = listOf(emptyList(), listOf(AcceptedRequest(givenRequest, MockResult(), RequestStatus.IDLE,null)), emptyList())
        assertEquals(expected, requestsStackStatsResult)
    }

    @Test
    fun testUpdate() = runTest {
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/test")
        val stackStates: MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            sut.requestsStack
                .take(3) // init [] → accept [r(IDLE)] → update [r(LOADING)]
                .collect { stackStates.add(it) }
        }
        yield()

        sut.accept(givenRequest, MockResult(), null)
        sut.update(givenRequest, RequestStatus.LOADING)

        collectorJob.join()

        assertEquals(3, stackStates.size)
        assertEquals(RequestStatus.LOADING, stackStates.last().first().status)
    }

    @Test
    fun testCancel() = runTest {
        var removeCount = 0
        val trackingSut = DefaultRequestsManager(onRequestRemoved = { removeCount++ })
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/test")

        val stackStates: MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            trackingSut.requestsStack
                .take(4) // init [] → accept [r(IDLE)] → cancel updates [r(LOADING)] → cancel removes []
                .collect { stackStates.add(it) }
        }
        yield()

        trackingSut.accept(givenRequest, MockResult(), null)
        trackingSut.cancel(givenRequest)

        collectorJob.join()

        assertEquals(1, removeCount, "onRequestRemoved should fire once for cancel")
        assertEquals(emptyList(), stackStates.last(), "stack should be empty after cancel")
    }

    @Test
    fun testFinish() = runTest {
        var removeCount = 0
        val trackingSut = DefaultRequestsManager(onRequestRemoved = { removeCount++ })
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/test")

        val stackStates: MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            trackingSut.requestsStack
                .take(3) // init [] → accept [r(IDLE)] → finish []
                .collect { stackStates.add(it) }
        }
        yield()

        trackingSut.accept(givenRequest, MockResult(), null)
        trackingSut.finish(givenRequest)

        collectorJob.join()

        assertEquals(1, removeCount, "onRequestRemoved should fire once for finish")
        assertEquals(emptyList(), stackStates.last(), "stack should be empty after finish")
    }

    @Test
    fun testGetRequestsStack() = runTest {
        val r1 = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/r1")
        val r2 = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/r2")
        val r3 = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/r3")

        val stackStates: MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            sut.requestsStack
                .take(4) // init [] → [r1] → [r1,r2] → [r1,r2,r3]
                .collect { stackStates.add(it) }
        }
        yield()

        sut.accept(r1, MockResult(), null)
        sut.accept(r2, MockResult(), null)
        sut.accept(r3, MockResult(), null)

        collectorJob.join()

        assertEquals(3, stackStates.last().size)
        assertEquals(r1, stackStates.last()[0].request)
        assertEquals(r2, stackStates.last()[1].request)
        assertEquals(r3, stackStates.last()[2].request)
    }

    @Test
    fun testGetOnRequestAccepted() = runTest {
        val capturedRequests: MutableList<Request> = mutableListOf()
        val trackingSut = DefaultRequestsManager(onRequestAccepted = { capturedRequests.add(it) })
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/test")

        trackingSut.accept(givenRequest, MockResult(), null)

        assertEquals(1, capturedRequests.size)
        assertEquals(givenRequest, capturedRequests.first())
    }

    @Test
    fun testGetOnRequestRemoved() = runTest {
        val capturedRequests: MutableList<Request> = mutableListOf()
        val trackingSut = DefaultRequestsManager(onRequestRemoved = { capturedRequests.add(it) })
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://google.com", path = "/v1/test")

        trackingSut.accept(givenRequest, MockResult(), null)
        trackingSut.finish(givenRequest)

        assertEquals(1, capturedRequests.size)
        assertEquals(givenRequest, capturedRequests.first())
    }

    @Test
    fun `respond with mock removes request from stack exactly once`() = runTest {
        var removeCount = 0
        val trackingSut = DefaultRequestsManager(onRequestRemoved = { removeCount++ })
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/test")
        val successMockInfo = MockFileInformation(
            rawPath = "/some/path/success_200.json",
            statusCode = 200u,
            delay = null,
            status = MockFileInformation.MockStatus.SUCCESS,
            displayName = "success_200.json",
            relativePath = null
        )
        val mockResponse = Mock.SUCCESS(
            name = "success_200.json",
            response = DefaultResponse(200u, 200u, null, null, null, emptyMap()),
            fileInformation = successMockInfo
        )

        val stackStates: MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            trackingSut.requestsStack
                .take(3)
                .collect { stackStates.add(it) }
        }
        yield()

        trackingSut.accept(givenRequest, MockResult(), null)
        trackingSut.respond(request = givenRequest, withMockResponse = mockResponse)

        collectorJob.join()

        assertEquals(1, removeCount, "onRequestRemoved should be called exactly once")
        assertEquals(3, stackStates.size)
        assertEquals(emptyList(), stackStates.last(), "stack should be empty after respond")
    }

    @Test
    fun `respond with mock does not double-finish - sentinel request survives`() = runTest {
        var removeCount = 0
        val trackingSut = DefaultRequestsManager(onRequestRemoved = { removeCount++ })
        val givenRequest = DefaultRequest(emptyMap(), "GET", "https://example.com", path = "/api/test")
        val successMockInfo = MockFileInformation(
            rawPath = "/some/path/success_200.json",
            statusCode = 200u,
            delay = null,
            status = MockFileInformation.MockStatus.SUCCESS,
            displayName = "success_200.json",
            relativePath = null
        )
        val mockResponse = Mock.SUCCESS(
            name = "success_200.json",
            response = DefaultResponse(200u, 200u, null, null, null, emptyMap()),
            fileInformation = successMockInfo
        )

        // Accept the same request twice to create a sentinel: if a redundant
        // finish() runs after the when-block, indexOf will find the second copy
        // and remove it too — making removeCount == 2 and the stack empty.
        val stackStates: MutableList<List<AcceptedRequest>> = mutableListOf()
        val collectorJob = launch {
            trackingSut.requestsStack
                .take(4) // init [] → accept [r] → accept [r,r] → finish [r]
                .collect { stackStates.add(it) }
        }
        yield()

        trackingSut.accept(givenRequest, MockResult(), null)
        trackingSut.accept(givenRequest, MockResult(), null)
        trackingSut.respond(request = givenRequest, withMockResponse = mockResponse)

        collectorJob.join()

        assertEquals(1, removeCount,
            "Only one finish() should execute; a double-finish would also remove the sentinel request")
        assertEquals(4, stackStates.size)
        assertEquals(1, stackStates.last().size,
            "Sentinel request must remain on stack; double-finish would leave it empty")
    }
}