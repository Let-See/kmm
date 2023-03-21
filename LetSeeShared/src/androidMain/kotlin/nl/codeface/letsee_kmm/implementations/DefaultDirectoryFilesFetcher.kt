package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import java.io.File

actual class DefaultDirectoryFilesFetcher: DirectoryFilesFetcher {
    actual override fun getFiles(path: String, fileType: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        val root = File(path)

        if (!root.isDirectory) {
            return emptyMap()
        }

        for (file in root.walkTopDown()) {
            if (file.isFile && file.extension == fileType) {
                val dir = file.parentFile?.toString()?.let { dir ->
                    val pathList = result.getOrDefault(dir, mutableListOf())
                    pathList.add(file.toString())
                    result[dir] = pathList
                }
            }
        }

        return result
    }
}