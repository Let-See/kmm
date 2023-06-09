package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.FileDataFetcher

expect class DefaultFileFetcher(): FileDataFetcher {
    override fun getFileData(filePath: String): ByteArray?
}