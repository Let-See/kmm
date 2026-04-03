package io.github.letsee.implementations

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import io.github.letsee.interfaces.ScenarioFileInformationProcessor
import io.github.letsee.models.ScenarioFileInformation
import io.github.letsee.models.ScenarioFileInformationExtended
import java.io.FileNotFoundException
import java.io.IOException

actual class DefaultScenarioFileInformation : ScenarioFileInformationProcessor {
    actual override fun process(filePath: String): ScenarioFileInformation? {
        try {
//            val jsonString = File(filePath).readText()
//            return Json.decodeFromString<ScenarioFileInformation>(jsonString)
            return androidContext?.assets?.open(filePath)?.bufferedReader().use {
                it?.readText()?.let {
                    val json = Json {
                        ignoreUnknownKeys = true

                    }
                    val extended = json.decodeFromString<ScenarioFileInformationExtended>(it)
                    val result = ScenarioFileInformation(extended.displayName ?: filePath.split("/").lastOrNull() ?: filePath, steps = extended.steps.map {
                        ScenarioFileInformation.Step(it.folder, (it.fileName ?: if (it.state == "failed" || it.state == "failure") "error_failed"
                        else throw FileNotFoundException("file name should be there for the success responses.")))
                    })
                    return result
                }

            }
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