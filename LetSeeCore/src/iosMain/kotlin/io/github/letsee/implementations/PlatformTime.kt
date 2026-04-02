package io.github.letsee.implementations

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

actual fun currentTimestamp(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    return formatter.stringFromDate(NSDate())
}
