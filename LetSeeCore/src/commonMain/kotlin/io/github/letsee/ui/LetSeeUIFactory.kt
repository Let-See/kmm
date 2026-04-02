package io.github.letsee.ui

import io.github.letsee.interfaces.LetSee
import kotlinx.coroutines.CoroutineScope

/**
 * Holds the three UI state holders that together power the LetSee debug panel.
 *
 * Obtain an instance via [LetSeeUIFactory.create].
 *
 * @property requestListStateHolder  drives the intercepted-request list
 * @property scenarioListStateHolder drives scenario selection
 * @property settingsStateHolder     drives configuration toggles
 */
data class LetSeeUIComponents(
    val requestListStateHolder: RequestListStateHolder,
    val scenarioListStateHolder: ScenarioListStateHolder,
    val settingsStateHolder: SettingsStateHolder,
)

/**
 * Factory that wires all UI state holders to a single [LetSee] instance.
 *
 * Usage:
 * ```
 * val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
 * val components = LetSeeUIFactory.create(letSee, scope)
 * // … use components.requestListStateHolder, etc. …
 * // When the debug UI is dismissed:
 * scope.cancel()
 * ```
 *
 * **Important:** the caller owns [coroutineScope] and must cancel it when
 * the debug UI is no longer needed to avoid resource leaks.
 */
object LetSeeUIFactory {

    /**
     * Creates all UI state holders wired to the given [letSee] instance.
     *
     * @param letSee         the core SDK instance
     * @param coroutineScope shared scope used by all state holders;
     *   the caller is responsible for cancelling it when done
     * @return a [LetSeeUIComponents] bundle ready for consumption by Compose UI
     */
    fun create(letSee: LetSee, coroutineScope: CoroutineScope): LetSeeUIComponents {
        val requestsManager = letSee.requestsManager

        return LetSeeUIComponents(
            requestListStateHolder = RequestListStateHolder(
                requestsManager = requestsManager,
                config = letSee.config,
                coroutineScope = coroutineScope,
            ),
            scenarioListStateHolder = ScenarioListStateHolder(
                letSee = letSee,
                requestsManager = requestsManager,
                coroutineScope = coroutineScope,
            ),
            settingsStateHolder = SettingsStateHolder(
                letSee = letSee,
                coroutineScope = coroutineScope,
            ),
        )
    }
}
