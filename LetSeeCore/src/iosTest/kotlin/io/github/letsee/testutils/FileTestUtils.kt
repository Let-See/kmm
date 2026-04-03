@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.testutils

import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

actual fun readFileForTest(path: String): String? {
    return try {
        NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
            ?.toString()
    } catch (_: Exception) {
        null
    }
}
