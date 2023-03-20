package nl.codeface.letsee_kmm.MockImplementations

import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.ScenarioFileInformation
class MockScenarioFileInformationProcessor(var result: List<ScenarioFileInformation> = emptyList()): ScenarioFileInformationProcessor {
    private var index: Int = -1
    override fun process(filePath: String): ScenarioFileInformation? {
        index += 1
        return if (result.count() > index) { result[index] } else { null }
    }
}