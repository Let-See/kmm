package io.github.letsee.implementations

import platform.Foundation.NSTemporaryDirectory

actual fun defaultLogDirectory(): String {
    return NSTemporaryDirectory().trimEnd('/')
}
