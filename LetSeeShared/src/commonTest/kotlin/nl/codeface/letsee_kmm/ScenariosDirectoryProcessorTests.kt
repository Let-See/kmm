package nl.codeface.letsee_kmm

import nl.codeface.letsee_kmm.MockImplementations.BaseUnitTest
import nl.codeface.letsee_kmm.MockImplementations.MockDirectoryFilesFetcher
import nl.codeface.letsee_kmm.MockImplementations.MockFileNameCleaner
import nl.codeface.letsee_kmm.MockImplementations.MockFileNameProcessor
import nl.codeface.letsee_kmm.MockImplementations.MockMockProcessor
import nl.codeface.letsee_kmm.MockImplementations.MockScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.implementations.DefaultGlobalMockDirectoryConfiguration
import nl.codeface.letsee_kmm.implementations.DefaultResponse
import nl.codeface.letsee_kmm.implementations.DefaultScenariosDirectoryProcessor
import nl.codeface.letsee_kmm.implementations.GlobalMockDirectoryConfiguration
import nl.codeface.letsee_kmm.implementations.MocksDirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.DirectoryProcessor
import nl.codeface.letsee_kmm.interfaces.Response
import nl.codeface.letsee_kmm.interfaces.ScenarioFileInformationProcessor
import nl.codeface.letsee_kmm.models.ScenarioFileInformation
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ScenariosDirectoryProcessorTests: BaseUnitTest() {
    private val defaultGlobalMockDirectoryConfiguration = DefaultGlobalMockDirectoryConfiguration(listOf(
        DefaultGlobalMockDirectoryConfiguration.Map("/inside","https://google.com/api/v2"),
        DefaultGlobalMockDirectoryConfiguration.Map("/someOther","https://apple.com/api/v2")))
    private lateinit var sut: DefaultScenariosDirectoryProcessor
    private val fileNameCleaner = MockFileNameCleaner()
    private val fileNameProcessor = MockFileNameProcessor(fileNameCleaner)
    private val directoryFileFetcher = MockDirectoryFilesFetcher()
    private val scenariosDirectoryProcessor = MockScenarioFileInformationProcessor()
    private var requestToMockMapper: (String)->List<Mock> = {
        emptyList()
    }
    @BeforeTest
    fun setUp() {
        sut = DefaultScenariosDirectoryProcessor(directoryFileFetcher, fileNameProcessor, scenariosDirectoryProcessor, defaultGlobalMockDirectoryConfiguration) { requestToMockMapper(it) }
    }

    /*
        Scenarios
            Scenario1
            Scenario2
            Scenario3
            Scenario4
            Scenario5
     */
    @Test
    fun `test process function should read the scenarios steps and map each step to the correct folder and obtain its mock`() {

        this.requestToMockMapper = {
            listOf(Mock.SUCCESS("Success_payment", M.Objects.SUCCESS_RESPONSE, M.Objects.SUCCESS_MOCK_INFORMATION),
                Mock.FAILURE("Failure_payment", M.Objects.FAILURE_RESPONSE, M.Objects.FAILURE_MOCK_INFORMATION))
        }
        val filePath = M.Strings.PATH
        directoryFileFetcher.result = mapOf(Pair(filePath, listOf(M.Strings.IOS_SCENARIO_SUCCESS, M.Strings.IOS_SCENARIO_FAILURE)))

        fileNameProcessor.result = listOf(
            M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.FAILURE_MOCK_INFORMATION
        )

        scenariosDirectoryProcessor.result = listOf(ScenarioFileInformation("SuccessPayment", listOf(
            ScenarioFileInformation.Step("arrangements", M.Objects.SUCCESS_MOCK_INFORMATION.displayName),
            ScenarioFileInformation.Step("arrangements", M.Objects.FAILURE_MOCK_INFORMATION.displayName))),
            ScenarioFileInformation("FailurePayment", listOf(
                ScenarioFileInformation.Step("arrangements", M.Objects.SUCCESS_MOCK_INFORMATION.displayName),
                ScenarioFileInformation.Step("arrangements", M.Objects.FAILURE_MOCK_INFORMATION.displayName)))
            )

        val result = sut.process(filePath)
        val firstExpectedKey = defaultGlobalMockDirectoryConfiguration.maps.first().to + "/inside"
//        assertEquals(3, result.size)
//        assertEquals(firstExpectedKey, result.keys.first())
//        assertNotNull( result[firstExpectedKey])
//        assertEquals(3, result[firstExpectedKey]?.size)
//        assertEquals(defaultGlobalMockDirectoryConfiguration.maps.first().to + "/inside/inside2", result.keys.drop(1).first())
//        assertEquals(defaultGlobalMockDirectoryConfiguration.maps.drop(1).first().to + "/someother", result.keys.drop(2).first())
    }
}


