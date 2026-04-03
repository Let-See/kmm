package nl.codeface.letsee_kmm.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Breed(
    val id: String,
    val name: String,
    val description: String = "",
    val origin: String = "",
    val temperament: String = "",
    @SerialName("wikipedia_url") val wikipediaUrl: String? = null,
    @SerialName("life_span") val lifeSpan: String? = null,
    val weight: Weight? = null,
    val image: BreedImage? = null,
)

@Serializable
data class Weight(
    val imperial: String? = null,
    val metric: String? = null,
)

@Serializable
data class BreedImage(
    val id: String? = null,
    val url: String? = null,
)
