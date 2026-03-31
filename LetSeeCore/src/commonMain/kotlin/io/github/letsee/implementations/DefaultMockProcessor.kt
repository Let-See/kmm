package io.github.letsee.implementations

import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.interfaces.FileDataFetcher
import io.github.letsee.interfaces.MockProcessor

class DefaultMockProcessor(private val fileDataFetcher: FileDataFetcher) : MockProcessor<MockFileInformation> {
    override fun process(fileInformation: MockFileInformation): Mock {
        val data = fileDataFetcher.getFileData(fileInformation.rawPath)
        return when(fileInformation.status) {
            MockFileInformation.MockStatus.FAILURE -> {
               Mock.FAILURE(fileInformation.displayName,
                   DefaultResponse(fileInformation.statusCode ?: 400u,
                   requestCode = 400u, data, null, null, emptyMap()), fileInformation)
            }
            MockFileInformation.MockStatus.SUCCESS -> {
                Mock.SUCCESS(fileInformation.displayName, DefaultResponse(fileInformation.statusCode ?: 200u,
                    requestCode = 200u, data, null, null, emptyMap()), fileInformation)
            }
        }
    }
}