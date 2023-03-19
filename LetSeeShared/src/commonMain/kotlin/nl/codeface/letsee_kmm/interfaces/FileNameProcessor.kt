package nl.codeface.letsee_kmm.interfaces

import nl.codeface.letsee_kmm.MockFileInformation

interface FileNameProcessor<out T> {
    /// /some/dile/address/200_20ms_some_file.json -> 200 status code, 20 ms delay, some_file displayName,
    fun process(filePath: String): T
}