package io.github.letsee.interfaces

import kotlinx.coroutines.flow.StateFlow
import io.github.letsee.models.Scenario

interface ScenarioManager {
    val activeScenario: StateFlow<Scenario?>
    val isScenarioActive: Boolean
    suspend fun activate(scenario: Scenario)
    suspend fun deactivateScenario()
    suspend fun nextStep()
}