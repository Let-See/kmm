package nl.codeface.letsee_kmm.implementations

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.codeface.letsee_kmm.interfaces.FileDataFetcher
import nl.codeface.letsee_kmm.models.ScenarioFileInformation
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception

actual class DefaultFileFetcher : FileDataFetcher {
    actual override fun getFileData(filePath: String): ByteArray? {
        try {
           return androidContext?.assets?.open(filePath)?.use { inputStream ->
                val length = inputStream.available()
                val bytes = ByteArray(length)
                inputStream.read(bytes)
               bytes
            }
//            return File(filePath).readBytes()
        } catch (e: Exception) {
            when(e) {
                is FileNotFoundException -> {
                    print(e.localizedMessage)
                }
            }
        }
        return null
    }
}