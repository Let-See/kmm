package io.github.letsee

import io.github.letsee.implementations.appendSystemMocks
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.Category
import io.github.letsee.models.Mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemMocksTest {

    @Test
    fun `null input returns 1-element list with GENERAL category`() {
        val result = appendSystemMocks(null)

        assertEquals(1, result.size)
        assertEquals(Category.GENERAL, result[0].category)
    }

    @Test
    fun `non-null input returns 2-element list with SPECIFIC first and GENERAL second`() {
        val specific = CategorisedMocks(Category.SPECIFIC, emptyList())
        val result = appendSystemMocks(specific)

        assertEquals(2, result.size)
        assertEquals(Category.SPECIFIC, result[0].category)
        assertEquals(Category.GENERAL, result[1].category)
    }

    @Test
    fun `GENERAL block contains exactly LIVE CANCEL Custom Success Custom Failure`() {
        val result = appendSystemMocks(null)
        val generalMocks = result[0].mocks

        assertEquals(4, generalMocks.size)
        assertEquals(Mock.LIVE, generalMocks[0])
        assertEquals(Mock.CANCEL, generalMocks[1])
        assertEquals("Custom Success", generalMocks[2].name)
        assertTrue(generalMocks[2] is Mock.SUCCESS, "third mock should be SUCCESS")
        assertEquals("Custom Failure", generalMocks[3].name)
        assertTrue(generalMocks[3] is Mock.FAILURE, "fourth mock should be FAILURE")
    }

    @Test
    fun `non-null input preserves the SPECIFIC mocks content`() {
        val successMock = Mock.defaultSuccess("API Response", "{}".encodeToByteArray())
        val specific = CategorisedMocks(Category.SPECIFIC, listOf(successMock))
        val result = appendSystemMocks(specific)

        assertEquals(2, result.size)
        assertEquals(1, result[0].mocks.size)
        assertEquals(successMock.name, result[0].mocks[0].name)
    }

    @Test
    fun `GENERAL mocks from non-null input match GENERAL mocks from null input`() {
        val specific = CategorisedMocks(Category.SPECIFIC, emptyList())
        val resultWithSpecific = appendSystemMocks(specific)
        val resultWithNull = appendSystemMocks(null)

        val generalFromSpecific = resultWithSpecific[1].mocks
        val generalFromNull = resultWithNull[0].mocks

        assertEquals(generalFromNull.size, generalFromSpecific.size)
        assertEquals(generalFromNull[0], generalFromSpecific[0])
        assertEquals(generalFromNull[1], generalFromSpecific[1])
        assertEquals(generalFromNull[2].name, generalFromSpecific[2].name)
        assertEquals(generalFromNull[3].name, generalFromSpecific[3].name)
    }
}
