package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.Mock
import nl.codeface.letsee_kmm.MockFileInformation
import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import nl.codeface.letsee_kmm.interfaces.DirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.interfaces.MockProcessor

class MocksDirectoryProcessor(
    private val fileNameProcessor: FileNameProcessor<MockFileInformation>,
    private val mockProcessor: MockProcessor<MockFileInformation>,
    private val directoryFilesFetcher: DirectoryFilesFetcher,
    private val globalMockDirectoryConfig: GlobalMockDirectoryConfiguration? = null,
    ) : DirectoryProcessor<Mock> {

    override fun process(path: String): Map<String, List<Mock>> {
        val files = directoryFilesFetcher.getFiles(path, fileType = "json")
        val filesInformation = files.mapValues { it.value.map { address -> this.fileNameProcessor.process(address) }  }.toMutableMap()
        val result : MutableMap<String, List<MockFileInformation>> = mutableMapOf()

        if(filesInformation.isEmpty()) {
            return  emptyMap()
        }

        val orderedItem = filesInformation.keys.sortedBy { it }.toMutableList()
        val globalConfigs = globalMockDirectoryConfig ?: GlobalMockDirectoryConfiguration.exists(inDirectory = orderedItem.first())

        // if globalConfigs is available it means that this folder should be the main folder and no file should be inside it,
        // so we can remove it from the results
        if (globalConfigs != null) {
            orderedItem.removeFirst()
        }

        orderedItem.forEach { key ->
            val filesInsideDirectory = filesInformation[key] ?: return emptyMap()
            val relativePath = this.makeRelativePath(forPath = key, relativeTo = path)
            val overriddenPath: String? = globalConfigs?.hasMap(forRelativePth = relativePath)?.to
            val fileInformation: List<MockFileInformation> = filesInsideDirectory.map { file ->
                    val relativePath = this.makeRelativePath(
                        forPath = file.rawPath,
                        relativeTo = path
                    )
                    file.copy(relativePath = relativePath)
            }
            val actualKey = if (overriddenPath == null) relativePath else overriddenPath + relativePath
            result[actualKey] = fileInformation
        }

        val mocks = result.mapValues { it.value.map { info -> this.mockProcessor.process(info) }  }

        return mocks
    }

    private fun makeRelativePath(forPath: String, relativeTo: String): String {
        return forPath.replace(relativeTo, "").lowercase()
    }
}

