package nl.codeface.letsee_kmm.models

import nl.codeface.letsee_kmm.models.Mock

data class Scenario (
    val name: String,
    val mocks: List<Mock>,
    val currentIndex: Int = 0
) {
    /**
     * Shows the current step of the flow, it means that the next request will be received this as it's response
     */
    val currentStep: Mock?
        get() {
            return if (currentIndex < mocks.size) {
                mocks[currentIndex]
            } else {
                null
            }
        }
}