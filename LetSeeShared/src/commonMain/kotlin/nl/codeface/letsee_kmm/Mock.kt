package nl.codeface.letsee_kmm

import nl.codeface.letsee_kmm.interfaces.Response

sealed class Mock(open val name: String, open val response: Response? = null, open val fileInformation: MockFileInformation? = null) {
    class FAILURE(override val name: String, override val response: Response, override val fileInformation: MockFileInformation): Mock(name, response, fileInformation) {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is Mock.FAILURE -> {
                    this.name == other.name &&
                            this.response == other.response &&
                            this.fileInformation == other.fileInformation
                }

                else -> {
                    super.equals(other)
                }
            }
        }
    }
    class SUCCESS(override val name: String, override val response: Response, override val fileInformation: MockFileInformation): Mock(name, response, fileInformation) {
        override fun equals(other: Any?): Boolean {
            return when(other){
                is Mock.SUCCESS -> {
                    this.name == other.name &&
                    this.response == other.response &&
                    this.fileInformation == other.fileInformation
                }
                else -> {
                    super.equals(other)
                }
            }
        }
    }
    class ERROR(override val name: String, val error: Throwable): Mock(name)  {
        override fun equals(other: Any?): Boolean {
            return when(other){
                is Mock.ERROR -> {
                    this.name == other.name &&
                            this.error == other.error
                }
                else -> {
                    super.equals(other)
                }
            }
        }
    }
    object LIVE: Mock(name = "LIVE") {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is Mock.LIVE -> {
                    true
                }
                else -> {
                    super.equals(other)
                }
            }
        }
    }
    object CANCEL: Mock("CANCEL") {
        override fun equals(other: Any?): Boolean {
            return when (other) {
                is Mock.CANCEL -> {
                    true
                }
                else -> {
                    super.equals(other)
                }
            }
        }
    }
}