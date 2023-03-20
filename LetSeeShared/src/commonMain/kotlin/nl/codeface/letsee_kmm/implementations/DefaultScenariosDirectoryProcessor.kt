package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.models.Mock
import nl.codeface.letsee_kmm.models.MockFileInformation
import nl.codeface.letsee_kmm.Scenario
import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import nl.codeface.letsee_kmm.interfaces.DirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.FileNameProcessor
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor


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
    private val globalMockDirectoryConfig: GlobalMockDirectoryConfiguration? = null,

    /// Each Scenario has one or more step, each step contains a folder name and a file. We need to map that look
    /// for that folder name in our mock and obtain it's mock, then processor searches the name of the file in the mocks
    /// and pulls the correct mock to that step
    private val scenarioFileNameToMockMapper: (String)->List<Mock>
) : DirectoryProcessor<Scenario> {
    override fun process(path: String): Map<String, List<Scenario>> {
        val mocksInDirectory = directoryFilesFetcher.getFiles(path, this.scenarioFileType())
        if (mocksInDirectory.isEmpty()){
            return emptyMap()
        }
        val availableScenarios = mocksInDirectory[mocksInDirectory.keys.first()]?.let { it ->
            it.mapNotNull { filePath -> scenarioFileInformationProcessor.process(filePath).let { scenarioInformation ->
                if (scenarioInformation == null) {
                    println("Could not parse scenario because of an issue in the $filePath")
                    null
                } else {
                    scenarioInformation
                }
            } }
        }
        val scenarios = availableScenarios?.let {
            it.fold(mutableListOf<Scenario>()) { acc, current ->
                val listOfScenarioMock = mutableListOf<Mock>()
                current.steps.forEach { item ->
                    val normalizedFolderName = item.folder.mockKeyNormalised()
                    val overriddenPath = globalMockDirectoryConfig?.hasMap(normalizedFolderName)?.to
                    val mockKey = if (overriddenPath != null)  overriddenPath + normalizedFolderName else normalizedFolderName
                    val mocks = scenarioFileNameToMockMapper(mockKey)
                    val fileName = fileNameProcessor.process("${item.folder}/${item.fileName.removeSuffix(".json")}.json")
                    val mock = mocks.firstOrNull { it.fileInformation?.displayName == fileName.displayName }
                    println("@${fileName} @" + mocks.map { it.fileInformation?.rawPath })
                    mock?.let {
                        listOfScenarioMock.add(mock)
                    }
                }
                acc.add(Scenario(current.displayName, listOfScenarioMock))
                acc
            }
        }
        return mapOf(path to (scenarios?.toList() ?: emptyList()))
    }
}

expect fun DefaultScenariosDirectoryProcessor.scenarioFileType(): String