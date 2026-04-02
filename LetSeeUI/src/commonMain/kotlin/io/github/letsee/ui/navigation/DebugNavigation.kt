package io.github.letsee.ui.navigation

import io.github.letsee.ui.RequestUIModel

sealed class DebugScreen {
    data object Settings : DebugScreen()
    data object RequestList : DebugScreen()
    data object Scenarios : DebugScreen()
    data class MockPicker(val requestUIModel: RequestUIModel) : DebugScreen()
    data class JsonDetail(val json: String) : DebugScreen()
}
