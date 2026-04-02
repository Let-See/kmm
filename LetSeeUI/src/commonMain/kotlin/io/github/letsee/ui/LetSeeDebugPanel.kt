package io.github.letsee.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import io.github.letsee.interfaces.LetSee
import io.github.letsee.ui.components.JsonViewer
import io.github.letsee.ui.navigation.DebugScreen
import io.github.letsee.ui.screens.MockPickerScreen
import io.github.letsee.ui.screens.RequestListScreen
import io.github.letsee.ui.screens.ScenariosScreen
import io.github.letsee.ui.screens.SettingsScreen

private val tabScreens = listOf(
    DebugScreen.RequestList,
    DebugScreen.Scenarios,
    DebugScreen.Settings,
)

private val tabLabels = listOf("Requests", "Scenarios", "Settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetSeeDebugPanel(letSee: LetSee, onClose: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val components = remember { LetSeeUIFactory.create(letSee, scope) }

    var selectedTab by remember { mutableStateOf(0) }
    val backStack = remember { mutableStateListOf<DebugScreen>() }
    var currentScreen by remember { mutableStateOf<DebugScreen>(DebugScreen.RequestList) }

    fun navigateTo(screen: DebugScreen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    fun navigateBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeLast()
            true
        } else {
            false
        }
    }

    val isOnTabRoot = currentScreen is DebugScreen.RequestList ||
        currentScreen is DebugScreen.Scenarios ||
        currentScreen is DebugScreen.Settings

    LetSeeTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "LetSee Debug",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.semantics {
                                contentDescription = "Close debug panel"
                            },
                        ) {
                            Text(
                                text = "X",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isOnTabRoot) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        tabLabels.forEachIndexed { index, label ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    backStack.clear()
                                    currentScreen = tabScreens[index]
                                },
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selectedTab == index) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    )
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "$label tab"
                                },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (val screen = currentScreen) {
                        is DebugScreen.RequestList -> {
                            RequestListScreen(
                                requestListStateHolder = components.requestListStateHolder,
                                onRequestSelected = { request ->
                                    navigateTo(DebugScreen.MockPicker(request))
                                },
                            )
                        }

                        is DebugScreen.Scenarios -> {
                            ScenariosScreen(
                                scenarioListStateHolder = components.scenarioListStateHolder,
                            )
                        }

                        is DebugScreen.Settings -> {
                            SettingsScreen(
                                settingsStateHolder = components.settingsStateHolder,
                            )
                        }

                        is DebugScreen.MockPicker -> {
                            MockPickerScreen(
                                requestUIModel = screen.requestUIModel,
                                requestListStateHolder = components.requestListStateHolder,
                                onMockSelected = { navigateBack() },
                                onViewJson = { mock ->
                                    mock.formatted?.let { json ->
                                        navigateTo(DebugScreen.JsonDetail(json))
                                    }
                                },
                                onBack = { navigateBack() },
                            )
                        }

                        is DebugScreen.JsonDetail -> {
                            JsonDetailContent(
                                json = screen.json,
                                onBack = { navigateBack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonDetailContent(
    json: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "JSON Detail",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back from JSON detail"
                        },
                    ) {
                        Text(
                            text = "\u2190 Back",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            JsonViewer(json = json)
        }
    }
}
