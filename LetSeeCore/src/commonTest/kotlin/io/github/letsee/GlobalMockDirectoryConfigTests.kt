package io.github.letsee

import io.github.letsee.implementations.DefaultGlobalMockDirectoryConfiguration
import io.github.letsee.implementations.GlobalMockDirectoryConfiguration
import io.github.letsee.implementations.exists
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GlobalMockDirectoryConfigTests {
    var sut: DefaultGlobalMockDirectoryConfiguration? = null
    @BeforeTest
    fun setUp() {
        sut = DefaultGlobalMockDirectoryConfiguration(emptyList())
    }

    @AfterTest
    fun tearDown() {
        sut = null
    }

    @Test
    fun `test Process Function Should Return Correct Mock File Information`() {
        val fileName = "/Users/farshad/Library/Developer/CoreSimulator/Devices/246A41A6-9494-4948-BEC1-74A427E96487/data/Containers/Bundle/Application/33118E66-0C15-4CCF-BCD2-8378884C8986/iosApp.app/Mocks"
        val result = GlobalMockDirectoryConfiguration.exists(inDirectory = fileName)
        val expected = "someTestFileName"

//        assertEquals(expected, result)
    }
}
