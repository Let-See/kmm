package io.github.letsee.implementations

import io.github.letsee.interfaces.FileNameProcessor
import io.github.letsee.models.MockFileInformation
import io.github.letsee.interfaces.FileNameCleaner

class JSONFileNameProcessor(private val cleaner: FileNameCleaner): FileNameProcessor<MockFileInformation> {
    override fun process(filePath: String): MockFileInformation {
        val fileName = cleaner.clean(filePath)
        val displayName = fileName.replaceFirstChar { it.uppercaseChar() }
        return MockFileInformation(filePath, statusCode = null, delay = null,
            if(fileName.lowercase().startsWith("error") || fileName.lowercase().startsWith("failure")) MockFileInformation.MockStatus.FAILURE
            else MockFileInformation.MockStatus.SUCCESS, displayName = displayName, relativePath = null)
    }
}