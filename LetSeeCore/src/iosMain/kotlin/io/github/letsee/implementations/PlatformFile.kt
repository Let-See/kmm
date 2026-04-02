@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.implementations

import platform.Foundation.NSFileManager
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

actual fun appendToFile(path: String, text: String) {
    val fm = NSFileManager.defaultManager
    val parentDir = path.substringBeforeLast("/")
    if (parentDir.isNotEmpty() && !fm.fileExistsAtPath(parentDir)) {
        fm.createDirectoryAtPath(parentDir, withIntermediateDirectories = true, attributes = null, error = null)
    }
    if (!fm.fileExistsAtPath(path)) {
        fm.createFileAtPath(path, contents = null, attributes = null)
    }
    val file = fopen(path, "a") ?: return
    fputs(text, file)
    fclose(file)
}

actual fun deleteFile(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}
