package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.CategorisedMocks
import nl.codeface.letsee_kmm.RequestStatus
import nl.codeface.letsee_kmm.Result
import nl.codeface.letsee_kmm.models.Request

data class AcceptedRequest(var request: Request, var response: Result, var status: RequestStatus, var mocks: List<CategorisedMocks>?) {
    override fun equals(other: Any?): Boolean {
        return when(other) {
            is AcceptedRequest -> {
                other.request == this.request && status == other.status && mocks == other.mocks
            }
            else -> {
                super.equals(other)
            }
        }
    }
}