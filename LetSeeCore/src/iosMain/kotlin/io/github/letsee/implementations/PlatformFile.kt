@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.implementations

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.create

actual fun appendToFile(path: String, text: String) {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createFileAtPath(path, contents = null, attributes = null)
    }
    val handle = NSFileHandle.fileHandleForWritingAtPath(path) ?: return
    handle.seekToEndOfFile()
    val bytes = text.encodeToByteArray()
    bytes.usePinned { pinned ->
        val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        handle.writeData(data)
    }
    handle.closeFile()
}

actual fun deleteFile(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}
