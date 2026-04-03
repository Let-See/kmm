package io.github.letsee.ui

import io.github.letsee.Configuration
import io.github.letsee.interfaces.LetSee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * UI state holder that exposes the current [Configuration] and provides
 * actions to toggle settings or update the base URL.
 *
 * All mutations are synchronous calls to [LetSee.setConfigurations]; the
 * resulting state is observed through [configuration].
 *
 * @param letSee  the core SDK instance whose configuration is managed
 * @param coroutineScope  shared scope for potential future async work;
 *   the caller is responsible for cancelling it when the UI is dismissed
 */
class SettingsStateHolder(
    private val letSee: LetSee,
    private val coroutineScope: CoroutineScope,
) {
    /** Current configuration, updated whenever [LetSee.setConfigurations] is called. */
    val configuration: StateFlow<Configuration> = letSee.config

    fun toggleMockEnabled() {
        val current = letSee.config.value
        letSee.setConfigurations(current.copy(isMockEnabled = !current.isMockEnabled))
    }

    fun toggleCutBaseURL() {
        val current = letSee.config.value
        letSee.setConfigurations(current.copy(shouldCutBaseURLFromURLsTitle = !current.shouldCutBaseURLFromURLsTitle))
    }

    fun setBaseURL(url: String) {
        val current = letSee.config.value
        letSee.setConfigurations(current.copy(baseURL = url))
    }
}
