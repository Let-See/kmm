package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher

expect class DefaultDirectoryFilesFetcher(): DirectoryFilesFetcher {
   override fun getFiles(path: String, fileType: String): Map<String, List<String>>
}