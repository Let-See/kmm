package io.github.letsee.implementations

import io.github.letsee.interfaces.DirectoryFilesFetcher

expect class DefaultDirectoryFilesFetcher(): DirectoryFilesFetcher {
   override fun getFiles(path: String, fileType: String): Map<String, List<String>>
}