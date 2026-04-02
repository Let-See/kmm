package io.github.letsee.ui

import io.github.letsee.CapturingRequestsManager
import io.github.letsee.DefaultLetSee
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LetSeeUIFactoryTest {

    @Test
    fun `create returns all three state holders`() = runTest {
        val letSee = DefaultLetSee(
            requestsManager = CapturingRequestsManager(),
            mocks = emptyMap(),
            dispatcher = UnconfinedTestDispatcher()
        )
        val scope = TestScope(UnconfinedTestDispatcher())

        val components = LetSeeUIFactory.create(letSee, scope)

        assertNotNull(components.requestListStateHolder)
        assertNotNull(components.scenarioListStateHolder)
        assertNotNull(components.settingsStateHolder)
    }

    @Test
    fun `created state holders share the same configuration source`() = runTest {
        val letSee = DefaultLetSee(
            requestsManager = CapturingRequestsManager(),
            mocks = emptyMap(),
            dispatcher = UnconfinedTestDispatcher()
        )
        val scope = TestScope(UnconfinedTestDispatcher())

        val components = LetSeeUIFactory.create(letSee, scope)
        components.settingsStateHolder.toggleMockEnabled()

        assertTrue(components.settingsStateHolder.configuration.value.isMockEnabled)
        assertEquals(
            letSee.config.value,
            components.settingsStateHolder.configuration.value
        )
    }

    @Test
    fun `created requestListStateHolder starts with empty requests`() = runTest {
        val letSee = DefaultLetSee(
            requestsManager = CapturingRequestsManager(),
            mocks = emptyMap(),
            dispatcher = UnconfinedTestDispatcher()
        )
        val scope = TestScope(UnconfinedTestDispatcher())

        val components = LetSeeUIFactory.create(letSee, scope)

        assertEquals(emptyList(), components.requestListStateHolder.requests.value)
    }
}
