package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import nl.codeface.letsee_kmm.interfaces.FileDataFetcher
import nl.codeface.letsee_kmm.interfaces.MockProcessor

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