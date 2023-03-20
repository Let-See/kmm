package nl.codeface.letsee_kmm.interfaces

import nl.codeface.letsee_kmm.models.Mock

/**
 * Mock Processor receives file information and map it the a `Mock`
 */
interface MockProcessor<T> {
    fun process(fileInformation: T): Mock
}