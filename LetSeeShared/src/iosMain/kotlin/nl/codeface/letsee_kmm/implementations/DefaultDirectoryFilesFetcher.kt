package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import platform.Foundation.NSDirectoryEnumerationSkipsPackageDescendants
import platform.Foundation.NSDirectoryEnumerator
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsRegularFileKey
import platform.Foundation.NSURLParentDirectoryURLKey
import platform.Foundation.NSURLResourceKey
import platform.Foundation.pathExtension

actual class DefaultDirectoryFilesFetcher: DirectoryFilesFetcher {
    actual override fun getFiles(path: String, fileType: String): Map<String, List<String>> {
        var pathURL = NSURL.fileURLWithPath(path)
        val enumerator: NSDirectoryEnumerator? = NSFileManager.defaultManager.enumeratorAtURL(
            pathURL,
            listOf<NSURLResourceKey>(NSURLIsRegularFileKey, NSURLParentDirectoryURLKey),
            NSDirectoryEnumerationSkipsPackageDescendants,
            errorHandler = null
        )
        var directoryAndFiles: MutableMap<String, MutableList<String>> = mutableMapOf()
        var nextURL = enumerator?.nextObject() as? NSURL
        while (nextURL != null) {
            val file = nextURL
            if (file.pathExtension == fileType) {
                file.resourceValuesForKeys(
                    listOf<NSURLResourceKey>(
                        NSURLIsRegularFileKey,
                        NSURLParentDirectoryURLKey
                    ), error = null
                )?.let { fileAttributes ->
                    if (fileAttributes[NSURLIsRegularFileKey] == true) {
                        (fileAttributes[NSURLParentDirectoryURLKey] as? NSURL)?.path?.let { parentDirectory ->
                            var fileList = directoryAndFiles[parentDirectory] ?: mutableListOf()
                            fileList.add(file.path!!)
                            directoryAndFiles[parentDirectory] = fileList
                        }
                    }
                }
            }
            nextURL = enumerator?.nextObject() as? NSURL
        }
        return directoryAndFiles
    }
}