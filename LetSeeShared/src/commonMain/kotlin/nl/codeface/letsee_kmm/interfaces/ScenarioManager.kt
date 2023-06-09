package nl.codeface.letsee_kmm.interfaces

import kotlinx.coroutines.flow.StateFlow
import nl.codeface.letsee_kmm.models.Scenario

interface ScenarioManager {
    val activeScenario: StateFlow<Scenario?>
    val isScenarioActive: Boolean
    suspend fun activate(scenario: Scenario)
    suspend fun deactivateScenario()
    suspend fun nextStep()
}