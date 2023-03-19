package nl.codeface.letsee_kmm.interfaces

import nl.codeface.letsee_kmm.models.ScenarioFileInformation

interface ScenarioFileInformationProcessor {
    fun process(filePath: String): ScenarioFileInformation?
}