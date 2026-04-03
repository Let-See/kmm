package io.github.letsee

import io.github.letsee.MockImplementations.BaseUnitTest
import io.github.letsee.MockImplementations.MockDirectoryFilesFetcher
import io.github.letsee.MockImplementations.MockFileNameCleaner
import io.github.letsee.MockImplementations.MockFileNameProcessor
import io.github.letsee.MockImplementations.MockMockProcessor
import io.github.letsee.implementations.DefaultGlobalMockDirectoryConfiguration
import io.github.letsee.implementations.DefaultMocksDirectoryProcessor
import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.implementations.GlobalMockDirectoryConfiguration
import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
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

private fun buildOrderingProcessor(
    files: Map<String, List<String>>,
    fileInfos: List<MockFileInformation>,
    mockResults: List<Mock>
): Pair<DefaultMocksDirectoryProcessor, String> {
    val cleaner = MockFileNameCleaner()
    val fnProcessor = MockFileNameProcessor(cleaner, result = fileInfos)
    val dirFetcher = MockDirectoryFilesFetcher(result = files)
    val mockProcessor = MockMockProcessor(fnProcessor, results = mockResults)
    val config = DefaultGlobalMockDirectoryConfiguration(emptyList())
    val sut = DefaultMocksDirectoryProcessor(fnProcessor, mockProcessor, dirFetcher) { config }
    return sut to files.keys.minOrNull()!!
}

class MocksOrderingTests {

    private val path = "/ordering-test/"
    private val subPath = "${path}api/"
    private val dummyInfo = MockFileInformation(
        "${path}.ls.global.json", null, null,
        MockFileInformation.MockStatus.SUCCESS, ".ls.global.json", null
    )
    private val zInfo = MockFileInformation(
        "${subPath}z-mock.json", null, null,
        MockFileInformation.MockStatus.SUCCESS, "z-mock.json", null
    )
    private val aInfo = MockFileInformation(
        "${subPath}a-mock.json", null, null,
        MockFileInformation.MockStatus.SUCCESS, "a-mock.json", null
    )
    private val mInfo = MockFileInformation(
        "${subPath}M-mock.json", null, null,
        MockFileInformation.MockStatus.SUCCESS, "M-mock.json", null
    )
    private val zMock = Mock.SUCCESS("z-mock", DefaultResponse(200u, 200u, null, null, null, emptyMap()), zInfo)
    private val aMock = Mock.SUCCESS("a-mock", DefaultResponse(200u, 200u, null, null, null, emptyMap()), aInfo)
    private val mMock = Mock.SUCCESS("M-mock", DefaultResponse(200u, 200u, null, null, null, emptyMap()), mInfo)

    private val files = mapOf(
        path to listOf(".ls.global.json"),
        subPath to listOf("z-mock.json", "a-mock.json", "M-mock.json")
    )
    private val fileInfos = listOf(dummyInfo, zInfo, aInfo, mInfo)
    private val mockResults = listOf(zMock, aMock, mMock)

    @Test
    fun `mock list is sorted alphabetically case-insensitive - z a M becomes a M z`() {
        val (sut, rootPath) = buildOrderingProcessor(files, fileInfos, mockResults)
        val result = sut.process(rootPath)

        assertEquals(1, result.size)
        val mocks = result.values.first()
        assertEquals(3, mocks.size)
        assertEquals("a-mock", mocks[0].name)
        assertEquals("M-mock", mocks[1].name)
        assertEquals("z-mock", mocks[2].name)
    }

    @Test
    fun `keys are sorted alphabetically case-insensitive across multiple directories`() {
        val rootPath = "/ordering-test/"
        val zDir = "${rootPath}Zebra/"
        val aDir = "${rootPath}alpha/"
        val mDir = "${rootPath}Mango/"

        val globalInfo = MockFileInformation(
            "${rootPath}.ls.global.json", null, null,
            MockFileInformation.MockStatus.SUCCESS, ".ls.global.json", null
        )
        val zFileInfo = MockFileInformation(
            "${zDir}success.json", null, null,
            MockFileInformation.MockStatus.SUCCESS, "success.json", null
        )
        val aFileInfo = MockFileInformation(
            "${aDir}success.json", null, null,
            MockFileInformation.MockStatus.SUCCESS, "success.json", null
        )
        val mFileInfo = MockFileInformation(
            "${mDir}success.json", null, null,
            MockFileInformation.MockStatus.SUCCESS, "success.json", null
        )

        val zMock = Mock.SUCCESS("z-success", DefaultResponse(200u, 200u, null, null, null, emptyMap()), zFileInfo)
        val aMock = Mock.SUCCESS("a-success", DefaultResponse(200u, 200u, null, null, null, emptyMap()), aFileInfo)
        val mMock = Mock.SUCCESS("m-success", DefaultResponse(200u, 200u, null, null, null, emptyMap()), mFileInfo)

        val files = mapOf(
            rootPath to listOf(".ls.global.json"),
            zDir to listOf("success.json"),
            aDir to listOf("success.json"),
            mDir to listOf("success.json")
        )
        val fileInfos = listOf(globalInfo, zFileInfo, aFileInfo, mFileInfo)
        val mockResults = listOf(zMock, aMock, mMock)

        val (sut, root) = buildOrderingProcessor(files, fileInfos, mockResults)
        val result = sut.process(root)

        assertEquals(3, result.size)
        val keys = result.keys.toList()
        assertEquals(keys, keys.sortedBy { it.lowercase() },
            "Keys should be sorted alphabetically (case-insensitive), got: $keys")
    }

    @Test
    fun `processing same directory twice produces identical mock order`() {
        val (sut1, rootPath1) = buildOrderingProcessor(files, fileInfos, mockResults)
        val (sut2, rootPath2) = buildOrderingProcessor(files, fileInfos, mockResults)

        val result1 = sut1.process(rootPath1)
        val result2 = sut2.process(rootPath2)

        assertEquals(result1.keys.toList(), result2.keys.toList())
        result1.forEach { (key, mocks1) ->
            val mocks2 = result2[key] ?: error("key $key missing in second run")
            assertEquals(
                mocks1.map { it.name },
                mocks2.map { it.name },
                "Mock order for key '$key' must be identical across runs"
            )
        }
    }
}
