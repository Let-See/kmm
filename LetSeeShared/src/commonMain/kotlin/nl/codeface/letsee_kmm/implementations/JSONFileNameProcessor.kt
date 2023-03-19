package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.MockFileInformation
import nl.codeface.letsee_kmm.interfaces.FileNameCleaner

class JSONFileNameProcessor(private val cleaner: FileNameCleaner): FileNameProcessor<MockFileInformation> {
    override fun process(filePath: String): MockFileInformation {
        val fileName = cleaner.clean(filePath)
        val displayName = fileName.replaceFirstChar { it.uppercaseChar() }
        return MockFileInformation(filePath, statusCode = null, delay = null,
            if(fileName.lowercase().startsWith("error")) MockFileInformation.MockStatus.FAILURE
            else MockFileInformation.MockStatus.SUCCESS, displayName = displayName, relativePath = null)
    }
}