package io.github.letsee.implementations
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile

actual fun GlobalMockDirectoryConfiguration.Companion.exists(inDirectory: String): DefaultGlobalMockDirectoryConfiguration? {
        val path = inDirectory + "/" + GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME
        NSString.stringWithContentsOfFile(path)?.toString()?.let { data ->
            try {
                return Json.decodeFromString(data)
            } catch (e: SerializationException) {

            } catch (e: IllegalArgumentException ) {

            }
        }
        return null
}

