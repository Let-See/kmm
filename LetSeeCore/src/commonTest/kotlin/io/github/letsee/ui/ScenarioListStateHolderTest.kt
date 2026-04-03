package io.github.letsee.ui

import io.github.letsee.CapturingRequestsManager
import io.github.letsee.Configuration
import io.github.letsee.DefaultLetSee
import io.github.letsee.models.Mock
import io.github.letsee.models.Scenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioListStateHolderTest {

    private fun createFixtures(): Triple<DefaultLetSee, CapturingRequestsManager, TestScope> {
        val manager = CapturingRequestsManager()
        val scope = TestScope(UnconfinedTestDispatcher())
        val letSee = DefaultLetSee(
            requestsManager = manager,
            mocks = emptyMap(),
            dispatcher = UnconfinedTestDispatcher()
        )
        return Triple(letSee, manager, scope)
    }

    @Test
    fun `scenarios reflects initial letSee scenarios`() = runTest {
        val (letSee, manager, scope) = createFixtures()
        val sut = ScenarioListStateHolder(letSee, manager, scope)

        assertEquals(letSee.scenarios, sut.scenarios.value)
        assertEquals(emptyList(), sut.scenarios.value)
    }

    @Test
    fun `activeScenario is initially null`() = runTest {
        val (letSee, manager, scope) = createFixtures()
        val sut = ScenarioListStateHolder(letSee, manager, scope)

        assertNull(sut.activeScenario.value)
    }

    @Test
    fun `activateScenario delegates to letSee and updates activeScenario`() = runTest {
        val (letSee, manager, scope) = createFixtures()
        val sut = ScenarioListStateHolder(letSee, manager, scope)

        val scenario = Scenario("login-flow", listOf(Mock.LIVE))
        sut.activateScenario(scenario)
        scope.advanceUntilIdle()

        assertNotNull(sut.activeScenario.value)
        assertEquals("login-flow", sut.activeScenario.value?.name)
    }

    @Test
    fun `deactivateScenario clears activeScenario`() = runTest {
        val (letSee, manager, scope) = createFixtures()
        val sut = ScenarioListStateHolder(letSee, manager, scope)

        val scenario = Scenario("login-flow", listOf(Mock.LIVE))
        sut.activateScenario(scenario)
        scope.advanceUntilIdle()
        assertNotNull(sut.activeScenario.value)

        sut.deactivateScenario()
        scope.advanceUntilIdle()
        assertNull(sut.activeScenario.value)
    }

    @Test
    fun `refreshScenarios re-reads from letSee`() = runTest {
        val (letSee, manager, scope) = createFixtures()
        val sut = ScenarioListStateHolder(letSee, manager, scope)

        assertEquals(emptyList(), sut.scenarios.value)

        sut.refreshScenarios()
        assertEquals(letSee.scenarios, sut.scenarios.value)
    }
}
