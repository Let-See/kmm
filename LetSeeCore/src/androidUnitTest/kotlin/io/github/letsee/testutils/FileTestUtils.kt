package io.github.letsee.testutils

actual fun readFileForTest(path: String): String? {
    return try {
        java.io.File(path).takeIf { it.exists() }?.readText()
    } catch (_: Exception) {
        null
    }
}
