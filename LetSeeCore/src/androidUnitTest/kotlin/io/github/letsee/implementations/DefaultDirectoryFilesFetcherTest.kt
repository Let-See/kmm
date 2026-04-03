package io.github.letsee.implementations

import org.junit.Before
import org.junit.Test

class DefaultDirectoryFilesFetcherTest {
    private lateinit var sut: DefaultDirectoryFilesFetcher

    @Before
    fun setup() {
        sut = DefaultDirectoryFilesFetcher()
    }
    @Test
    fun testGetFiles() {
        var result = sut.getFiles(javaClass.classLoader?.getResource("mocks")?.path ?: "", "json")

    }
}