package nl.codeface.letsee_kmm

import nl.codeface.letsee_kmm.implementations.JSONFileNameCleaner
import nl.codeface.letsee_kmm.interfaces.FileNameCleaner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

actual class FileNameCleanerTest {
    var sut: FileNameCleaner? = null
    @BeforeTest
    fun setUp() {
        sut = JSONFileNameCleaner()
    }

    @AfterTest
    fun tearDown() {
        sut = null
    }

    @Test
    actual fun `test file cleaner should work correctly when the file path`() {
        val fileName = "/some/long/address/someTestFileName.json"
        val result = sut!!.clean(fileName)
        val expected = "someTestFileName"

        assertEquals(expected, result)
    }
}
