package nl.codeface.letsee_kmm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import nl.codeface.letsee_kmm.models.CategorisedMocks
import nl.codeface.letsee_kmm.models.Category
import nl.codeface.letsee_kmm.implementations.DefaultDirectoryFilesFetcher
import nl.codeface.letsee_kmm.implementations.DefaultFileFetcher
import nl.codeface.letsee_kmm.implementations.DefaultMockProcessor
import nl.codeface.letsee_kmm.implementations.DefaultMocksDirectoryProcessor
import nl.codeface.letsee_kmm.implementations.DefaultRequestsManager
import nl.codeface.letsee_kmm.implementations.DefaultScenarioFileInformation
import nl.codeface.letsee_kmm.implementations.DefaultScenariosDirectoryProcessor
import nl.codeface.letsee_kmm.implementations.GlobalMockDirectoryConfiguration
import nl.codeface.letsee_kmm.implementations.JSONFileNameCleaner
import nl.codeface.letsee_kmm.implementations.JSONFileNameProcessor
import nl.codeface.letsee_kmm.implementations.exists
import nl.codeface.letsee_kmm.interfaces.Result
import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import nl.codeface.letsee_kmm.interfaces.DirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.FileDataFetcher
import nl.codeface.letsee_kmm.interfaces.FileNameCleaner
import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.interfaces.LetSee
import nl.codeface.letsee_kmm.interfaces.MockProcessor
import nl.codeface.letsee_kmm.interfaces.RequestsManager
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import nl.codeface.letsee_kmm.models.Request
import nl.codeface.letsee_kmm.models.Scenario

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
    private var config: Configuration = Configuration.default,
    val requestsManager: RequestsManager = DefaultRequestsManager()
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
    override fun setConfig(config: Configuration) {
        this.config = config
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