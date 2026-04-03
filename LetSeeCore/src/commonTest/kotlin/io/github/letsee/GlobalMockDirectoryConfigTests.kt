package io.github.letsee

import io.github.letsee.implementations.GlobalMockDirectoryConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

class GlobalMockDirectoryConfigTests {

    @Test
    fun `GLOBAL_CONFIG_FILE_NAME has leading dot`() {
        assertEquals(".ls.global.json", GlobalMockDirectoryConfiguration.GLOBAL_CONFIG_FILE_NAME)
    }
}
