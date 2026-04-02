package io.github.letsee.ui

import io.github.letsee.interfaces.LetSee
import io.github.letsee.interfaces.RequestsManager
import io.github.letsee.models.Scenario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenarioListStateHolder(
    private val letSee: LetSee,
    requestsManager: RequestsManager,
    private val coroutineScope: CoroutineScope
) {
    private val _scenarios = MutableStateFlow(letSee.scenarios)
    val scenarios: StateFlow<List<Scenario>> = _scenarios.asStateFlow()

    val activeScenario: StateFlow<Scenario?> = requestsManager.scenarioManager.activeScenario

    fun refreshScenarios() {
        _scenarios.value = letSee.scenarios
    }

    fun activateScenario(scenario: Scenario) {
        letSee.activateScenario(scenario)
    }

    fun deactivateScenario() {
        letSee.deactivateScenario()
    }
}
