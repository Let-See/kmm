package nl.codeface.letsee_kmm.MockImplementations

import nl.codeface.letsee_kmm.interfaces.FileNameCleaner

class MockFileNameCleaner(var result: String? = null): FileNameCleaner {
    override fun clean(filePath: String): String {
        return result ?: filePath
    }
}