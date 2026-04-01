package io.github.letsee

import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MockTest {

    private fun successMock(name: String, json: String = "{}") = Mock.SUCCESS(
        name = name,
        response = DefaultResponse(200u, 200u, json.encodeToByteArray(), null, null, emptyMap()),
        fileInformation = MockFileInformation(
            rawPath = "",
            statusCode = 200u,
            delay = null,
            status = MockFileInformation.MockStatus.SUCCESS,
            displayName = name,
            relativePath = null
        )
    )

    private fun failureMock(name: String, json: String = "{}") = Mock.FAILURE(
        name = name,
        response = DefaultResponse(400u, 400u, json.encodeToByteArray(), null, null, emptyMap()),
        fileInformation = MockFileInformation(
            rawPath = "",
            statusCode = 400u,
            delay = null,
            status = MockFileInformation.MockStatus.FAILURE,
            displayName = name,
            relativePath = null
        )
    )

    // ─── Comparable / Sorting ────────────────────────────────────────────────

    @Test
    fun `sorted list follows FAILURE then ERROR then SUCCESS then LIVE then CANCEL order`() {
        val successB = successMock("B success")
        val successA = successMock("A success")
        val failureZ = failureMock("Z failure")
        val failureA = failureMock("A failure")
        val errorX = Mock.ERROR("X error", RuntimeException("err"))

        val sorted = listOf(Mock.LIVE, Mock.CANCEL, successB, successA, failureZ, failureA, errorX).sorted()

        assertEquals(failureA, sorted[0], "first: failureA")
        assertEquals(failureZ, sorted[1], "second: failureZ")
        assertEquals(errorX, sorted[2], "third: errorX")
        assertEquals(successA, sorted[3], "fourth: successA")
        assertEquals(successB, sorted[4], "fifth: successB")
        assertEquals(Mock.LIVE, sorted[5], "sixth: LIVE")
        assertEquals(Mock.CANCEL, sorted[6], "seventh: CANCEL")
    }

    @Test
    fun `same type mocks are sorted case-insensitively by name`() {
        val mockZ = successMock("Zebra")
        val mockA = successMock("apple")
        val mockM = successMock("Mango")

        val sorted = listOf(mockZ, mockM, mockA).sorted()

        assertEquals("apple", sorted[0].name)
        assertEquals("Mango", sorted[1].name)
        assertEquals("Zebra", sorted[2].name)
    }

    // ─── Factory methods ─────────────────────────────────────────────────────

    @Test
    fun `defaultSuccess creates SUCCESS with responseCode 200 and Content-Type header`() {
        val data = """{"key":"value"}""".encodeToByteArray()
        val mock = Mock.defaultSuccess("Test Success", data)

        assertEquals(200u, mock.response.responseCode)
        assertEquals(200u, mock.response.requestCode)
        assertEquals(data.toList(), mock.response.byteResponse?.toList())
        assertEquals(listOf("application/json"), mock.response.headers["Content-Type"])
        assertEquals("Test Success", mock.name)
        assertEquals(MockFileInformation.MockStatus.SUCCESS, mock.fileInformation.status)
        assertEquals(200u, mock.fileInformation.statusCode)
        assertEquals("Test Success", mock.fileInformation.displayName)
    }

    @Test
    fun `defaultFailure creates FAILURE with responseCode 400`() {
        val data = """{"error":"bad"}""".encodeToByteArray()
        val mock = Mock.defaultFailure("Test Failure", data)

        assertEquals(400u, mock.response.responseCode)
        assertEquals(400u, mock.response.requestCode)
        assertEquals(data.toList(), mock.response.byteResponse?.toList())
        assertEquals("Test Failure", mock.name)
        assertEquals(MockFileInformation.MockStatus.FAILURE, mock.fileInformation.status)
        assertEquals(400u, mock.fileInformation.statusCode)
        assertEquals("Test Failure", mock.fileInformation.displayName)
    }

    // ─── displayName ─────────────────────────────────────────────────────────

    @Test
    fun `displayName returns Live for LIVE`() {
        assertEquals("Live", Mock.LIVE.displayName)
    }

    @Test
    fun `displayName returns Cancel for CANCEL`() {
        assertEquals("Cancel", Mock.CANCEL.displayName)
    }

    @Test
    fun `displayName returns name property for SUCCESS`() {
        assertEquals("my-success", successMock("my-success").displayName)
    }

    @Test
    fun `displayName returns name property for FAILURE`() {
        assertEquals("my-failure", failureMock("my-failure").displayName)
    }

    @Test
    fun `displayName returns name property for ERROR`() {
        val mock = Mock.ERROR("my-error", RuntimeException())
        assertEquals("my-error", mock.displayName)
    }

    // ─── formatted ───────────────────────────────────────────────────────────

    @Test
    fun `formatted returns pretty-printed JSON for valid JSON in SUCCESS`() {
        val mock = Mock.defaultSuccess("Test", """{"key":"value"}""".encodeToByteArray())
        val formatted = mock.formatted

        assertNotNull(formatted, "formatted should not be null for SUCCESS")
        assertTrue(formatted.contains("\"key\""), "formatted should contain the key")
        assertTrue(formatted.contains("\"value\""), "formatted should contain the value")
        assertTrue(formatted.contains("\n"), "pretty-printed JSON should contain newlines")
    }

    @Test
    fun `formatted returns pretty-printed JSON for valid JSON in FAILURE`() {
        val mock = Mock.defaultFailure("Test", """{"error":"not found"}""".encodeToByteArray())
        val formatted = mock.formatted

        assertNotNull(formatted, "formatted should not be null for FAILURE")
        assertTrue(formatted.contains("\"error\""), "formatted should contain the error key")
        assertTrue(formatted.contains("\n"), "pretty-printed JSON should contain newlines")
    }

    @Test
    fun `formatted returns raw string for non-JSON data`() {
        val mock = Mock.defaultSuccess("Test", "not json at all".encodeToByteArray())
        assertEquals("not json at all", mock.formatted)
    }

    @Test
    fun `formatted returns null for LIVE`() {
        assertNull(Mock.LIVE.formatted)
    }

    @Test
    fun `formatted returns null for CANCEL`() {
        assertNull(Mock.CANCEL.formatted)
    }

    @Test
    fun `formatted returns null for ERROR`() {
        assertNull(Mock.ERROR("Test", RuntimeException()).formatted)
    }

    @Test
    fun `formatted returns null for SUCCESS with null byteResponse`() {
        val mock = Mock.SUCCESS(
            name = "null-bytes",
            response = DefaultResponse(200u, 200u, null, null, null, emptyMap()),
            fileInformation = MockFileInformation(
                rawPath = "",
                statusCode = 200u,
                delay = null,
                status = MockFileInformation.MockStatus.SUCCESS,
                displayName = "null-bytes",
                relativePath = null
            )
        )
        assertNull(mock.formatted)
    }
}
