package io.github.letsee

import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.interfaces.Response
import io.github.letsee.models.MockFileInformation

class M {
    class Strings {
        companion object {
            const val SUCCESS_FILE_NAME_EXPLICITLY: String = "success_some_2323112_23_file_name.json"
            const val IOS_SCENARIO_SUCCESS: String = "success_payment.plist"
            const val IOS_SCENARIO_FAILURE: String = "failure_payment.plist"
            const val SUCCESS_FILE_NAME_IMPLICITLY: String = "some_file_name_434_32.json"
            const val ERROR_FILE_NAME_IMPLICITLY: String = "error_some_32321ds_3_1_file_name.json"
            const val FAILURE_FILE_NAME_IMPLICITLY: String = "failure_some_333_file_name_2321_21_22.json"
            const val PATH: String = "/some/directory/to/some/"
            const val IOS_FileURI_PATH: String = "file://some/directory/to/some/where"
            const val ANDROID_FileURI_PATH: String = "file://some/directory/to/some/where"
        }
    }

    class Objects {
        companion object {
            val FAILURE_MOCK_INFORMATION: MockFileInformation = MockFileInformation(
                "${Strings.PATH}${Strings.FAILURE_FILE_NAME_IMPLICITLY}",
                statusCode = null,
                delay = null,
                MockFileInformation.MockStatus.FAILURE,
                Strings.FAILURE_FILE_NAME_IMPLICITLY,
                null
            )

            val SUCCESS_MOCK_INFORMATION: MockFileInformation = MockFileInformation(
                "${Strings.PATH}${Strings.SUCCESS_FILE_NAME_EXPLICITLY}",
                statusCode = null,
                delay = null,
                MockFileInformation.MockStatus.SUCCESS,
                Strings.SUCCESS_FILE_NAME_EXPLICITLY,
                null
            )

            val SUCCESS_MOCK_INFORMATION_ALL_PROPERTIES: MockFileInformation = MockFileInformation(
                "${Strings.PATH}${Strings.SUCCESS_FILE_NAME_EXPLICITLY}",
                statusCode = 200u,
                delay = 100,
                MockFileInformation.MockStatus.SUCCESS,
                Strings.SUCCESS_FILE_NAME_EXPLICITLY,
                "${Strings.PATH}${Strings.SUCCESS_FILE_NAME_EXPLICITLY}"
            )

            val SUCCESS_RESPONSE: Response = SUCCESS_MOCK_INFORMATION_ALL_PROPERTIES.mapToResponse()
            val FAILURE_RESPONSE: Response = FAILURE_MOCK_INFORMATION.mapToResponse()
        }
    }
}

fun MockFileInformation.mapToResponse(): Response {
    val fileInformation = this
    val statusCode = fileInformation.statusCode ?: if(fileInformation.status == MockFileInformation.MockStatus.SUCCESS) 200u else 400u
    return DefaultResponse(statusCode,statusCode,null,null,null, emptyMap())
}