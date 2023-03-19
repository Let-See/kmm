package nl.codeface.letsee_kmm.MockImplementations

import nl.codeface.letsee_kmm.interfaces.FileDataFetcher

class MockFileDataFetcher(var result: ByteArray? = null): FileDataFetcher {
    override fun getFileData(filePath: String): ByteArray? {
        return result
    }
}