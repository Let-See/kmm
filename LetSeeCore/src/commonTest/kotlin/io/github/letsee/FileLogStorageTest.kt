package io.github.letsee

import io.github.letsee.implementations.FileLogStorage
import io.github.letsee.implementations.defaultLogDirectory
import io.github.letsee.implementations.deleteFile
import io.github.letsee.testutils.readFileForTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [FileLogStorage].
 *
 * These tests exercise the real platform file I/O via the `appendToFile`/`deleteFile`
 * expect/actual implementations.
 *
 * On Android unit tests, `androidContext` is null so `defaultLogDirectory()` falls back
 * to `System.getProperty("java.io.tmpdir")`.
 * On iOS simulator tests, `NSTemporaryDirectory()` provides the temp directory.
 */
class FileLogStorageTest {

    private val testFilePath = defaultLogDirectory() + "/letsee_test_${kotlin.random.Random.nextInt(100_000)}.log"

    @AfterTest
    fun cleanup() {
        deleteFile(testFilePath)
    }

    // 10.4 – clear-on-init=true deletes existing content
    @Test
    fun `clearOnInit true removes existing file content on construction`() {
        // Write some content using a non-clearing storage
        val first = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        first.append("existing data")

        val contentBefore = readFileForTest(testFilePath)
        assertNotNull(contentBefore, "File should exist after first append")
        assertTrue(contentBefore.contains("existing data"), "File should contain written data")

        // Construct with clearOnInit=true — should delete the file
        FileLogStorage(filePath = testFilePath, clearOnInit = true)

        val contentAfter = readFileForTest(testFilePath)
        assertTrue(
            contentAfter == null || contentAfter.isEmpty(),
            "File should be absent or empty after clearOnInit=true"
        )
    }

    // 10.4 – clearOnInit=false preserves existing file
    @Test
    fun `clearOnInit false preserves existing content`() {
        val storage = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        storage.append("hello world")

        // Open same file without clearing
        val storage2 = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        storage2.append(" again")

        val content = readFileForTest(testFilePath)
        assertNotNull(content)
        assertTrue(content.contains("hello world"), "File should still have first write")
        assertTrue(content.contains(" again"), "File should have second write appended")
    }

    // append writes text to file
    @Test
    fun `append writes text to the file`() {
        val storage = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        storage.append("line one\n")
        storage.append("line two\n")

        val content = readFileForTest(testFilePath)
        assertNotNull(content, "File should exist after appending")
        assertTrue(content.contains("line one"), "first append should be in file")
        assertTrue(content.contains("line two"), "second append should be in file")
    }

    // clear() deletes/empties the file
    @Test
    fun `clear removes file content`() {
        val storage = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        storage.append("some data")
        storage.clear()

        val content = readFileForTest(testFilePath)
        assertTrue(
            content == null || content.isEmpty(),
            "File should be absent or empty after clear()"
        )
    }

    // filePath property returns the configured path
    @Test
    fun `filePath returns the configured path`() {
        val storage = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        assertEquals(testFilePath, storage.filePath)
    }

    // Multiple appends accumulate content
    @Test
    fun `multiple appends accumulate all content`() {
        val storage = FileLogStorage(filePath = testFilePath, clearOnInit = false)
        val lines = listOf("alpha", "beta", "gamma")
        lines.forEach { storage.append("$it\n") }

        val content = readFileForTest(testFilePath)
        assertNotNull(content)
        lines.forEach { line ->
            assertTrue(content.contains(line), "content should include '$line'")
        }
    }
}
