package nl.codeface.letsee_kmm
import nl.codeface.letsee_kmm.MockImplementations.MockFileDataFetcher
import nl.codeface.letsee_kmm.implementations.DefaultMockProcessor
import nl.codeface.letsee_kmm.interfaces.DefaultRequestsManager
import kotlin.test.BeforeTest
import kotlin.test.Test

class DefaultRequestsManagerTest {
    private lateinit var sut: DefaultRequestsManager
    @BeforeTest
    fun setup() {
        sut = DefaultRequestsManager()
    }

    @Test
    fun testAccept() {
    }

    @Test
    fun testRespond() {
    }

    @Test
    fun testTestRespond() {
    }

    @Test
    fun testTestRespond1() {
    }

    @Test
    fun testUpdate() {
    }

    @Test
    fun testCancel() {
    }

    @Test
    fun testFinish() {
    }

    @Test
    fun testGetRequestsStack() {
    }

    @Test
    fun testGetOnRequestAccepted() {
    }

    @Test
    fun testGetOnRequestRemoved() {
    }
}