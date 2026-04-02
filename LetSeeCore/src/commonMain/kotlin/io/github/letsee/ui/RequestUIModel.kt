package io.github.letsee.ui

import io.github.letsee.implementations.AcceptedRequest
import io.github.letsee.Configuration
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.Request
import io.github.letsee.models.RequestStatus
import io.github.letsee.models.displayName

data class RequestUIModel(
    val requestId: Int,
    val displayName: String,
    val status: RequestStatus,
    val categorisedMocks: List<CategorisedMocks>,
    val request: Request
)

fun AcceptedRequest.toUIModel(configuration: Configuration): RequestUIModel {
    return RequestUIModel(
        requestId = request.id,
        displayName = request.displayName(configuration),
        status = status,
        categorisedMocks = mocks.orEmpty(),
        request = request
    )
}
