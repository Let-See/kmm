package nl.codeface.letsee_kmm.models

import kotlinx.serialization.Serializable

@Serializable
data class ScenarioFileInformation(val displayName: String, val steps: List<Step>) {
    @Serializable
    data class Step(val folder: String, val fileName: String)
}