package io.github.letsee.implementations

import io.github.letsee.interfaces.FileNameCleaner

expect class JSONFileNameCleaner(): FileNameCleaner {
    override fun clean(filePath: String): String
}