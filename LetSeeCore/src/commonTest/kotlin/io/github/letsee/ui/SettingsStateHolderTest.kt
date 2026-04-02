package io.github.letsee.ui

import io.github.letsee.CapturingRequestsManager
import io.github.letsee.Configuration
import io.github.letsee.DefaultLetSee
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStateHolderTest {

    private fun createLetSee(): DefaultLetSee =
        DefaultLetSee(
            requestsManager = CapturingRequestsManager(),
            mocks = emptyMap(),
            dispatcher = UnconfinedTestDispatcher()
        )

    @Test
    fun `toggleMockEnabled flips isMockEnabled`() = runTest {
        val letSee = createLetSee()
        val sut = SettingsStateHolder(letSee, TestScope(UnconfinedTestDispatcher()))

        assertFalse(sut.configuration.value.isMockEnabled)
        sut.toggleMockEnabled()
        assertTrue(sut.configuration.value.isMockEnabled)
        sut.toggleMockEnabled()
        assertFalse(sut.configuration.value.isMockEnabled)
    }

    @Test
    fun `toggleCutBaseURL flips shouldCutBaseURLFromURLsTitle`() = runTest {
        val letSee = createLetSee()
        val sut = SettingsStateHolder(letSee, TestScope(UnconfinedTestDispatcher()))

        assertFalse(sut.configuration.value.shouldCutBaseURLFromURLsTitle)
        sut.toggleCutBaseURL()
        assertTrue(sut.configuration.value.shouldCutBaseURLFromURLsTitle)
        sut.toggleCutBaseURL()
        assertFalse(sut.configuration.value.shouldCutBaseURLFromURLsTitle)
    }

    @Test
    fun `setBaseURL updates baseURL`() = runTest {
        val letSee = createLetSee()
        val sut = SettingsStateHolder(letSee, TestScope(UnconfinedTestDispatcher()))

        assertEquals(Configuration.default.baseURL, sut.configuration.value.baseURL)
        sut.setBaseURL("https://new.api.com")
        assertEquals("https://new.api.com", sut.configuration.value.baseURL)
    }

    @Test
    fun `configuration reflects external changes to LetSee config`() = runTest {
        val letSee = createLetSee()
        val sut = SettingsStateHolder(letSee, TestScope(UnconfinedTestDispatcher()))

        letSee.setConfigurations(Configuration(isMockEnabled = true, shouldCutBaseURLFromURLsTitle = true, baseURL = "https://external.com"))
        assertTrue(sut.configuration.value.isMockEnabled)
        assertTrue(sut.configuration.value.shouldCutBaseURLFromURLsTitle)
        assertEquals("https://external.com", sut.configuration.value.baseURL)
    }
}
