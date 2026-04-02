package io.github.letsee

import io.github.letsee.implementations.DefaultScenarioManager
import io.github.letsee.models.Mock
import io.github.letsee.models.Scenario
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenarioManagerTest {

    private fun scenarioWith(vararg mocks: Mock) =
        Scenario(name = "test-scenario", mocks = mocks.toList())

    @Test
    fun `activate scenario with 3 mocks and step through all`() = runTest {
        val sut = DefaultScenarioManager()
        val scenario = scenarioWith(Mock.LIVE, Mock.LIVE, Mock.LIVE)

        sut.activate(scenario)
        assertEquals(0, sut.activeScenario.value?.currentIndex)

        sut.nextStep()
        assertEquals(1, sut.activeScenario.value?.currentIndex)

        sut.nextStep()
        assertEquals(2, sut.activeScenario.value?.currentIndex)

        sut.nextStep()
        assertNull(sut.activeScenario.value, "Scenario should auto-deactivate after last step")
    }

    @Test
    fun `activate and deactivate mid-way`() = runTest {
        val sut = DefaultScenarioManager()
        val scenario = scenarioWith(Mock.LIVE, Mock.LIVE, Mock.LIVE)

        sut.activate(scenario)
        sut.nextStep()
        assertEquals(1, sut.activeScenario.value?.currentIndex)

        sut.deactivateScenario()
        assertNull(sut.activeScenario.value)
        assertFalse(sut.isScenarioActive)
    }

    @Test
    fun `activate empty scenario does nothing`() = runTest {
        val sut = DefaultScenarioManager()
        val emptyScenario = Scenario(name = "empty", mocks = emptyList())

        sut.activate(emptyScenario)
        assertNull(sut.activeScenario.value)
        assertFalse(sut.isScenarioActive)
    }

    @Test
    fun `nextStep on inactive scenario does nothing`() = runTest {
        val sut = DefaultScenarioManager()
        assertNull(sut.activeScenario.value)

        sut.nextStep()
        assertNull(sut.activeScenario.value)
        assertFalse(sut.isScenarioActive)
    }

    @Test
    fun `currentStep returns correct mock at each index`() = runTest {
        val sut = DefaultScenarioManager()
        val mock0 = Mock.LIVE
        val mock1 = Mock.CANCEL
        val mock2 = Mock.defaultSuccess("step-2", byteArrayOf())
        val scenario = scenarioWith(mock0, mock1, mock2)

        sut.activate(scenario)
        assertEquals(mock0, sut.activeScenario.value?.currentStep)

        sut.nextStep()
        assertEquals(mock1, sut.activeScenario.value?.currentStep)

        sut.nextStep()
        assertEquals(mock2.name, sut.activeScenario.value?.currentStep?.name)
    }
}
