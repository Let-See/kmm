package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.FileDataFetcher
import java.io.File

actual class DefaultFileFetcher: FileDataFetcher {
    actual override fun getFileData(filePath: String): ByteArray? {
        return File(filePath).readBytes()
    }
}