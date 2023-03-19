package nl.codeface.letsee_kmm.interfaces

import nl.codeface.letsee_kmm.Mock
import nl.codeface.letsee_kmm.MockFileInformation

interface MockProcessor<T> {
    fun process(fileInformation: T): Mock
}