package io.github.letsee

import io.github.letsee.MockImplementations.MockDirectoryFilesFetcher
import io.github.letsee.MockImplementations.MockFileDataFetcher
import io.github.letsee.MockImplementations.MockFileNameCleaner
import io.github.letsee.MockImplementations.MockFileNameProcessor
import io.github.letsee.MockImplementations.MockMockProcessor
import io.github.letsee.implementations.DefaultGlobalMockDirectoryConfiguration
import io.github.letsee.implementations.DefaultMocksDirectoryProcessor
import io.github.letsee.implementations.GlobalMockDirectoryConfiguration
import io.github.letsee.models.MockFileInformation
import io.github.letsee.models.PathConfig
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PathConfigSerializationTest {

    @Test
    fun `PathConfig deserializes from JSON correctly`() {
        val json = """{"path": "/api/v2/users"}"""
        val result = Json.decodeFromString<PathConfig>(json)
        assertEquals("/api/v2/users", result.path)
    }

    @Test
    fun `PathConfig serializes to JSON correctly`() {
        val config = PathConfig(path = "/api/v2/users")
        val json = Json.encodeToString(PathConfig.serializer(), config)
        val decoded = Json.decodeFromString<PathConfig>(json)
        assertEquals(config.path, decoded.path)
    }
}

class PathConfigDirectoryProcessorTest {

    private val path = "/mocks/"
    private val subPath = "${path}users/"

    private fun buildSut(
        dirResult: Map<String, List<String>>,
        fileInfos: List<MockFileInformation>,
        fileDataFetcher: MockFileDataFetcher? = null,
        globalConfig: DefaultGlobalMockDirectoryConfiguration = DefaultGlobalMockDirectoryConfiguration(emptyList())
    ): DefaultMocksDirectoryProcessor {
        val cleaner = MockFileNameCleaner()
        val fnProcessor = MockFileNameProcessor(cleaner, result = fileInfos)
        val dirFetcher = MockDirectoryFilesFetcher(result = dirResult)
        val mockProcessor = MockMockProcessor(fnProcessor)
        return DefaultMocksDirectoryProcessor(
            fnProcessor, mockProcessor, dirFetcher,
            fileDataFetcher = fileDataFetcher
        ) { globalConfig }
    }

    @Test
    fun `directory processor uses pathconfigs json path as key prefix`() {
        val pathConfigContent = """{"path": "/api/v2"}"""
        val fileDataFetcher = MockFileDataFetcher(result = pathConfigContent.encodeToByteArray())

        val dirResult = mapOf(
            path to listOf("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}"),
            subPath to listOf("${subPath}.pathconfigs.json", "${subPath}success.json")
        )
        val fileInfos = listOf(
            MockFileInformation("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}", null, null, MockFileInformation.MockStatus.SUCCESS, GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME, null),
            MockFileInformation("${subPath}.pathconfigs.json", null, null, MockFileInformation.MockStatus.SUCCESS, ".pathconfigs.json", null),
            MockFileInformation("${subPath}success.json", null, null, MockFileInformation.MockStatus.SUCCESS, "success.json", null),
        )

        val sut = buildSut(dirResult, fileInfos, fileDataFetcher)
        val result = sut.process(path)

        assertEquals(1, result.size)
        val key = result.keys.first()
        assertTrue(key.startsWith("/api/v2"), "Expected key to start with /api/v2, got: $key")
        assertEquals("/api/v2/users/", key)
    }

    @Test
    fun `directory processor filters out pathconfigs json from mock file list`() {
        val pathConfigContent = """{"path": "/api/v2"}"""
        val fileDataFetcher = MockFileDataFetcher(result = pathConfigContent.encodeToByteArray())

        val dirResult = mapOf(
            path to listOf("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}"),
            subPath to listOf("${subPath}.pathconfigs.json", "${subPath}success.json", "${subPath}failure.json")
        )
        val fileInfos = listOf(
            MockFileInformation("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}", null, null, MockFileInformation.MockStatus.SUCCESS, GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME, null),
            MockFileInformation("${subPath}.pathconfigs.json", null, null, MockFileInformation.MockStatus.SUCCESS, ".pathconfigs.json", null),
            MockFileInformation("${subPath}success.json", null, null, MockFileInformation.MockStatus.SUCCESS, "success.json", null),
            MockFileInformation("${subPath}failure.json", null, null, MockFileInformation.MockStatus.FAILURE, "failure.json", null),
        )

        val sut = buildSut(dirResult, fileInfos, fileDataFetcher)
        val result = sut.process(path)

        assertEquals(1, result.size)
        // Only the 2 mock files should be present, not the .pathconfigs.json
        assertEquals(2, result.values.first().size)
    }

