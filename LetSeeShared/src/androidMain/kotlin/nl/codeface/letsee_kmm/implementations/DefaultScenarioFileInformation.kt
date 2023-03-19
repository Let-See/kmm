package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.ScenarioFileInformation

actual class DefaultScenarioFileInformation : ScenarioFileInformationProcessor {
    actual override fun process(filePath: String): ScenarioFileInformation? {
        TODO("Not yet implemented")
    }
}