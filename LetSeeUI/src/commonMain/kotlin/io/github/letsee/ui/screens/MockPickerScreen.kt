package io.github.letsee.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.letsee.models.Category
import io.github.letsee.models.Mock
import io.github.letsee.models.name
import io.github.letsee.ui.RequestListStateHolder
import io.github.letsee.ui.RequestUIModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockPickerScreen(
    requestUIModel: RequestUIModel,
    requestListStateHolder: RequestListStateHolder,
    onMockSelected: () -> Unit,
    onViewJson: (Mock) -> Unit,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = requestUIModel.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("mock_picker_back")
                            .semantics { contentDescription = "Navigate back" },
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
        val sectionOrder = listOf(Category.SPECIFIC, Category.GENERAL, Category.SUGGESTED)
        val sectionMap = requestUIModel.categorisedMocks.associateBy { it.category }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("letsee_mock_picker"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        ) {
            for (category in sectionOrder) {
                val categorised = sectionMap[category] ?: continue
                val sortedMocks = categorised.mocks.sorted()
                if (sortedMocks.isEmpty()) continue

                item(key = "header_${category.name}") {
                    SectionHeader(label = category.name())
                }

                items(
                    items = sortedMocks,
                    key = { "${category.name}_${it.name}" },
                ) { mock ->
                    MockRow(
                        mock = mock,
                        onTap = {
                            when (mock) {
                                is Mock.LIVE -> requestListStateHolder.respondLive(requestUIModel.request)
                                else -> requestListStateHolder.selectMock(requestUIModel.request, mock)
                            }
                            onMockSelected()
                        },
                        onViewJson = { onViewJson(mock) },
                    )
                }

                item(key = "spacer_${category.name}") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MockRow(
    mock: Mock,
    onTap: () -> Unit,
    onViewJson: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .testTag("mock_row_${mock.name}")
            .semantics {
                val type = when (mock) {
                    is Mock.SUCCESS -> "success"
                    is Mock.FAILURE -> "failure"
                    is Mock.ERROR -> "error"
                    is Mock.LIVE -> "live"
                    is Mock.CANCEL -> "cancel"
                }
                contentDescription = "Mock: ${mock.displayName}, category: $type"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MockTypeIndicator(mock = mock)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mock.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (mock is Mock.SUCCESS || mock is Mock.FAILURE) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onViewJson,
                    modifier = Modifier
                        .testTag("view_json_${mock.name}")
                        .semantics { contentDescription = "View JSON for ${mock.displayName}" },
                ) {
                    Text(
                        text = "[JSON]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MockTypeIndicator(mock: Mock) {
    val (label, color) = when (mock) {
        is Mock.SUCCESS -> "OK" to MaterialTheme.colorScheme.primary
        is Mock.FAILURE -> "FAIL" to MaterialTheme.colorScheme.error
        is Mock.ERROR -> "ERR" to MaterialTheme.colorScheme.error
        is Mock.LIVE -> "LIVE" to MaterialTheme.colorScheme.tertiary
        is Mock.CANCEL -> "X" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
