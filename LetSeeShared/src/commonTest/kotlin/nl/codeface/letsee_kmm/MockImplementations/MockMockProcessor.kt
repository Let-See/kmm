package nl.codeface.letsee_kmm.MockImplementations

import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import nl.codeface.letsee_kmm.implementations.DefaultResponse
import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.interfaces.MockProcessor

class MockMockProcessor(val fileNameProcessor: FileNameProcessor<MockFileInformation>, var results: List<Mock> = emptyList()) : MockProcessor<MockFileInformation> {
    private var index: Int = -1
    override fun process(fileInformation: MockFileInformation): Mock {
        index += 1
        return if (results.count() > index) { results[index] } else {
            makeDefault(fileInformation)
        }
    }
    private fun makeDefault(fileInformation: MockFileInformation?): Mock {
        return  if (fileInformation?.status == MockFileInformation.MockStatus.SUCCESS ) {
             Mock.SUCCESS(name = "default value", response = DefaultResponse(200u,200u,null, null, null,
                emptyMap()
            ), fileInformation!!)
        } else {
            Mock.CANCEL
        }
    }
}