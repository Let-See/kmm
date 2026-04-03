package io.github.letsee.interfaces

interface LogStorage {
    val filePath: String
    fun append(text: String)
    fun clear()
}
