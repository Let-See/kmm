package io.github.letsee.implementations

import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.RequestStatus
import io.github.letsee.interfaces.Result
import io.github.letsee.models.Request

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