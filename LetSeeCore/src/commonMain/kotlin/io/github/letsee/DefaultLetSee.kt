package io.github.letsee

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import io.github.letsee.models.CategorisedMocks
import io.github.letsee.models.Category
import io.github.letsee.implementations.DefaultDirectoryFilesFetcher
import io.github.letsee.implementations.DefaultFileFetcher
import io.github.letsee.implementations.DefaultMockProcessor
import io.github.letsee.implementations.DefaultMocksDirectoryProcessor
import io.github.letsee.implementations.DefaultRequestsManager
import io.github.letsee.implementations.DefaultScenarioFileInformation
import io.github.letsee.implementations.DefaultScenariosDirectoryProcessor
import io.github.letsee.implementations.GlobalMockDirectoryConfiguration
import io.github.letsee.implementations.JSONFileNameCleaner
import io.github.letsee.implementations.JSONFileNameProcessor
import io.github.letsee.implementations.appendSystemMocks
import io.github.letsee.implementations.exists
import io.github.letsee.implementations.mockKeyNormalised
import io.github.letsee.implementations.LoggingResult
import io.github.letsee.implementations.currentTimeMillis
import io.github.letsee.interfaces.RequestResponseLogger
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import io.github.letsee.interfaces.DirectoryFilesFetcher
import io.github.letsee.interfaces.DirectoryProcessor
import io.github.letsee.interfaces.FileDataFetcher
import io.github.letsee.interfaces.FileNameCleaner
import io.github.letsee.interfaces.FileNameProcessor
import io.github.letsee.interfaces.LetSee
import io.github.letsee.interfaces.MockProcessor
import io.github.letsee.interfaces.RequestsManager
import io.github.letsee.interfaces.ScenarioFileInformationProcessor
import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.models.Request
import io.github.letsee.models.Scenario

class DefaultLetSee(
    private val fileDataFetcher: FileDataFetcher = DefaultFileFetcher(),
    private val fileNameCleaner: FileNameCleaner = JSONFileNameCleaner(),
    private val fileNameProcessor: FileNameProcessor<MockFileInformation> = JSONFileNameProcessor(
        fileNameCleaner
    ),
    private val mockProcessor: MockProcessor<MockFileInformation> = DefaultMockProcessor(
        fileDataFetcher
    ),
    private val directoryFilesFetcher: DirectoryFilesFetcher = DefaultDirectoryFilesFetcher(),
    private var globalMockDirectoryConfig: GlobalMockDirectoryConfiguration? = null,
    @Volatile override var mocks: Map<String, List<Mock>> = mapOf(),
    private val scenarioFileInformationProcessor: ScenarioFileInformationProcessor = DefaultScenarioFileInformation(),
    private val mocksDirectoryProcessor: DirectoryProcessor<Mock> = DefaultMocksDirectoryProcessor(
        fileNameProcessor, mockProcessor, directoryFilesFetcher, fileDataFetcher
    ) { globalMockDirectoryConfig },
    private var scenariosDirectoryProcessor: DirectoryProcessor<Scenario>? = null,
    private var _config: MutableStateFlow<Configuration> = MutableStateFlow(Configuration.default),
    override val requestsManager: RequestsManager = DefaultRequestsManager(),
    private val dispatcher: CoroutineDispatcher = defaultDispatcher(),
    private val onCoroutineError: (Throwable) -> Unit = { println("[DefaultLetSee] Coroutine exception: ${it.stackTraceToString()}") },
    override val logger: RequestResponseLogger? = null
    ): LetSee {
    @Volatile override var liveRequestHandler: (suspend (Request) -> Response)? = null

    private val scenarioFileNameToMockMapper: (String)->List<Mock> = {
        this.mocks[it] ?: emptyList()
    }
    init {
        (requestsManager as? DefaultRequestsManager)?.let {
            it.liveRequestHandlerProvider = { this.liveRequestHandler }
        } ?: println("[DefaultLetSee] Warning: requestsManager is not DefaultRequestsManager — live request forwarding disabled")
        scenariosDirectoryProcessor  = scenariosDirectoryProcessor ?: DefaultScenariosDirectoryProcessor(directoryFilesFetcher,
            fileNameProcessor,
            scenarioFileInformationProcessor,
            { this.globalMockDirectoryConfig },
            scenarioFileNameToMockMapper
        )
    }
    override val config: StateFlow<Configuration>
        get() = _config

    override val mockStateChanged: Flow<Boolean> = _config
        .map { it.isMockEnabled }
        .distinctUntilChanged()
        .drop(1)

    override fun setConfigurations(config: Configuration) {
        this._config.value = config
    }

    override var scenarios: List<Scenario> = listOf()
        private set

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onCoroutineError(throwable)
    }

    // SupervisorJob prevents one failed child coroutine from cancelling siblings.
    // addRequest reads and setMocks writes are confined to this single-threaded scope.
    // The public getter is protected by @Volatile for cross-thread visibility.
    // Note: the listener (Result) is not notified if requestsManager.accept throws —
    // the exception routes to onCoroutineError. Full listener error reporting requires
    // an error-Response factory (deferred).
    private val scope = CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)

    /** Cancels the coroutine scope when this instance is no longer needed. */
    fun close() {
        scope.cancel()
    }

    // setMocks is asynchronous: the mocks map is populated after this method returns.
    // Call during app initialization, before addRequest. The @Volatile backing field
    // ensures memory visibility for concurrent reads from the public getter.
    override fun setMocks(path: String) {
        globalMockDirectoryConfig = GlobalMockDirectoryConfiguration.exists(inDirectory = path)
        val newMocks = mocksDirectoryProcessor.process(path)
        scope.launch { mocks = newMocks }
    }

    override fun setScenarios(path: String) {
        val result = scenariosDirectoryProcessor?.process(path)
        result?.keys?.firstOrNull()?.let {
            result[it]?.let {scenarios ->
                this.scenarios = scenarios
            }
        }
    }

    override fun addRequest(request: Request, listener: Result) {
        scope.launch {
            val normalizedPath = request.path
                .substringBefore("?")
                .split("/")
                .filter { it.isNotEmpty() }
                .joinToString("/")
                .mockKeyNormalised()
            val requestWithHeader = if (request.headers.containsKey(Request.LOGGER_HEADER_KEY)) {
                request
            } else {
                HeaderInjectedRequest(
                    delegate = request,
                    headers = request.headers + (Request.LOGGER_HEADER_KEY to request.logId)
                )
            }
            val effectiveListener = if (logger != null) {
                runCatching { logger.logRequest(requestWithHeader) }
                LoggingResult(listener, logger, requestWithHeader, currentTimeMillis())
            } else {
                listener
            }
            requestsManager.accept(requestWithHeader, effectiveListener,
                appendSystemMocks(CategorisedMocks(Category.SPECIFIC, mocks[normalizedPath] ?: emptyList()))
            )
        }
    }

    override fun activateScenario(scenario: Scenario) {
        scope.launch { requestsManager.scenarioManager.activate(scenario) }
    }

    override fun deactivateScenario() {
        scope.launch { requestsManager.scenarioManager.deactivateScenario() }
    }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

        val letSee = DefaultLetSee()
    }
}

private class HeaderInjectedRequest(
    private val delegate: Request,
    override val headers: Map<String, String>
) : Request by delegate