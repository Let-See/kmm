package io.github.letsee.interfaces

import kotlinx.coroutines.flow.StateFlow
import io.github.letsee.Configuration
import io.github.letsee.models.Mock
import io.github.letsee.models.Request
import io.github.letsee.models.Scenario

interface LetSee {
    val config: StateFlow<Configuration>
    /**
     * All available mocks that LetSee have found on the given mock directory
     */
    val mocks: Map<String, List<Mock>>
    /**
     * All available scenarios that LetSee have found on the given scenario directory
     */
    val scenarios: List<Scenario>
    /**
     * Sets the given `Configuration` for LetSee.
     *
     * @param config the `Configuration` to be used by LetSee.
     */
    fun setConfigurations(config: Configuration)
    /**
     * Adds mock files from the given path to LetSee.
     *
     * @param path the path of the directory that contains the mock files.
     */
    fun setMocks(path: String)
    /**
     * Adds the scenarios from the given directory path to the `scenarios` property of the `LetSee` instance.
     *
     * The `scenarios` property is a dictionary where each key is the name of the scenario file, and the value is an array of `LetSeeMock` objects that represent the mocks for each step of the scenario.
     *
     * The scenario files should be in the form of Property List (.plist) files, and should contain a top-level key called "steps" which is an array of dictionaries. Each dictionary should contain the following keys:
     * - "folder": The name of the folder containing the mock data for this step.
     * - "fileName": The name of the mock data file (with or without the "success" or "error" prefix).
     *
     * If the `LetSee` instance cannot find a mock data file with the given name and folder, it will print an error message and skip that step in the scenario.
     *
     * @param path The directory path where the scenario files are located.
     */
    fun setScenarios(path: String)
    /**
     * Runs a data task with the given request and calls the completion handler with the received data, response, and error.
     *
     * @param request The request to run the data task with.
     * @param listener The completion handler to call with the received data, response, and error.
     *
     * @return The data task that would be run.
     */
    fun addRequest(request: Request, listener: Result)

    val requestsManager: RequestsManager
}