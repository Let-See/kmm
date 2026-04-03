package io.github.letsee.interfaces

/**
 * File Data Fetcher reads the Data of the given file. We need to read the data of the file and as the data type and path processing
 * is different in each platform, this interface conducts the retrieving the data and returns it in the Kotlin Native ByteArray
 *
 */
interface FileDataFetcher {
    fun getFileData(filePath: String): ByteArray?
}