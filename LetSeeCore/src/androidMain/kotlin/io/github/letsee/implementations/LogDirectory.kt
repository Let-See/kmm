package io.github.letsee.implementations

actual fun defaultLogDirectory(): String {
    return androidContext?.cacheDir?.absolutePath
        ?: System.getProperty("java.io.tmpdir")
        ?: "/tmp"
}
