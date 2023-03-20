package nl.codeface.letsee_kmm.interfaces

import nl.codeface.letsee_kmm.models.ScenarioFileInformation

/**
 * Receives a path of a scenario file and maps that file to the `ScenarioFileInformation`
 */
interface ScenarioFileInformationProcessor {
    fun process(filePath: String): ScenarioFileInformation?
}