package io.github.letsee.interfaces

import io.github.letsee.models.Mock

/**
 * Mock Processor receives file information and map it the a `Mock`
 */
interface MockProcessor<T> {
    fun process(fileInformation: T): Mock
}