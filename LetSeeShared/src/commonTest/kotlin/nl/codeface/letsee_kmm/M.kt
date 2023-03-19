package nl.codeface.letsee_kmm

class M {
    class Strings {
        companion object {
            const val SUCCESS_FILE_NAME_EXPLICITLY: String = "success_some_2323112_23_file_name.json"
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
        }
    }
}