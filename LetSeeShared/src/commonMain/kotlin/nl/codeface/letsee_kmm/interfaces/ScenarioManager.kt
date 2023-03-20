package nl.codeface.letsee_kmm.interfaces

import kotlinx.coroutines.flow.SharedFlow
import nl.codeface.letsee_kmm.models.Scenario

interface ScenarioManager {
    val scenario: SharedFlow<Scenario?>
    val isScenarioActive: Boolean
    val activeScenario: Scenario?
    suspend fun activate(scenario: Scenario)
    suspend fun deactivateScenario()
}