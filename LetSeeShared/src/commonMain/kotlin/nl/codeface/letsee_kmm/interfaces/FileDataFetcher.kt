package nl.codeface.letsee_kmm.interfaces

interface FileDataFetcher {
    fun getFileData(filePath: String): ByteArray?
}