package io.github.letsee

import io.github.letsee.MockImplementations.BaseUnitTest
import io.github.letsee.MockImplementations.MockDirectoryFilesFetcher
import io.github.letsee.MockImplementations.MockFileNameCleaner
import io.github.letsee.MockImplementations.MockFileNameProcessor
import io.github.letsee.MockImplementations.MockScenarioFileInformationProcessor
import io.github.letsee.implementations.DefaultGlobalMockDirectoryConfiguration
import io.github.letsee.implementations.DefaultScenariosDirectoryProcessor
import io.github.letsee.models.Mock
import io.github.letsee.models.ScenarioFileInformation
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
    private val mockNameProcessor = MockFileNameProcessor(fileNameCleaner)
    private val directoryFileFetcher = MockDirectoryFilesFetcher()
    private val scenariosDirectoryProcessor = MockScenarioFileInformationProcessor()
    private var requestToMockMapper: (String)->List<Mock> = {
        emptyList()
    }
    @BeforeTest
    fun setUp() {
        sut = DefaultScenariosDirectoryProcessor(directoryFileFetcher, mockNameProcessor, scenariosDirectoryProcessor,
            { defaultGlobalMockDirectoryConfiguration }) { requestToMockMapper(it) }
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
        // map the requested path to the mock objects
        this.requestToMockMapper = {
            listOf(
                Mock.SUCCESS("Success_payment", M.Objects.SUCCESS_RESPONSE, M.Objects.SUCCESS_MOCK_INFORMATION),
                Mock.FAILURE("Failure_payment", M.Objects.FAILURE_RESPONSE, M.Objects.FAILURE_MOCK_INFORMATION))
        }
        val filePath = M.Strings.PATH
        // when processor asks for the files in the given folder, it'll receive these
        directoryFileFetcher.result = mapOf(Pair(filePath, listOf(M.Strings.IOS_SCENARIO_SUCCESS, M.Strings.IOS_SCENARIO_FAILURE)))

        // when processor asks for cleaned file name in the scenario file to match that with the received mocks
        mockNameProcessor.result = listOf(
            M.Objects.SUCCESS_MOCK_INFORMATION, M.Objects.FAILURE_MOCK_INFORMATION
        )

        // when processor passes the retrieved files in the directory to map them to type safe objects will receive these
        val scenarioNames = listOf("SuccessPayment", "FailurePayment")
        scenariosDirectoryProcessor.result = listOf(ScenarioFileInformation(scenarioNames[0], listOf(
            ScenarioFileInformation.Step("arrangements", M.Objects.SUCCESS_MOCK_INFORMATION.displayName),
            ScenarioFileInformation.Step("arrangements", M.Objects.FAILURE_MOCK_INFORMATION.displayName))),
            ScenarioFileInformation(scenarioNames[1], listOf(
                ScenarioFileInformation.Step("arrangements", M.Objects.FAILURE_MOCK_INFORMATION.displayName),
                ScenarioFileInformation.Step("arrangements", M.Objects.FAILURE_MOCK_INFORMATION.displayName)))
            )

        val result = sut.process(filePath)
        assertNotNull(result[result.keys.first()])
        val scenarios = result[result.keys.first()]!!
        assertEquals(2, scenarios.size)
        val expectedList: List<Mock> = requestToMockMapper("")
        assertEquals(expectedList, scenarios[0].mocks)
        assertEquals(listOf(expectedList[1], expectedList[1]), scenarios[1].mocks)
        assertEquals(scenarioNames, scenarios.map { it.name })
    }
}