    @Test
    fun `directory processor with global config override - global wins over pathconfigs json`() {
        val pathConfigContent = """{"path": "/api/v2"}"""
        val fileDataFetcher = MockFileDataFetcher(result = pathConfigContent.encodeToByteArray())

        val dirResult = mapOf(
            path to listOf("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}"),
            subPath to listOf("${subPath}.pathconfigs.json", "${subPath}success.json")
        )
        val fileInfos = listOf(
            MockFileInformation("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}", null, null, MockFileInformation.MockStatus.SUCCESS, GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME, null),
            MockFileInformation("${subPath}.pathconfigs.json", null, null, MockFileInformation.MockStatus.SUCCESS, ".pathconfigs.json", null),
            MockFileInformation("${subPath}success.json", null, null, MockFileInformation.MockStatus.SUCCESS, "success.json", null),
        )
        val globalConfig = DefaultGlobalMockDirectoryConfiguration(
            listOf(DefaultGlobalMockDirectoryConfiguration.Map("/users/", "https://api.example.com"))
        )

        val sut = buildSut(dirResult, fileInfos, fileDataFetcher, globalConfig)
        val result = sut.process(path)

        assertEquals(1, result.size)
        val key = result.keys.first()
        assertTrue(key.startsWith("https://api.example.com"), "Expected global override to win, got: $key")
    }

    @Test
    fun `directory processor without pathconfigs json uses relative path as key`() {
        val dirResult = mapOf(
            path to listOf("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}"),
            subPath to listOf("${subPath}success.json")
        )
        val fileInfos = listOf(
            MockFileInformation("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}", null, null, MockFileInformation.MockStatus.SUCCESS, GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME, null),
            MockFileInformation("${subPath}success.json", null, null, MockFileInformation.MockStatus.SUCCESS, "success.json", null),
        )

        val sut = buildSut(dirResult, fileInfos)
        val result = sut.process(path)

        assertEquals(1, result.size)
        val key = result.keys.first()
        assertEquals("/users/", key)
    }

    @Test
    fun `processing same directory twice produces identical ordering`() {
        val dirResult = mapOf(
            path to listOf("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}"),
            subPath to listOf("${subPath}success.json", "${subPath}failure.json")
        )
        val fileInfos = listOf(
            MockFileInformation("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}", null, null, MockFileInformation.MockStatus.SUCCESS, GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME, null),
            MockFileInformation("${subPath}success.json", null, null, MockFileInformation.MockStatus.SUCCESS, "success.json", null),
            MockFileInformation("${subPath}failure.json", null, null, MockFileInformation.MockStatus.FAILURE, "failure.json", null),
        )

        val sut = buildSut(dirResult, fileInfos)
        val first = sut.process(path)
        val second = sut.process(path)

        assertEquals(first.keys.toList(), second.keys.toList(), "Keys must be identical across runs")
        first.forEach { (key, mocks) ->
            assertEquals(mocks.map { it.name }, second[key]?.map { it.name }, "Mock order for '$key' must be identical")
        }
    }

    @Test
    fun `directory processor with null fileDataFetcher ignores pathconfigs json`() {
        // No fileDataFetcher provided — .pathconfigs.json present but cannot be read → falls back to relative path
        val dirResult = mapOf(
            path to listOf("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}"),
            subPath to listOf("${subPath}.pathconfigs.json", "${subPath}success.json")
        )
        val fileInfos = listOf(
            MockFileInformation("${path}${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}", null, null, MockFileInformation.MockStatus.SUCCESS, GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME, null),
            MockFileInformation("${subPath}.pathconfigs.json", null, null, MockFileInformation.MockStatus.SUCCESS, ".pathconfigs.json", null),
            MockFileInformation("${subPath}success.json", null, null, MockFileInformation.MockStatus.SUCCESS, "success.json", null),
        )

        val sut = buildSut(dirResult, fileInfos, fileDataFetcher = null)
        val result = sut.process(path)

        assertEquals(1, result.size)
        val key = result.keys.first()
        assertEquals("/users/", key)
    }
}
