package io.github.letsee.ui

import io.github.letsee.Configuration
import io.github.letsee.interfaces.RequestsManager
import io.github.letsee.models.Mock
import io.github.letsee.models.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RequestListStateHolder(
    private val requestsManager: RequestsManager,
    config: StateFlow<Configuration>,
    private val coroutineScope: CoroutineScope
) {
    val requests: StateFlow<List<RequestUIModel>> =
        requestsManager.requestsStack
            .combine(config) { accepted, cfg ->
                accepted.map { it.toUIModel(cfg) }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectMock(request: Request, mock: Mock) {
        coroutineScope.launch {
            requestsManager.respond(request, withMockResponse = mock)
        }
    }

    fun respondLive(request: Request) {
        coroutineScope.launch {
            requestsManager.respond(request)
        }
    }
}
