package nl.codeface.letsee_kmm.implementations

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import nl.codeface.letsee_kmm.interfaces.ScenarioManager
import nl.codeface.letsee_kmm.models.Scenario

class DefaultScenarioManager : ScenarioManager {
    override val scenario: SharedFlow<Scenario?>
        get() = _activeScenario.asSharedFlow()

    private var _activeScenario = run {
        val flow = MutableSharedFlow<Scenario?>(replay = 1)
        flow.tryEmit(null)
        flow
    }
    override val activeScenario: Scenario?
        get() = _activeScenario.replayCache.first()
    override suspend fun activate(scenario: Scenario) {
        _activeScenario.emit(scenario)
    }

    override suspend fun deactivateScenario() {
        _activeScenario.emit(null)
    }

    /**
    The `isScenarioActive` property gets a boolean value indicating whether there is a scenario active or not.
     */
    override val isScenarioActive: Boolean
        get() = activeScenario?.currentStep != null

}