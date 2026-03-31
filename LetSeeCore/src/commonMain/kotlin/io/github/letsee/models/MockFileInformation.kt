package io.github.letsee.models

data class MockFileInformation(
    val rawPath: String,
    val statusCode: UInt?,
    val delay: Long?,
    val status: MockStatus,
    val displayName: String,
    val relativePath: String?
) {
    enum class MockStatus {
        SUCCESS, FAILURE
    }
}