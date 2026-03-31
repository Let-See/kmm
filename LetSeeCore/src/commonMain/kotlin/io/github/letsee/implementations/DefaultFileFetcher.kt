package io.github.letsee.implementations

import io.github.letsee.interfaces.FileDataFetcher

expect class DefaultFileFetcher(): FileDataFetcher {
    override fun getFileData(filePath: String): ByteArray?
}