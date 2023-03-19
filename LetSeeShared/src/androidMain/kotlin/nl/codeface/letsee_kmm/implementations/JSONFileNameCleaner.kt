package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.FileNameCleaner
import java.io.File

actual class JSONFileNameCleaner: FileNameCleaner {
    override fun clean(filePath: String): String {
        return File(filePath).nameWithoutExtension
    }
}