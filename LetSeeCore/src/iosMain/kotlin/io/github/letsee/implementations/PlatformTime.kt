@file:OptIn(ExperimentalForeignApi::class)

package io.github.letsee.implementations

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.posix.gettimeofday
import platform.posix.timeval

actual fun currentTimeMillis(): Long {
    return memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        (tv.tv_sec.toLong() * 1000L) + (tv.tv_usec.toLong() / 1000L)
    }
}

actual fun currentTimestamp(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    return formatter.stringFromDate(NSDate())
}
