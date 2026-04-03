package io.github.letsee.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioFileInformation(var displayName: String, val steps: List<Step>) {
    @Serializable
    data class Step(val folder: String, val fileName: String)
}

@Serializable
data class ScenarioFileInformationExtended(var displayName: String? = null, val steps: List<Step>) {
    @Serializable
    data class Step(val folder: String, @SerialName("responseFileName") val fileName: String? = null, val state: String? = null)
}