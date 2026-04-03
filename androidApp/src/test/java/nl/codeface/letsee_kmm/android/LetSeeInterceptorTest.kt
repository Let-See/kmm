package nl.codeface.letsee_kmm.android

import io.github.letsee.Configuration
import io.github.letsee.DefaultLetSee
import io.github.letsee.models.Mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import nl.codeface.letsee_kmm.android.interceptor.LetSeeInterceptor
import nl.codeface.letsee_kmm.android.interceptor.configureLetSeeLiveHandler
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LetSeeInterceptorTest {

    private lateinit var letSee: DefaultLetSee

    @BeforeTest
    fun setUp() {
        letSee = DefaultLetSee()
    }

    @AfterTest
    fun tearDown() {
        letSee.close()
    }

    private fun enableMocks() {
        letSee.setConfigurations(
            Configuration(isMockEnabled = true, shouldCutBaseURLFromURLsTitle = false, baseURL = "")
        )
    }

    private fun disableMocks() {
        letSee.setConfigurations(
            Configuration(isMockEnabled = false, shouldCutBaseURLFromURLsTitle = false, baseURL = "")
        )
    }

    // ── (a) Happy-path interception ─────────────────────────────────────────

    @Test
    fun test_request_intercepted_when_mocks_enabled() = runTest {
        enableMocks()
        val client = OkHttpClient.Builder()
            .addInterceptor(LetSeeInterceptor(letSee))
            .build()

        val responderJob = launch(Dispatchers.Default) {
            val stack = letSee.requestsManager.requestsStack.first { it.isNotEmpty() }
            letSee.requestsManager.respond(
                stack.first().request,
                Mock.defaultSuccess("intercepted", """{"intercepted":true}""".toByteArray())
            )
        }

        val response = withContext(Dispatchers.IO) {
            client.newCall(
                okhttp3.Request.Builder().url("http://localhost/api/test").build()
            ).execute()
        }

        responderJob.join()

        assertEquals(200, response.code)
        val body = response.body?.string()
        assertNotNull(body)
        assertTrue(body.contains("intercepted"))
    }

    // ── (b) Bypass when mocks disabled ──────────────────────────────────────

    @Test
    fun test_request_bypasses_when_mocks_disabled() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"live":true}""").setResponseCode(200))
        server.start()

        try {
            disableMocks()
            val client = OkHttpClient.Builder()
                .addInterceptor(LetSeeInterceptor(letSee))
                .build()

            val response = client.newCall(
                okhttp3.Request.Builder().url(server.url("/api/bypass")).build()
            ).execute()

            assertEquals(200, response.code)
            assertEquals("""{"live":true}""", response.body?.string())
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    // ── (c) Exact mock body match ───────────────────────────────────────────

    @Test
    fun test_mock_response_data_matches() = runTest {
        enableMocks()
        val expectedJson = """{"id":42,"name":"LetSee","active":true}"""
        val mock = Mock.defaultSuccess("exact-match", expectedJson.toByteArray())

        val client = OkHttpClient.Builder()
            .addInterceptor(LetSeeInterceptor(letSee))
            .build()

        val responderJob = launch(Dispatchers.Default) {
            val stack = letSee.requestsManager.requestsStack.first { it.isNotEmpty() }
            letSee.requestsManager.respond(stack.first().request, mock)
        }

        val response = withContext(Dispatchers.IO) {
            client.newCall(
                okhttp3.Request.Builder().url("http://localhost/api/data").build()
            ).execute()
        }

        responderJob.join()

        assertEquals(200, response.code)
        assertEquals(expectedJson, response.body?.string())
        assertEquals("application/json", response.header("Content-Type"))
    }

    // ── (d) Concurrent requests ─────────────────────────────────────────────

    @Test
    fun test_concurrent_requests_no_race() = runTest {
        enableMocks()
        val client = OkHttpClient.Builder()
            .addInterceptor(LetSeeInterceptor(letSee))
            .build()

        val respondedCount = AtomicInteger(0)

        val responderJob = launch(Dispatchers.Default) {
            val stack = letSee.requestsManager.requestsStack.first { it.size >= 3 }
            for (accepted in stack) {
                letSee.requestsManager.respond(
                    accepted.request,
                    Mock.defaultSuccess("concurrent", """{"ok":true}""".toByteArray())
                )
                respondedCount.incrementAndGet()
            }
        }

        val deferreds = (1..3).map { i ->
            async(Dispatchers.IO) {
                client.newCall(
                    okhttp3.Request.Builder().url("http://localhost/api/item/$i").build()
                ).execute()
            }
        }

        val responses = deferreds.map { it.await() }
        responderJob.join()

        assertEquals(3, responses.size)
        responses.forEach { assertEquals(200, it.code) }
        assertEquals(3, respondedCount.get())
    }

    // ── (e) Timeout ─────────────────────────────────────────────────────────

    @Test
    fun test_timeout_throws_ioexception() = runTest {
        enableMocks()
        val client = OkHttpClient.Builder()
            .addInterceptor(LetSeeInterceptor(letSee, mockSelectionTimeoutMs = 100))
            .build()

        val exception = withContext(Dispatchers.IO) {
            assertFailsWith<IOException> {
                client.newCall(
                    okhttp3.Request.Builder().url("http://localhost/api/slow").build()
                ).execute()
            }
        }

        assertTrue(exception.message?.contains("timed out") == true)
    }

    // ── (f) Live forwarding via MockWebServer ───────────────────────────────

    @Test
    fun test_live_response_reaches_server() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("""{"from":"server"}""").setResponseCode(200))
        server.start()

        try {
            enableMocks()
            configureLetSeeLiveHandler(letSee)

            val client = OkHttpClient.Builder()
                .addInterceptor(LetSeeInterceptor(letSee))
                .build()

            val responderJob = launch(Dispatchers.Default) {
                val stack = letSee.requestsManager.requestsStack.first { it.isNotEmpty() }
                letSee.requestsManager.respond(stack.first().request, Mock.LIVE)
            }

            val response = withContext(Dispatchers.IO) {
                client.newCall(
                    okhttp3.Request.Builder().url(server.url("/api/live-test")).build()
                ).execute()
            }

            responderJob.join()

            assertEquals(200, response.code)
            assertEquals(1, server.requestCount)
            val recorded = server.takeRequest(1, TimeUnit.SECONDS)
            assertNotNull(recorded)
            assertEquals("/api/live-test", recorded.path)
        } finally {
            server.shutdown()
        }
    }
}
