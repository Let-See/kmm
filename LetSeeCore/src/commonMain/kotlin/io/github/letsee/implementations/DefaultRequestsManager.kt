package io.github.letsee.implementations

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.RequestStatus
import io.github.letsee.interfaces.Result
import io.github.letsee.interfaces.RequestsManager
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.ScenarioManager
import io.github.letsee.models.Mock
import io.github.letsee.models.Request

class DefaultRequestsManager(
    override val onRequestAccepted: ((Request) -> Unit)? = null,
    override val onRequestRemoved:  ((Request) -> Unit)? = null,
    override val scenarioManager: ScenarioManager = DefaultScenarioManager()
) : RequestsManager {
    private val _requestsStack = MutableSharedFlow<List<AcceptedRequest>>(replay = 1)
    init {
        _requestsStack.tryEmit(emptyList())
    }
    override val requestsStack: SharedFlow<List<AcceptedRequest>>
        get() = _requestsStack.asSharedFlow()
    private val currentStack: List<AcceptedRequest>
        get() = _requestsStack.replayCache.first().toMutableList()
    override suspend fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
        val list = currentStack.toMutableList()
        list.add(AcceptedRequest(request, response = listener, RequestStatus.IDLE, mocks))
        _requestsStack.emit(list)
        onRequestAccepted?.let { it(request) }

        if (this.scenarioManager.isScenarioActive) {
            respondUsingScenario( request)
        }
    }

    override suspend fun respond(request: Request, withResponse: Response) {
        val queuedRequestIndex = indexOf(request) ?: return
        val queuedRequest = currentStack[queuedRequestIndex]
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
    override suspend fun respond(request: Request) {
        TODO("Not yet implemented")
    }
    private fun indexOf(request: Request): Int? {
         return currentStack.indexOfFirst { it.request.id == request.id }.let {index ->
             if(index>=0) {
                 index
             } else {
                 null
             }
         }
    }

    /**
    The `respondUsingScenario(request:)` function responds to a request using the current scenario.
    - Parameters:
    - request: The request to be responded to.
     */
    private suspend fun respondUsingScenario(request: Request) {
        scenarioManager.activeScenario.value?.currentStep?.let {
            this.respond(request,  it)
            this.scenarioManager.nextStep()
        } ?: scenarioManager.deactivateScenario()
    }

    /**
    Responds to a request in the queue with the given mock response.
    - Parameters:
    - request: The URL request to be responded to.
    - response: The mock response to use for the request.
     */
    override suspend fun respond(request: Request, withMockResponse: Mock) {
        when(withMockResponse) {
            is Mock.FAILURE -> {
                this.respond(request, withResponse = withMockResponse.response)
            }
            is Mock.SUCCESS -> {
                this.respond(request, withResponse = withMockResponse.response)
            }
            is Mock.CANCEL -> this.cancel(request)
            is Mock.LIVE -> this.respond(request)
            is Mock.ERROR -> this.cancel(request)
        }
        finish(request)
    }
    override suspend fun update(request: Request, status: RequestStatus) {
        indexOf(request)?.let {
            val item = currentStack[it]
                item.status = status
            currentStack[it].status = status
            _requestsStack.emit(currentStack)
        }
    }

    override suspend fun cancel(request: Request) {
        indexOf(request)?.let {
            this.update(request, RequestStatus.LOADING)
            this.currentStack[it].response.failure(DefaultResponse.CANCEL)
            this.finish(request)
        }
    }

    override suspend fun finish(request: Request) {
        indexOf(request)?.let {index ->
            val list = this.currentStack.toMutableList()
            list.removeAt(index)
            this._requestsStack.emit(list)
            onRequestRemoved?.let { it(request) }
        }
    }
}