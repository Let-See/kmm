package io.github.letsee.interfaces

/**
 * Travers the given directory and it's sub directories and retrieves the files path. It filters the result based on the given fileType properties
 * The main Idea is to delegate the file gathering to the platform and hide the implementation of that in each specific platform separately
 */
interface DirectoryFilesFetcher {
    fun getFiles(path: String, fileType: String): Map<String, List<String>>
}