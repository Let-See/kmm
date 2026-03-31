package io.github.letsee.implementations

import io.github.letsee.models.ScenarioFileInformation
import io.github.letsee.interfaces.ScenarioFileInformationProcessor

expect class DefaultScenarioFileInformation(): ScenarioFileInformationProcessor {
    override fun process(filePath: String): ScenarioFileInformation?
}