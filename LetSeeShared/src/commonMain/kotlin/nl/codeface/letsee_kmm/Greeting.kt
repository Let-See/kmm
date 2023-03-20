package nl.codeface.letsee_kmm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import nl.codeface.letsee_kmm.interfaces.DirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.FileDataFetcher
import nl.codeface.letsee_kmm.interfaces.FileNameCleaner
import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.interfaces.MockProcessor
import nl.codeface.letsee_kmm.interfaces.RequestsManager
import nl.codeface.letsee_kmm.interfaces.Response
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import nl.codeface.letsee_kmm.models.Request
import nl.codeface.letsee_kmm.models.Scenario

interface Result {
    fun success(response: Response)
    fun failure(error: Response)
}
enum class RequestStatus {
    LOADING,
    IDLE,
    ACTIVE
}

enum class Category {
    GENERAL,
    SPECIFIC,
    SUGGESTED;
}
/**
 * The name of the category as a string.
 *
 * @return The name of the category.
 */
fun Category.name(): String {
     return when (this) {
         Category.GENERAL -> "General"
         Category.SPECIFIC -> "Specific"
         Category.SUGGESTED -> "Suggested"
     }
}

data class CategorisedMocks(
    // Category of the mocks, can be general or a specific scenario
    val category: Category,
    // List of mocks belonging to the category
    val mocks: List<Mock>
)

//interface LetSee {
//    /**
//     * The `Configuration` to be used by LetSee.
//     */
//    val configuration: Configuration
//
//    /**
//     * All available mocks that LetSee have found on the given mock directory
//     */
//    val mocks: Map<String, Set<Mock>>
//
//    /**
//     * All available scenarios that LetSee have found on the given scenario directory
//     */
//    val scenarios: List<Scenario>
//
//    /**
//     * A closure that is called when the mock state of the LetSee object changes. It takes a single argument, a Bool value indicating whether mock is enabled or not. It can be set or retrieved using the set and get functions.
//     */
//    var onMockStateChanged: StateFlow<Boolean>
//
//    val jsonFileParser: FileNameProcessor<MockFileInformation>
//    val interceptor: RequestsManager
//
//    /**
//     * Sets the given `Configuration` for LetSee.
//     *
//     * @param config the `Configuration` to be used by LetSee.
//     */
//    fun config(config: Configuration)
//
//    /**
//     * Adds mock files from the given path to LetSee.
//     *
//     * @param path the path of the directory that contains the mock files.
//     */
//    fun addMocks(from: String)
//
//    /**
//     * Adds the scenarios from the given directory path to the `scenarios` property of the `LetSee` instance.
//     *
//     * The `scenarios` property is a dictionary where each key is the name of the scenario file, and the value is an array of `LetSeeMock` objects that represent the mocks for each step of the scenario.
//     *
//     * The scenario files should be in the form of Property List (.plist) files, and should contain a top-level key called "steps" which is an array of dictionaries. Each dictionary should contain the following keys:
//     * - "folder": The name of the folder containing the mock data for this step.
//     * - "responseFileName": The name of the mock data file (with or without the "success" or "error" prefix).
//     *
//     * If the `LetSee` instance cannot find a mock data file with the given name and folder, it will print an error message and skip that step in the scenario.
//     *
//     * @param path The directory path where the scenario files are located.
//     */
//    fun addScenarios(from: String)
//
//    /**
//     * Runs a data task with the given request and calls the completion handler with the received data, response, and error.
//     *
//     * @param defaultSession The default session to be used to execute the task.
//     * @param request The request to run the data task with.
//     * @param completionHandler The completion handler to call with the received data, response, and error.
//     *
//     * @return The data task that would be run.
//     */
//    fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?)
//}

interface LetSee {
    val mocks: Map<String, List<Mock>>
    val scenarios: List<Scenario>
    fun setConfig(config: Configuration)
    fun setMocks(path: String)
    fun setScenarios(path: String)
    fun addRequest(request: Request, listener: Result)
}

class DefaultLetSee(
    private val fileDataFetcher: FileDataFetcher = DefaultFileFetcher(),
    private val fileNameCleaner: FileNameCleaner = JSONFileNameCleaner(),
    private val fileNameProcessor: FileNameProcessor<MockFileInformation> = JSONFileNameProcessor(fileNameCleaner),
    private val mockProcessor: MockProcessor<MockFileInformation> = DefaultMockProcessor(fileDataFetcher),
    private val directoryFilesFetcher: DirectoryFilesFetcher = DefaultDirectoryFilesFetcher(),
    private var globalMockDirectoryConfig: GlobalMockDirectoryConfiguration? = null,
    override var mocks: Map<String, List<Mock>> = mapOf(),
    private val scenarioFileInformationProcessor: ScenarioFileInformationProcessor = DefaultScenarioFileInformation(),
    private val mocksDirectoryProcessor: DirectoryProcessor<Mock> = DefaultMocksDirectoryProcessor(fileNameProcessor, mockProcessor, directoryFilesFetcher
    ) { globalMockDirectoryConfig },
    private var scenariosDirectoryProcessor: DirectoryProcessor<Scenario>? = null,
    private var config: Configuration = Configuration.default,
    val requestsManager: RequestsManager = DefaultRequestsManager()
    ): LetSee {
    private val scenarioFileNameToMockMapper: (String)->List<Mock> = {
        this.mocks[it] ?: emptyList()
    }
    init {
        scenariosDirectoryProcessor  = scenariosDirectoryProcessor ?: DefaultScenariosDirectoryProcessor(directoryFilesFetcher, fileNameProcessor, scenarioFileInformationProcessor,
            { this.globalMockDirectoryConfig }, scenarioFileNameToMockMapper)
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
            requestsManager.accept(request, listener, mocks.keys.firstOrNull{it.startsWith(request.path)}.let { listOf(CategorisedMocks(Category.SPECIFIC, mocks[it] ?: emptyList())) })
        }.start()
    }

    companion object {
        val letSee = DefaultLetSee()
        const val cdd = "ssss"
    }
}