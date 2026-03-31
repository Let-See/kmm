package io.github.letsee

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import io.github.letsee.implementations.exists
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
    override var mocks: Map<String, List<Mock>> = mapOf(),
    private val scenarioFileInformationProcessor: ScenarioFileInformationProcessor = DefaultScenarioFileInformation(),
    private val mocksDirectoryProcessor: DirectoryProcessor<Mock> = DefaultMocksDirectoryProcessor(
        fileNameProcessor, mockProcessor, directoryFilesFetcher
    ) { globalMockDirectoryConfig },
    private var scenariosDirectoryProcessor: DirectoryProcessor<Scenario>? = null,
    private var _config: MutableStateFlow<Configuration> = MutableStateFlow(Configuration.default),
    override val requestsManager: RequestsManager = DefaultRequestsManager()
    ): LetSee {
    private val scenarioFileNameToMockMapper: (String)->List<Mock> = {
        this.mocks[it] ?: emptyList()
    }
    init {
        scenariosDirectoryProcessor  = scenariosDirectoryProcessor ?: DefaultScenariosDirectoryProcessor(directoryFilesFetcher,
            fileNameProcessor,
            scenarioFileInformationProcessor,
            { this.globalMockDirectoryConfig },
            scenarioFileNameToMockMapper
        )
    }
    override val config: StateFlow<Configuration>
        get() = _config

    override fun setConfigurations(config: Configuration) {
        this._config.value = config
    }

    override var scenarios: List<Scenario> = listOf()
        private set

    override fun setMocks(path: String) {
        globalMockDirectoryConfig = GlobalMockDirectoryConfiguration.exists(inDirectory = path)
        mocks = mocksDirectoryProcessor.process(path)
    }
    override fun setScenarios(path: String) {
        val result = scenariosDirectoryProcessor?.process(path)
        result?.keys?.firstOrNull()?.let {
            result[it]?.let {scenarios ->
                this.scenarios = scenarios
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThreadDispatcher = Dispatchers.Default.limitedParallelism(1)

    override fun addRequest(request: Request, listener: Result) {
        CoroutineScope(singleThreadDispatcher).launch {
            requestsManager.accept(request, listener, mocks.keys.firstOrNull{it.startsWith(request.path)}.let { listOf(
                CategorisedMocks(Category.SPECIFIC, mocks[it] ?: emptyList())
            ) })
        }.start()
    }

    companion object {
        val letSee = DefaultLetSee()
        const val cdd = "ssss"
    }
}