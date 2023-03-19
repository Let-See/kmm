package nl.codeface.letsee_kmm

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