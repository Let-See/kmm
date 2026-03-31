package io.github.letsee.implementations

import android.app.Application
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception

var androidContext: Application? = null
fun setLetSeeAndroidContext(context: Application) { androidContext = context }
actual fun GlobalMockDirectoryConfiguration.Companion.exists(inDirectory: String): DefaultGlobalMockDirectoryConfiguration? {
    try {
        //val jsonString = //File("$inDirectory/${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}" ).readText()
          return androidContext?.assets?.open("$inDirectory/${GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME}" )?.bufferedReader().use {
              it?.readText()?.let { Json.decodeFromString<DefaultGlobalMockDirectoryConfiguration>(it) }
          }

//        return Json.decodeFromString<DefaultGlobalMockDirectoryConfiguration>(jsonString)
    } catch (e: Exception) {
        when(e) {
            is FileNotFoundException, is SerializationException, is IllegalArgumentException -> {
                print(e.localizedMessage)
            }
        }
    }
    return null
}