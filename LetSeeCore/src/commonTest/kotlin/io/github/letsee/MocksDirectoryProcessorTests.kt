package io.github.letsee

import io.github.letsee.MockImplementations.BaseUnitTest
import io.github.letsee.MockImplementations.MockDirectoryFilesFetcher
import io.github.letsee.MockImplementations.MockFileNameCleaner
import io.github.letsee.MockImplementations.MockFileNameProcessor
import io.github.letsee.MockImplementations.MockMockProcessor
import io.github.letsee.implementations.DefaultGlobalMockDirectoryConfiguration
import io.github.letsee.implementations.DefaultMocksDirectoryProcessor
import io.github.letsee.implementations.GlobalMockDirectoryConfiguration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MocksDirectoryProcessorTests: BaseUnitTest() {
    private val defaultGlobalMockDirectoryConfiguration = DefaultGlobalMockDirectoryConfiguration(listOf(DefaultGlobalMockDirectoryConfiguration.Map("/inside/","https://google.com/api/v2"),
         DefaultGlobalMockDirectoryConfiguration.Map("/someOther/","https://apple.com/api/v2")))
    private var sut: DefaultMocksDirectoryProcessor? = null
    private val fileNameCleaner = MockFileNameCleaner()
    private val fileNameProcessor = MockFileNameProcessor(fileNameCleaner)
    private val directoryFileFetcher = MockDirectoryFilesFetcher()
    private val mockProcessing = MockMockProcessor(fileNameProcessor)
    @BeforeTest
    fun setUp() {
        sut = DefaultMocksDirectoryProcessor(fileNameProcessor, mockProcessing, directoryFileFetcher, globalMockDirectoryConfig = { defaultGlobalMockDirectoryConfiguration })
    }

    @AfterTest
    fun tearDown() {
        sut = null
    }

    /*
        Mocks
            .ls.global.json
            /Inside/
                some_mocks.json
                some_mocks.json
                some_mocks.json
                /Inside2/
                    some_mocks.json
                    some_mocks.json
                    some_mocks.json
     */
    @Test
    fun `test Process Function Should Return Correct Mock File Information With correct path from global config file`() {
        val fileName = M.Strings.PATH
        directoryFileFetcher.result = mapOf(Pair(fileName, listOf(GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME)),
            Pair("${fileName}/inside/", listOf(M.Strings.SUCCESS_FILE_NAME_EXPLICITLY, M.Strings.FAILURE_FILE_NAME_IMPLICITLY, M.Strings.SUCCESS_FILE_NAME_EXPLICITLY)),
            Pair("${fileName}/inside/inside2/", listOf(M.Strings.SUCCESS_FILE_NAME_EXPLICITLY, M.Strings.FAILURE_FILE_NAME_IMPLICITLY, M.Strings.SUCCESS_FILE_NAME_EXPLICITLY)),
            Pair("${fileName}/someOther/", listOf(M.Strings.SUCCESS_FILE_NAME_EXPLICITLY, M.Strings.FAILURE_FILE_NAME_IMPLICITLY, M.Strings.SUCCESS_FILE_NAME_EXPLICITLY)),
        )
        fileNameProcessor.result = listOf(
            M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.FAILURE_MOCK_INFORMATION, M.Objects.SUCCESS_MOCK_INFORMATION,
            M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.FAILURE_MOCK_INFORMATION, M.Objects.SUCCESS_MOCK_INFORMATION,
            M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.FAILURE_MOCK_INFORMATION, M.Objects.SUCCESS_MOCK_INFORMATION
            )
        val result = sut!!.process(fileName)
        val firstExpectedKey = defaultGlobalMockDirectoryConfiguration.maps.first().to + "/inside/"
        assertEquals(3, result.size)
        assertEquals(firstExpectedKey, result.keys.first())
        assertNotNull( result[firstExpectedKey])
        assertEquals(3, result[firstExpectedKey]?.size)
        assertEquals(defaultGlobalMockDirectoryConfiguration.maps.first().to + "/inside/inside2/", result.keys.drop(1).first())
        assertEquals(defaultGlobalMockDirectoryConfiguration.maps.drop(1).first().to + "/someother/", result.keys.drop(2).first())
    }
}


