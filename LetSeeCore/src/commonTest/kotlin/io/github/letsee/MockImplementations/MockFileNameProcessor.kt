package io.github.letsee.MockImplementations

import io.github.letsee.M
import io.github.letsee.models.MockFileInformation
import io.github.letsee.interfaces.FileNameCleaner
import io.github.letsee.interfaces.FileNameProcessor

class MockFileNameProcessor(val cleaner: FileNameCleaner, var result: List<MockFileInformation> = emptyList(), var default: MockFileInformation = MockFileInformation(
    "${M.Strings.PATH}${M.Strings.FAILURE_FILE_NAME_IMPLICITLY}",
    statusCode = null,
    delay = null,
    MockFileInformation.MockStatus.FAILURE,
    M.Strings.FAILURE_FILE_NAME_IMPLICITLY,
    null
)
): FileNameProcessor<MockFileInformation> {
    private var index: Int = -1
    override fun process(filePath: String): MockFileInformation {
        index += 1
        return if (result.count() > index) { result[index] } else { default }
    }
}