package io.github.letsee.ui

import io.github.letsee.interfaces.LetSee
import io.github.letsee.models.Category
import io.github.letsee.models.Mock
import io.github.letsee.models.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Visual style category for a mock pill — avoids Swift having to inspect Kotlin sealed-class
 * subtypes or rely on SKIE enum transformation details.
 */
enum class MockStyleCategory {
    SUCCESS, FAILURE, LIVE, CANCEL;
}

/**
 * Lightweight per-mock holder for the quick-access panel.
 *
 * [styleCategory] tells the Swift UI which colour to apply without any type inspection.
 */
data class QuickAccessMock(
    val mock: Mock,
    val displayName: String,
    val styleCategory: MockStyleCategory,
)

/**
 * Lightweight data holder passed from Kotlin to Swift for the quick-access panel.
 *
 * Created synchronously from [getQuickAccessData] so that Swift polling code can call it
 * from a `Timer` without needing coroutines.
 */
data class QuickAccessData(
    /** Short display name / path of the intercepted request (e.g. "/api/breeds"). */
    val requestPath: String,
    /** SPECIFIC-category mocks, wrapped with display/style metadata, sorted. */
    val mocks: List<QuickAccessMock>,
    /** The original request — passed back to [respondToRequestWithMock] on selection. */
    val request: Request,
)

private fun Mock.toQuickAccessMock() = QuickAccessMock(
    mock = this,
    displayName = displayName,
    styleCategory = when (this) {
        is Mock.SUCCESS -> MockStyleCategory.SUCCESS
        is Mock.FAILURE -> MockStyleCategory.FAILURE
        is Mock.ERROR -> MockStyleCategory.FAILURE
        is Mock.LIVE -> MockStyleCategory.LIVE
        is Mock.CANCEL -> MockStyleCategory.CANCEL
    },
)

/**
 * Returns a [QuickAccessData] for the latest pending request if:
 * - no scenario is currently active, AND
 * - the request has at least one SPECIFIC-category mock.
 *
 * Returns `null` otherwise.  Safe to call on any thread; reads the [SharedFlow.replayCache]
 * synchronously without suspending.
 */
fun getQuickAccessData(letSee: LetSee): QuickAccessData? {
    if (letSee.requestsManager.scenarioManager.isScenarioActive) return null

    val latestAccepted = letSee.requestsManager.requestsStack
        .replayCache
        .firstOrNull()
        ?.firstOrNull()
        ?: return null

    val specificMocks = latestAccepted.mocks
        ?.firstOrNull { it.category == Category.SPECIFIC }
        ?.mocks
        ?.sorted()
        ?: return null

    if (specificMocks.isEmpty()) return null

    return QuickAccessData(
        requestPath = latestAccepted.request.path,
        mocks = specificMocks.map { it.toQuickAccessMock() },
        request = latestAccepted.request,
    )
}

/**
 * Responds to [request] with the chosen [mock].
 *
 * Dispatches onto the Kotlin main-thread dispatcher; safe to call from any Swift thread
 * (e.g. a `UIButton` action on the main thread).  The coroutine scope is intentionally
 * short-lived — it is cancelled automatically once the single `respond` call completes.
 */
fun respondToRequestWithMock(letSee: LetSee, request: Request, mock: Mock) {
    CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
        letSee.requestsManager.respond(request, withMockResponse = mock)
    }
}
