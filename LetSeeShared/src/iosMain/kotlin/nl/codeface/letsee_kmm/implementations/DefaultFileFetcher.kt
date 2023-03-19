package nl.codeface.letsee_kmm.implementations

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import nl.codeface.letsee_kmm.interfaces.FileDataFetcher
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

actual class DefaultFileFetcher: FileDataFetcher {
   actual override fun getFileData(filePath: String): ByteArray? {
       val data = NSData.dataWithContentsOfURL(NSURL(fileURLWithPath = filePath))
       return data?.let {
           ByteArray(data.length.toInt()).apply {
               usePinned {
                   memcpy(it.addressOf(0), data.bytes, data.length)
               }
           }
       }
   }
}

fun ByteArray.toData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toData),
        length = this@toData.size.toULong())
}