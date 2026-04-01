package io.github.letsee

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MockStateChangedTest {

    @Test
    fun `mockStateChanged emits true when isMockEnabled toggled to true`() = runTest {
        val sut = DefaultLetSee()
        val emissions = mutableListOf<Boolean>()
        val job = launch { sut.mockStateChanged.collect { emissions.add(it) } }
        // Advance so the collector starts and processes the initial (dropped) emission before changes
        testScheduler.advanceUntilIdle()
        sut.setConfigurations(Configuration.default.copy(isMockEnabled = true))
        testScheduler.advanceUntilIdle()
        job.cancel()
        assertEquals(listOf(true), emissions)
    }

    @Test
    fun `mockStateChanged does not emit on initial subscription`() = runTest {
        val sut = DefaultLetSee()
        val emissions = mutableListOf<Boolean>()
        val job = launch { sut.mockStateChanged.collect { emissions.add(it) } }
        testScheduler.advanceUntilIdle()
        job.cancel()
        assertEquals(emptyList(), emissions)
    }

    @Test
    fun `mockStateChanged emits only distinct values`() = runTest {
        val sut = DefaultLetSee()
        val emissions = mutableListOf<Boolean>()
        val job = launch { sut.mockStateChanged.collect { emissions.add(it) } }
        // Advance so the collector starts and processes the initial (dropped) emission
        testScheduler.advanceUntilIdle()
        // StateFlow is conflated: advance between each change so the collector sees each intermediate value
        sut.setConfigurations(Configuration.default.copy(isMockEnabled = true))
        testScheduler.advanceUntilIdle()
        sut.setConfigurations(Configuration.default.copy(isMockEnabled = true)) // same value — should not re-emit
        testScheduler.advanceUntilIdle()
        sut.setConfigurations(Configuration.default.copy(isMockEnabled = false))
        testScheduler.advanceUntilIdle()
        job.cancel()
        assertEquals(listOf(true, false), emissions)
    }
}
