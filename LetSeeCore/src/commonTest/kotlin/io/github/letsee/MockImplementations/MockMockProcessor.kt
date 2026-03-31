package io.github.letsee.MockImplementations

import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.interfaces.FileNameProcessor
import io.github.letsee.interfaces.MockProcessor

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
            ), fileInformation)
        } else {
            Mock.CANCEL
        }
    }
}