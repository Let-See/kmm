package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.FileNameCleaner
import platform.Foundation.NSString
import platform.Foundation.lastPathComponent
import platform.Foundation.stringByDeletingPathExtension

actual class JSONFileNameCleaner: FileNameCleaner {
    override fun clean(filePath: String): String {
        val filename: String = (filePath as NSString).stringByDeletingPathExtension
        return (filename as NSString).lastPathComponent
    }
}