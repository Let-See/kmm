package io.github.letsee.implementations

import io.github.letsee.interfaces.FileDataFetcher
import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.models.PathConfig
import io.github.letsee.interfaces.DirectoryFilesFetcher
import io.github.letsee.interfaces.DirectoryProcessor
import io.github.letsee.interfaces.FileNameProcessor
import io.github.letsee.interfaces.MockProcessor
import kotlinx.serialization.json.Json

private const val PATH_CONFIGS_FILE_NAME = ".pathconfigs.json"

class DefaultMocksDirectoryProcessor(
    private val fileNameProcessor: FileNameProcessor<MockFileInformation>,
    private val mockProcessor: MockProcessor<MockFileInformation>,
    private val directoryFilesFetcher: DirectoryFilesFetcher,
    private val fileDataFetcher: FileDataFetcher? = null,
    private val globalMockDirectoryConfig: (()->GlobalMockDirectoryConfiguration?)? = null,
    ) : DirectoryProcessor<Mock> {

    override fun process(path: String): Map<String, List<Mock>> {
        val files = directoryFilesFetcher.getFiles(path, fileType = "json")
        val filesInformation = files.mapValues { it.value.map { address -> this.fileNameProcessor.process(address) }  }.toMutableMap()
        val result: LinkedHashMap<String, List<MockFileInformation>> = linkedMapOf()

        if(filesInformation.isEmpty()) {
            return  emptyMap()
        }

        val orderedItem = filesInformation.keys.sortedBy { it.lowercase() }.toMutableList()
        val globalConfigs = globalMockDirectoryConfig?.let { it() } ?: GlobalMockDirectoryConfiguration.exists(inDirectory = orderedItem.first())

        // if globalConfigs is available it means that this folder should be the main folder and no file should be inside it,
        // so we can remove it from the results
        if (globalConfigs != null) {
            orderedItem.removeAt(0)
        }

        orderedItem.forEach { key ->
            val allFilesInsideDirectory = filesInformation[key] ?: return emptyMap()

            // Filter out .pathconfigs.json — it is a config file, not a mock
            val filesInsideDirectory = allFilesInsideDirectory.filter {
                !it.rawPath.endsWith(PATH_CONFIGS_FILE_NAME)
            }

            val relativePath = this.makeRelativePath(forPath = key, relativeTo = path).mockKeyNormalised()
            val overriddenPath: String? = globalConfigs?.hasMap(forRelativePth = relativePath)?.to

            // Resolve per-directory path prefix from .pathconfigs.json (only when no global override)
            val pathConfigPrefix: String? = if (overriddenPath == null) {
                val pathConfigFile = allFilesInsideDirectory.find { it.rawPath.endsWith(PATH_CONFIGS_FILE_NAME) }
                pathConfigFile?.let { fileInfo ->
                    runCatching {
                        val content = fileDataFetcher?.getFileData(fileInfo.rawPath)
                        content?.let {
                            Json.decodeFromString<PathConfig>(it.decodeToString()).path
                        }
                    }.onFailure { e ->
                        println("[DefaultMocksDirectoryProcessor] Failed to parse ${fileInfo.rawPath}: ${e.message}")
                    }.getOrNull()
                }
            } else null

            val fileInformation: List<MockFileInformation> = filesInsideDirectory.map { file ->
                    val relPath = this.makeRelativePath(
                        forPath = file.rawPath,
                        relativeTo = path
                    )
                    file.copy(relativePath = relPath)
            }
            val actualKey = when {
                overriddenPath != null -> overriddenPath + relativePath
                pathConfigPrefix != null -> pathConfigPrefix.trimEnd('/') + relativePath
                else -> relativePath
            }
            result[actualKey] = fileInformation
        }

        val mocks = result.mapValues { entry ->
            entry.value
                .map { info -> this.mockProcessor.process(info) }
                .sortedBy { it.name.lowercase() }
        }

        return mocks
    }

    private fun makeRelativePath(forPath: String, relativeTo: String): String {
        return forPath.replace(relativeTo, "").lowercase()
    }
}

fun String.mockKeyNormalised(): String {
    var folder = this.lowercase()
    folder = if (folder.startsWith("/")) folder else "/$folder"
    folder = if (folder.endsWith("/"))  folder else "$folder/"
    return folder
}