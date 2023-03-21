package nl.codeface.letsee_kmm.implementations

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception

actual fun GlobalMockDirectoryConfiguration.Companion.exists(inDirectory: String): DefaultGlobalMockDirectoryConfiguration? {
    try {
        val jsonString = File("$inDirectory/${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}" ).readText()
        return Json.decodeFromString<DefaultGlobalMockDirectoryConfiguration>(jsonString)
    } catch (e: Exception) {
        when(e) {
            is FileNotFoundException, is SerializationException, is IllegalArgumentException -> {
                print(e.localizedMessage)
            }
        }
    }
    return null
}