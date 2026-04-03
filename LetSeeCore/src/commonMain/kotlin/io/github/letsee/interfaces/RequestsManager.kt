package io.github.letsee.interfaces

import kotlinx.coroutines.flow.SharedFlow
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.RequestStatus
import io.github.letsee.implementations.AcceptedRequest
import io.github.letsee.models.Mock
import io.github.letsee.models.Request

/**
 * Requests Manager intercepts the requests and collects them into a stack (last-in, first-out), once the answer to that request is ready like when the user
 * selects the specific response, it fulfills the top requests with the selected response and removes it from the stack.
 */
interface RequestsManager {
    val scenarioManager: ScenarioManager
    val requestsStack: SharedFlow<List<AcceptedRequest>>
    val onRequestAccepted:  ((Request) -> Unit)?
    val onRequestRemoved:  ((Request) -> Unit)?
    suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?)
    suspend fun respond(request: Request, withResponse: Response)
    suspend fun respond(request: Request, withMockResponse: Mock)
    suspend fun respond(request: Request)
    suspend fun update(request: Request, status: RequestStatus)
    suspend fun cancel(request: Request)
    suspend fun finish(request: Request)
}