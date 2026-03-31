package io.github.letsee.MockImplementations

import io.github.letsee.interfaces.FileDataFetcher

class MockFileDataFetcher(var result: ByteArray? = null): FileDataFetcher {
    override fun getFileData(filePath: String): ByteArray? {
        return result
    }
}