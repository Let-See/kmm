package io.github.letsee.models

enum class Category {
    GENERAL,
    SPECIFIC,
    SUGGESTED;
}
/**
 * The name of the category as a string.
 *
 * @return The name of the category.
 */
fun Category.name(): String {
     return when (this) {
         Category.GENERAL -> "General"
         Category.SPECIFIC -> "Specific"
         Category.SUGGESTED -> "Suggested"
     }
}

data class CategorisedMocks(
    // Category of the mocks, can be general or a specific scenario
    val category: Category,
    // List of mocks belonging to the category
    val mocks: List<Mock>
)

