package io.github.letsee.MockImplementations

import io.github.letsee.interfaces.ScenarioFileInformationProcessor
import io.github.letsee.models.ScenarioFileInformation
class MockScenarioFileInformationProcessor(var result: List<ScenarioFileInformation> = emptyList()): ScenarioFileInformationProcessor {
    private var index: Int = -1
    override fun process(filePath: String): ScenarioFileInformation? {
        index += 1
        return if (result.count() > index) { result[index] } else { null }
    }
}