package nl.codeface.letsee_kmm.interfaces

import kotlinx.coroutines.flow.SharedFlow
import nl.codeface.letsee_kmm.CategorisedMocks
import nl.codeface.letsee_kmm.RequestStatus
import nl.codeface.letsee_kmm.Result
import nl.codeface.letsee_kmm.implementations.AcceptedRequest
import nl.codeface.letsee_kmm.models.Request

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
    suspend fun respond(request: Request)
    suspend fun update(request: Request, status: RequestStatus)
    suspend fun cancel(request: Request)
    suspend fun finish(request: Request)
}