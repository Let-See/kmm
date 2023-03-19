package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher

actual class DefaultDirectoryFilesFetcher: DirectoryFilesFetcher {
    actual override fun getFiles(path: String, fileType: String): Map<String, List<String>> {
        TODO()
    }
}