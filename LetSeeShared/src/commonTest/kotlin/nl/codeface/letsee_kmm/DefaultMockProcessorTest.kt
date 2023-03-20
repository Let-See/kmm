package nl.codeface.letsee_kmm

import nl.codeface.letsee_kmm.MockImplementations.MockFileDataFetcher
import nl.codeface.letsee_kmm.implementations.DefaultMockProcessor
import nl.codeface.letsee_kmm.implementations.DefaultResponse
import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DefaultMockProcessorTest {
    private lateinit var sut: DefaultMockProcessor
    private val fileDataFetcher = MockFileDataFetcher()
    @BeforeTest
    fun setUp() {
        sut = DefaultMockProcessor(fileDataFetcher)
    }

    @Test
    fun `test process file information and make correct mock when success`() {
        val fileInformation = MockFileInformation(
            M.Strings.IOS_FileURI_PATH+"/"+ M.Strings.SUCCESS_FILE_NAME_EXPLICITLY,
        200u, 200, MockFileInformation.MockStatus.SUCCESS, "Some Mock", null)
        val data = byteArrayOf()
        fileDataFetcher.result = data
        val result = sut.process(fileInformation)
        val expectedResult = Mock.SUCCESS(fileInformation.displayName, DefaultResponse(fileInformation.statusCode!!, 200u, data, null,
        null, emptyMap()), fileInformation)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `test process file information and make correct mock when failure`() {
        val fileInformation = MockFileInformation(
            M.Strings.IOS_FileURI_PATH+"/"+ M.Strings.FAILURE_FILE_NAME_IMPLICITLY,
            400u, 200, MockFileInformation.MockStatus.FAILURE, "Some Mock", null)
        val data = byteArrayOf()
        fileDataFetcher.result = data
        val result = sut.process(fileInformation)
        val expectedResult = Mock.FAILURE(fileInformation.displayName, DefaultResponse(fileInformation.statusCode!!, 400u, data, null,
            null, emptyMap()), fileInformation)
        assertEquals(expectedResult, result)
    }

    @Test
    fun testProcess() {
    }
}