package io.github.letsee.implementations

actual fun appendToFile(path: String, text: String) {
    java.io.File(path).appendText(text)
}

actual fun deleteFile(path: String) {
    java.io.File(path).delete()
}
