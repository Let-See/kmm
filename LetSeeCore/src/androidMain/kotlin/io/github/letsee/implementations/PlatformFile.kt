package io.github.letsee.implementations

actual fun appendToFile(path: String, text: String) {
    val file = java.io.File(path)
    file.parentFile?.mkdirs()
    file.appendText(text)
}

actual fun deleteFile(path: String) {
    java.io.File(path).delete()
}
