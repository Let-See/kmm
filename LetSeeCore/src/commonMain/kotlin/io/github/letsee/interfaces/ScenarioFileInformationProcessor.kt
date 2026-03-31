package io.github.letsee.interfaces

import io.github.letsee.models.ScenarioFileInformation

/**
 * Receives a path of a scenario file and maps that file to the `ScenarioFileInformation`
 */
interface ScenarioFileInformationProcessor {
    fun process(filePath: String): ScenarioFileInformation?
}