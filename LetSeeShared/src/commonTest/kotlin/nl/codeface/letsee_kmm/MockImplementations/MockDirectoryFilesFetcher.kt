package nl.codeface.letsee_kmm.MockImplementations

import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher

class MockDirectoryFilesFetcher(var result: Map<String, List<String>>? = null):
    DirectoryFilesFetcher {
    override fun getFiles(path: String, fileType: String): Map<String, List<String>> {
        return result ?: emptyMap()
    }
}