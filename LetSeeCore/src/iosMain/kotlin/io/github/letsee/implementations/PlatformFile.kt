@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.implementations

import kotlinx.cinterop.memScoped
import kotlinx.cinterop.cstr
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
    memScoped {
        val file = fopen(path.cstr.ptr, "a".cstr.ptr) ?: return
        fputs(text.cstr.ptr, file)
        fclose(file)
    }
}

actual fun deleteFile(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}
