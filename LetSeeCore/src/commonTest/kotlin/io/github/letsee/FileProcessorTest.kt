package io.github.letsee

import io.github.letsee.MockImplementations.MockFileNameCleaner
import io.github.letsee.implementations.JSONFileNameProcessor
import io.github.letsee.models.MockFileInformation
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileProcessorTest {
    private val fileCleaner = MockFileNameCleaner()
    private lateinit var sut: JSONFileNameProcessor

    @BeforeTest
    fun setUp() {
        sut = JSONFileNameProcessor(fileCleaner)
    }

    @Test
    fun `test Process Function Should Return Correct Mock File Information File Name Should Be Correct`() {
        val fileName = "some_22_someTestFileName.json"
        fileCleaner.result = fileName
        val result = sut.process(fileName)
        val expected = MockFileInformation(fileName, null,null,
            MockFileInformation.MockStatus.SUCCESS, fileName, relativePath = "")

        assertEquals(expected.rawPath, result.rawPath)
    }

    @Test
    fun `test Process Function Should Return Correct Mock File Information Display Name Should Be Correct`() {
        val fileName = "some_x_someTestFileName.json"
        fileCleaner.result = fileName.removeSuffix(".json")
        val result = sut.process(fileName)
        val expected = MockFileInformation(fileName, null,null,
            MockFileInformation.MockStatus.SUCCESS, "Some_x_someTestFileName", relativePath = "")

        assertEquals(expected.displayName, result.displayName)
    }
//
//    @Test
//    fun `test process file name with valid status code and delay`() {
//        val filePath = "200_500ms_mock.json"
//        val expected = MockFileInformation(
//            filePath,
//            statusCode = 200,
//            delay = 500,
//            status = MockFileInformation.MockStatus.SUCCESS,
//            displayName = "200_500ms_mock"
//        )
//
//        val actual = sut!!.process(filePath)
//
//        assertEquals(expected, actual)
//    }
//
//    @Test
//    fun `test process file name with valid delay but no status code`() {
//        val filePath = "1000ms_mock.json"
//        val expected = MockFileInformation(
//            filePath,
//            statusCode = null,
//            delay = 1000,
//            status = MockFileInformation.MockStatus.SUCCESS,
//            displayName = "1000ms_mock"
//        )
//
//        val actual = sut!!.process(filePath)
//
//        assertEquals(expected, actual)
//    }
//
//    @Test
//    fun `test process file name with invalid status code and delay`() {
//        val filePath = "mock_bad_status_code_bad_delay.json"
//        val expected = MockFileInformation(
//            filePath,
//            statusCode = null,
//            delay = null,
//            status = MockFileInformation.MockStatus.SUCCESS,
//            displayName = "Mock_bad_status_code_bad_delay"
//        )
//
//        val actual = sut!!.process(filePath)
//
//        assertEquals(expected, actual)
//    }
//
//    @Test
//    fun testProcessEdgeCases() {
//        // File path with only underscores
//        val underscoreFilePath = "______"
//        val expectedUnderscore = MockFileInformation("______", null, null, MockFileInformation.MockStatus.SUCCESS, "______")
//        assertEquals(expectedUnderscore, sut!!.process(underscoreFilePath))
//
//        // File path with no delay or status code
//        val noDelayOrStatusFilePath = "fileName.json"
//        val expectedNoDelayOrStatus = MockFileInformation("fileName.json", null, null, MockFileInformation.MockStatus.SUCCESS, "FileName")
//        assertEquals(expectedNoDelayOrStatus, sut!!.process(noDelayOrStatusFilePath))
//
//        // File path with delay only
//        val delayOnlyFilePath = "5ms_file.json"
//        val expectedDelayOnly = MockFileInformation(delayOnlyFilePath, null, 5, MockFileInformation.MockStatus.SUCCESS, "File")
//        assertEquals(expectedDelayOnly, sut!!.process(delayOnlyFilePath))
//
//        // File path with status code only
//        val statusCodeOnlyFilePath = "error.json"
//        val expectedStatusCodeOnly = MockFileInformation(statusCodeOnlyFilePath, null, null, MockFileInformation.MockStatus.FAILURE, "Error")
//        assertEquals(expectedStatusCodeOnly, sut!!.process(statusCodeOnlyFilePath))
//
//        // File path with delay and status code
//        val delayAndStatusCodeFilePath = "404_10ms_file.json"
//        val expectedDelayAndStatusCode = MockFileInformation(delayAndStatusCodeFilePath, 404, 10, MockFileInformation.MockStatus.SUCCESS, "File")
//        assertEquals(expectedDelayAndStatusCode, sut!!.process(delayAndStatusCodeFilePath))
//
//        // File path with invalid delay format
//        val invalidDelayFilePath = "5s_file.json"
//        assertFails { sut!!.process(invalidDelayFilePath) }
//    }
}