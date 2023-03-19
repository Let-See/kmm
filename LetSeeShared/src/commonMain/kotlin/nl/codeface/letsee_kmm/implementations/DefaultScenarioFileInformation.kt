package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.models.ScenarioFileInformation
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor

expect class DefaultScenarioFileInformation: ScenarioFileInformationProcessor {
    override fun process(filePath: String): ScenarioFileInformation?
}