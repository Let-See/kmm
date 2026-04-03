@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.implementations

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultScenarioFileInformationTest {

    private fun writeTempPlistFile(plistXml: String): String {
        val tmpDir = NSTemporaryDirectory()
        val uniqueName = NSProcessInfo.processInfo.globallyUniqueString
        val filePath = "${tmpDir}letsee_test_$uniqueName.plist"
        val bytes = plistXml.encodeToByteArray()
        val file = fopen(filePath, "wb") ?: error("Cannot open temp file for writing: $filePath")
        try {
            bytes.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.convert(), bytes.size.convert(), file)
            }
        } finally {
            fclose(file)
        }
        return filePath
    }

    // AC 5: Valid plist parses correctly (existing behavior preserved)
    @Test
    fun `valid plist with one step parses correctly`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>steps</key>
    <array>
        <dict>
            <key>folder</key>
            <string>myFolder</string>
            <key>fileName</key>
            <string>myFile.json</string>
        </dict>
    </array>
</dict>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertEquals(1, result.steps.size)
        assertEquals("myFolder", result.steps[0].folder)
        assertEquals("myFile.json", result.steps[0].fileName)
    }

    // AC 5: Multiple steps all parse correctly
    @Test
    fun `valid plist with multiple steps parses all steps`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>steps</key>
    <array>
        <dict>
            <key>folder</key>
            <string>folderA</string>
            <key>fileName</key>
            <string>fileA.json</string>
        </dict>
        <dict>
            <key>folder</key>
            <string>folderB</string>
            <key>fileName</key>
            <string>fileB.json</string>
        </dict>
    </array>
</dict>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertEquals(2, result.steps.size)
        assertEquals("folderA", result.steps[0].folder)
        assertEquals("fileA.json", result.steps[0].fileName)
        assertEquals("folderB", result.steps[1].folder)
        assertEquals("fileB.json", result.steps[1].fileName)
    }

    // AC 5: displayName strips .plist suffix
    @Test
    fun `valid plist display name strips plist suffix`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>steps</key>
    <array>
        <dict>
            <key>folder</key>
            <string>f</string>
            <key>fileName</key>
            <string>file.json</string>
        </dict>
    </array>
</dict>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertTrue(!result.displayName.endsWith(".plist"), "displayName should not end with .plist")
    }

    // AC 6: Malformed plist — missing "steps" key — does not crash, returns empty steps
    @Test
    fun `malformed plist missing steps key returns empty steps without crashing`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>name</key>
    <string>NoStepsScenario</string>
</dict>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertEquals(0, result.steps.size)
    }

    // AC 4: Empty steps array returns empty step list
    @Test
    fun `plist with empty steps array returns empty steps`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>steps</key>
    <array/>
</dict>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertEquals(0, result.steps.size)
    }

    @Test
    fun `non-existent file path returns empty steps without crashing`() {
        val sut = DefaultScenarioFileInformation()
        val result = sut.process("/nonexistent/path/to/scenario.plist")

        assertNotNull(result)
        assertEquals(0, result.steps.size)
    }

    @Test
    fun `plist with NSArray root instead of NSDictionary returns empty steps`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<array>
    <string>not a dictionary</string>
</array>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertEquals(0, result.steps.size)
    }

    // AC 4: Step missing "folder" key is skipped, subsequent valid steps are still parsed
    @Test
    fun `step missing folder key is skipped and remaining valid steps are preserved`() {
        val plistXml = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>steps</key>
    <array>
        <dict>
            <key>fileName</key>
            <string>fileNoFolder.json</string>
        </dict>
        <dict>
            <key>folder</key>
            <string>validFolder</string>
            <key>fileName</key>
            <string>validFile.json</string>
        </dict>
    </array>
</dict>
</plist>"""
        val filePath = writeTempPlistFile(plistXml)
        val sut = DefaultScenarioFileInformation()

        val result = sut.process(filePath)

        assertNotNull(result)
        assertEquals(1, result.steps.size, "Only the valid step should be parsed")
        assertEquals("validFolder", result.steps[0].folder)
        assertEquals("validFile.json", result.steps[0].fileName)
    }
}
