package io.github.letsee.implementations

import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.Category
import io.github.letsee.models.Mock

fun appendSystemMocks(mocks: CategorisedMocks?): List<CategorisedMocks> {
    val generalMocks = CategorisedMocks(
        category = Category.GENERAL,
        mocks = listOf(
            Mock.LIVE,
            Mock.CANCEL,
            Mock.defaultSuccess("Custom Success", "{}".encodeToByteArray()),
            Mock.defaultFailure("Custom Failure", "{}".encodeToByteArray())
        )
    )
    return if (mocks != null) listOf(mocks, generalMocks) else listOf(generalMocks)
}
