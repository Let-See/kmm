package io.github.letsee.MockImplementations

import io.github.letsee.interfaces.FileNameCleaner

class MockFileNameCleaner(var result: String? = null): FileNameCleaner {
    override fun clean(filePath: String): String {
        return result ?: filePath
    }
}