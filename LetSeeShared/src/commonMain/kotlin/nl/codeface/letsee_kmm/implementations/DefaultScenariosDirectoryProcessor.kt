package nl.codeface.letsee_kmm.implementations

import nl.codeface.letsee_kmm.interfaces.DirectoryFilesFetcher
import nl.codeface.letsee_kmm.interfaces.DirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.ScenarioFileInformation

class DefaultScenariosDirectoryProcessor(
    private val directoryFilesFetcher: DirectoryFilesFetcher,
    private val scenarioFileInformationProcessor: ScenarioFileInformationProcessor
) : DirectoryProcessor<ScenarioFileInformation> {
    override fun process(path: String): Map<String, List<ScenarioFileInformation>> {
        val mocksInDirectory = directoryFilesFetcher.getFiles(path, this.scenarioFileType())
        if (mocksInDirectory.isEmpty()){
            return emptyMap()
        }
        val availableScenarios = mocksInDirectory[mocksInDirectory.keys.first()]?.let { it ->
            mapOf(path to it.map { filePath -> scenarioFileInformationProcessor.process(filePath).let { scenarioInformation ->
                if (scenarioInformation == null) {
                    println("Could not parse scenarios because of an issue in the $filePath")
                    return emptyMap()
                } else {
                    scenarioInformation
                }
            } })
        }
        return availableScenarios ?: emptyMap()
    }
}

expect fun DefaultScenariosDirectoryProcessor.scenarioFileType(): String