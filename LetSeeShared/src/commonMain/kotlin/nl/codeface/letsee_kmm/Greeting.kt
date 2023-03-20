package nl.codeface.letsee_kmm

import kotlinx.coroutines.flow.StateFlow
import nl.codeface.letsee_kmm.models.Scenario
import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.interfaces.RequestsManager
import nl.codeface.letsee_kmm.interfaces.Response
import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import nl.codeface.letsee_kmm.models.Request

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

//class Interceptor(
//    override val scenarioManager: ScenarioManaging,
//    override val onRequestAccepted: StateFlow<Request>,
//    override val onRequestRemoved: StateFlow<Request>
//) : RequestsManaging {
//    private val _requestsQueue: MutableStateFlow<List<AcceptedRequest>> = MutableStateFlow(emptyList())
//    override val requestsQueue: StateFlow<List<AcceptedRequest>>
//        get() = _requestsQueue
//
//    override fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?) {
//        val queue = _requestsQueue.value.toMutableList()
//        queue.add(element = AcceptedRequest(request=request, response = listener, status = RequestStatus.IDLE, mocks = mocks))
//        _requestsQueue.value = queue
//    }
//}



interface LetSeeProtocol {
    /**
     * The `Configuration` to be used by LetSee.
     */
    val configuration: Configuration

    /**
     * All available mocks that LetSee have found on the given mock directory
     */
    val mocks: MutableMap<String, Set<Mock>>

    /**
     * All available scenarios that LetSee have found on the given scenario directory
     */
    val scenarios: List<Scenario>

    /**
     * A closure that is called when the mock state of the LetSee object changes. It takes a single argument, a Bool value indicating whether mock is enabled or not. It can be set or retrieved using the set and get functions.
     */
    var onMockStateChanged: StateFlow<Boolean>

    val jsonFileParser: FileNameProcessor<MockFileInformation>
    val interceptor: RequestsManager

    /**
     * Sets the given `Configuration` for LetSee.
     *
     * @param config the `Configuration` to be used by LetSee.
     */
    fun config(config: Configuration)

    /**
     * Adds mock files from the given path to LetSee.
     *
     * @param path the path of the directory that contains the mock files.
     */
    fun addMocks(from: String)

    /**
     * Adds the scenarios from the given directory path to the `scenarios` property of the `LetSee` instance.
     *
     * The `scenarios` property is a dictionary where each key is the name of the scenario file, and the value is an array of `LetSeeMock` objects that represent the mocks for each step of the scenario.
     *
     * The scenario files should be in the form of Property List (.plist) files, and should contain a top-level key called "steps" which is an array of dictionaries. Each dictionary should contain the following keys:
     * - "folder": The name of the folder containing the mock data for this step.
     * - "responseFileName": The name of the mock data file (with or without the "success" or "error" prefix).
     *
     * If the `LetSee` instance cannot find a mock data file with the given name and folder, it will print an error message and skip that step in the scenario.
     *
     * @param path The directory path where the scenario files are located.
     */
    fun addScenarios(from: String)

    /**
     * Runs a data task with the given request and calls the completion handler with the received data, response, and error.
     *
     * @param defaultSession The default session to be used to execute the task.
     * @param request The request to run the data task with.
     * @param completionHandler The completion handler to call with the received data, response, and error.
     *
     * @return The data task that would be run.
     */
    fun accept(request: Request, listener: Result, mocks: List<CategorisedMocks>?)
}

