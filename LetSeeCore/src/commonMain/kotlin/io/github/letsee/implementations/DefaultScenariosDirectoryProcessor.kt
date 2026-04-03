package io.github.letsee.implementations

import io.github.letsee.models.Mock
import io.github.letsee.models.MockFileInformation
import io.github.letsee.interfaces.DirectoryFilesFetcher
import io.github.letsee.interfaces.DirectoryProcessor
import io.github.letsee.interfaces.FileNameProcessor
import io.github.letsee.interfaces.ScenarioFileInformationProcessor
import io.github.letsee.models.Scenario

/**
 * Builds scenario object for all of scenario files in the given folder
 * Flow:
 *  1.  Retrieves all the files in the given directory
 *  2.  Decode those files to `ScenarioFileInformation` objects
 *  3.  Goes through all scenarios and their steps and for each step
 *      3.1 Looks up the folder name in the global config file and retrieves overridden address if it exists, and makes the Folder Key
 *      3.2 Obtains all the mocks for that Key
 *      3.3 Cleans the step's fileName property with the file name processor
 *      3.4 Searches for the cleaned file name in the Mocks that have been obtained in the 3.2
 *  4. Returns all the Scenario Objects
 */
class DefaultScenariosDirectoryProcessor(
    /// Retrieves all the files in the given folder
    private val directoryFilesFetcher: DirectoryFilesFetcher,

    /// Scenario Directory Processor needs th map the file name in the scenario object to the correct and cleaned file name
    /// exists in the mock folder.
    private val fileNameProcessor: FileNameProcessor<MockFileInformation>,

    /// Each mock file contains some information, more accurately a list of steps, processor passes the file path to the
    /// `ScenarioFileInformationProcessor` and receives the object represents the data in that scenario file
    private val scenarioFileInformationProcessor: ScenarioFileInformationProcessor,

    /// Scenario Processor needs to have access to global folder mapper, as it needs to map the folder name in the scenario step
    /// to the correct address to be able to get all the mocks related to that folder and ultimately map the step to the correct mock
    private val globalMockDirectoryConfig: (()->GlobalMockDirectoryConfiguration?)? = null,

    /// Each Scenario has one or more step, each step contains a folder name and a file. We need to map that look
    /// for that folder name in our mock and obtain it's mock, then processor searches the name of the file in the mocks
    /// and pulls the correct mock to that step
    private val scenarioFileNameToMockMapper: (String)->List<Mock>
) : DirectoryProcessor<Scenario> {
    override fun process(path: String): Map<String, List<Scenario>> {
        val scenarioFilesInDirectory = directoryFilesFetcher.getFiles(path, this.scenarioFileType())
        if (scenarioFilesInDirectory.isEmpty()){
            return emptyMap()
        }
        val availableScenarios = scenarioFilesInDirectory[scenarioFilesInDirectory.keys.first()]?.let { it ->
            it.mapNotNull { filePath -> scenarioFileInformationProcessor.process(filePath).let { scenarioInformation ->
                if (scenarioInformation == null) {
                    println("Could not parse scenario because of an issue in the $filePath")
                    null
                } else {
                    scenarioInformation
                }
            } }
        }
        val globalMockDirectoryConfig = globalMockDirectoryConfig?.let { it() }
        val scenarios = availableScenarios?.let {
            it.fold(mutableListOf<Scenario>()) { acc, current ->
                val listOfScenarioMock = mutableListOf<Mock>()
                current.steps.forEach { item ->
                    val normalizedFolderName = item.folder.mockKeyNormalised()

                    val overriddenPath = globalMockDirectoryConfig?.let {
                        it.hasMap(normalizedFolderName)?.to
                    }
                    val mockKey = if (overriddenPath != null)  overriddenPath + normalizedFolderName else normalizedFolderName
                    val mocks = scenarioFileNameToMockMapper(mockKey)
                    val fileName = fileNameProcessor.process("${item.folder}/${item.fileName.removeSuffix(".json")}.json")
                    val mock = mocks.firstOrNull { it.fileInformation?.displayName == fileName.displayName }
                    mock?.let {
                        listOfScenarioMock.add(mock)
                    }
                }
                acc.add(Scenario(current.displayName ?: "", listOfScenarioMock))
                acc
            }
        }
        return mapOf(path to (scenarios?.toList() ?: emptyList()))
    }
}

expect fun DefaultScenariosDirectoryProcessor.scenarioFileType(): String