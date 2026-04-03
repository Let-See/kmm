package io.github.letsee.implementations

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.github.letsee.interfaces.ScenarioManager
import io.github.letsee.models.Mock
import io.github.letsee.models.Scenario

class DefaultScenarioManager : ScenarioManager {

    private var _activeScenario: MutableStateFlow<Scenario?> = MutableStateFlow(null)
    override val activeScenario: StateFlow<Scenario?>
        get() = _activeScenario

    override suspend fun activate(scenario: Scenario) {
        if (scenario.mocks.isEmpty()) return
        _activeScenario.emit(scenario)
    }

    override suspend fun deactivateScenario() {
        _activeScenario.emit(null)
    }

    override suspend fun nextStep() {
        val activeScenario = activeScenario.value ?: return
        if (activeScenario.currentIndex + 1 < activeScenario.mocks.size) {
            _activeScenario.emit(nextStep(activeScenario))
        } else {
            deactivateScenario()
        }
    }

    /**
     * Moves the cursor to the next mock, when the request received the current mock, this function should be called.
     */
    private fun nextStep(scenario: Scenario): Scenario? {
        return if (scenario.currentIndex < scenario.mocks.size) {
            scenario.copy(currentIndex = scenario.currentIndex + 1 )
        } else {
            return null
        }
    }

    /**
    The `isScenarioActive` property gets a boolean value indicating whether there is a scenario active or not.
     */
    override val isScenarioActive: Boolean
        get() = activeScenario.value != null

}