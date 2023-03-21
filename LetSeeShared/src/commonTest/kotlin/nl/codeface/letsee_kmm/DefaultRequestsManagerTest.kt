package nl.codeface.letsee_kmm
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import nl.codeface.letsee_kmm.implementations.DefaultResponse
import nl.codeface.letsee_kmm.implementations.AcceptedRequest
import nl.codeface.letsee_kmm.implementations.DefaultRequestsManager
import nl.codeface.letsee_kmm.interfaces.Response
import nl.codeface.letsee_kmm.interfaces.Result
import nl.codeface.letsee_kmm.models.DefaultRequest
import nl.codeface.letsee_kmm.models.RequestStatus
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
    fun testAccept() = runBlocking {
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
    fun testRespond() = runBlocking {
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
    fun testUpdate() {
    }

    @Test
    fun testCancel() {
    }

    @Test
    fun testFinish() {
    }

    @Test
    fun testGetRequestsStack() {
    }

    @Test
    fun testGetOnRequestAccepted() {
    }

    @Test
    fun testGetOnRequestRemoved() {
    }
}