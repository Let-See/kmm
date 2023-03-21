package nl.codeface.letsee_kmm.implementations

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.ScenarioFileInformation
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception

actual class DefaultScenarioFileInformation : ScenarioFileInformationProcessor {
    actual override fun process(filePath: String): ScenarioFileInformation? {
        try {
            val jsonString = File(filePath).readText()
            return Json.decodeFromString<ScenarioFileInformation>(jsonString)
        } catch (e: Exception) {
            when(e) {
                is FileNotFoundException, is SerializationException, is IllegalArgumentException -> {
                    print(e.localizedMessage)
                }
            }
        }
        return null
    }
}