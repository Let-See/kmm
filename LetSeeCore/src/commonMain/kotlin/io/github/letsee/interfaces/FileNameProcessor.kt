package io.github.letsee.interfaces

/**
 * File Name Processor receives a file path, and extracts useful information out of the file name. The implementor specifies the type of information
 * for example we can have JSON file, Plist file and...
 */
interface FileNameProcessor<out T> {
    /**
     * extracts useful information out of the file name
     * /some/dile/address/200_20ms_some_file.json -> 200 status code, 20 ms delay, some_file displayName,
     */
    fun process(filePath: String): T
}