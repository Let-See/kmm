package nl.codeface.letsee_kmm.models

import nl.codeface.letsee_kmm.models.Mock

data class Scenario (
    val name: String,
    val mocks: List<Mock>,
    private var currentIndex: Int = 0
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

    /**
     * Moves the cursor to the next mock, when the request received the current mock, this function should be called.
     */
     fun nextStep(): Mock? {
        return if (currentIndex < mocks.size) {
            val mock = mocks[currentIndex]
            currentIndex++
            mock
        } else {
            null
        }
    }
}