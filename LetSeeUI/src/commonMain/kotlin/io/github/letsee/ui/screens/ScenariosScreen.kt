package io.github.letsee.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.letsee.models.Scenario
import io.github.letsee.ui.ScenarioListStateHolder

@Composable
fun ScenariosScreen(
    scenarioListStateHolder: ScenarioListStateHolder,
    modifier: Modifier = Modifier,
) {
    val scenarios by scenarioListStateHolder.scenarios.collectAsState()
    val activeScenario by scenarioListStateHolder.activeScenario.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (scenarios.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No scenarios loaded",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(scenarios, key = { it.name }) { scenario ->
                    val active = activeScenario
                    val isActive = active != null && active.name == scenario.name
                    val stepInfo = if (isActive) {
                        val step = minOf(active!!.currentIndex + 1, active.mocks.size)
                        "Step $step of ${active.mocks.size}"
                    } else {
                        null
                    }

                    ScenarioRow(
                        scenario = scenario,
                        isActive = isActive,
                        stepInfo = stepInfo,
                        onClick = {
                            if (isActive) {
                                scenarioListStateHolder.deactivateScenario()
                            } else {
                                scenarioListStateHolder.activateScenario(scenario)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScenarioRow(
    scenario: Scenario,
    isActive: Boolean,
    stepInfo: String?,
    onClick: () -> Unit,
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = scenario.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = "${scenario.mocks.size} steps",
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 2.dp),
            )
            if (stepInfo != null) {
                Text(
                    text = stepInfo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
