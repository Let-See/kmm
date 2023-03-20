package nl.codeface.letsee_kmm.interfaces

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import nl.codeface.letsee_kmm.AcceptedRequest
import nl.codeface.letsee_kmm.CategorisedMocks
import nl.codeface.letsee_kmm.RequestStatus
import nl.codeface.letsee_kmm.Result
import nl.codeface.letsee_kmm.ScenarioManager
import nl.codeface.letsee_kmm.implementations.DefaultResponse
import nl.codeface.letsee_kmm.models.Request

/**
 * Requests Manager intercepts the requests and collects them into a stack (last-in, first-out), once the answer to that request is ready like when the user
 * selects the specific response, it fulfills the top requests with the selected response and removes it from the stack.
 */
interface RequestsManager {
//    val scenarioManager: ScenarioManager
    val requestsStack: StateFlow<List<AcceptedRequest>>
    val onRequestAccepted:  ((Request) -> Unit)?
    val onRequestRemoved:  ((Request) -> Unit)?
    fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?)
    fun respond(request: Request, withResponse: Response)
    fun respond(request: Request)
    fun update(request: Request, status: RequestStatus)
    fun cancel(request: Request)
    fun finish(request: Request)
}

data class AcceptedRequest(var request: Request, var response: Result, var status: RequestStatus, var mocks: List<CategorisedMocks>?)

class DefaultRequestsManager(
    override val onRequestAccepted: ((Request) -> Unit)? = null,
    override val onRequestRemoved:  ((Request) -> Unit)? = null
) : RequestsManager {
    private val _requestsStack = MutableStateFlow<List<AcceptedRequest>>(emptyList())
    override val requestsStack: StateFlow<List<AcceptedRequest>>
        get() = _requestsStack

    override fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
        val list = _requestsStack.value.toMutableList()
        list.add(AcceptedRequest(request, response = listener, RequestStatus.IDLE, mocks))
        _requestsStack.value = list
        onRequestAccepted?.let { it(request) }
    }

    override fun respond(request: Request, withResponse: Response) {
        val queuedRequestIndex = indexOf(request) ?: return
        val queuedRequest = requestsStack.value[queuedRequestIndex]
        when (withResponse.responseCode) {
            in (200u..299u) -> {
                queuedRequest.response.success(withResponse)
            }
            else -> {
                queuedRequest.response.failure(withResponse)
            }
        }
        finish(request)
    }

    // Live To Server
    override fun respond(request: Request) {
        TODO("Not yet implemented")
    }
    private fun indexOf(request: Request): Int? {
         return _requestsStack.value.indexOfFirst { it.request == request }.let {index ->
             if(index>=0) {
                 index
             } else {
                 null
             }
         }
    }

    override fun update(request: Request, status: RequestStatus) {
        indexOf(request)?.let {
            _requestsStack.value[it].status = status
        }
    }

    override fun cancel(request: Request) {
        indexOf(request)?.let {
            this.update(request, RequestStatus.LOADING)
            this._requestsStack.value[it].response.failure(DefaultResponse.CANCEL)
            this.finish(request)
        }
    }

    override fun finish(request: Request) {
        indexOf(request)?.let {index ->
            val list = this._requestsStack.value.toMutableList()
            list.removeAt(index)
            this._requestsStack.value = list
            onRequestRemoved?.let { it(request) }
        }
    }
}