package io.github.letsee.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.letsee.models.Category
import io.github.letsee.models.Mock
import io.github.letsee.models.Request
import io.github.letsee.ui.RequestUIModel
import kotlin.math.roundToInt

private const val BUTTON_SIZE_DP = 56

/**
 * Floating LetSee debug button with an optional quick-access mock panel.
 *
 * When [quickAccessRequest] is non-null and its SPECIFIC-category mocks are non-empty the
 * container expands horizontally to show an animated card with scrollable mock pill buttons
 * alongside the main FAB.  Tapping a pill immediately calls [onMockSelected] without opening
 * the full debug panel.
 *
 * The entire widget is draggable.  After each layout pass [onInteractiveBoundsChanged] is
 * invoked with the current bounding box (in root-coordinate dp / UIKit-point units) so that
 * the iOS host window can narrow its touch-passthrough area to exactly this region.
 *
 * @param pendingCount             count of intercepted pending requests (drives the badge)
 * @param quickAccessRequest       the latest pending request whose mocks should be surfaced
 *                                 inline; null when no quick access should be shown
 * @param onClick                  called when the main FAB circle is tapped
 * @param onMockSelected           called when a mock pill is tapped
 * @param onInteractiveBoundsChanged  platform callback: (x, y, width, height) in logical units
 * @param initialOffsetX           initial horizontal drag offset in pixels
 * @param initialOffsetY           initial vertical drag offset in pixels
 */
@Composable
fun LetSeeFloatingButton(
    pendingCount: Int,
    quickAccessRequest: RequestUIModel? = null,
    onClick: () -> Unit,
    onMockSelected: (Request, Mock) -> Unit = { _, _ -> },
    onInteractiveBoundsChanged: ((x: Float, y: Float, width: Float, height: Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
) {
    var offsetX by remember { mutableStateOf(initialOffsetX) }
    var offsetY by remember { mutableStateOf(initialOffsetY) }
    val density = LocalDensity.current

    val specificMocks = remember(quickAccessRequest) {
        quickAccessRequest
            ?.categorisedMocks
            ?.firstOrNull { it.category == Category.SPECIFIC }
            ?.mocks
            ?.sorted()
            ?: emptyList()
    }

    val showQuickAccess = quickAccessRequest != null && specificMocks.isNotEmpty()

    val accessibilityLabel = if (pendingCount > 0) {
        "LetSee debug button, $pendingCount pending requests"
    } else {
        "LetSee debug button"
    }

    Row(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                val buttonSizePx = BUTTON_SIZE_DP.dp.toPx()
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val maxX = size.width - buttonSizePx
                    val maxY = size.height - buttonSizePx
                    offsetX = (offsetX + dragAmount.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + dragAmount.y).coerceIn(-maxY, maxY)
                }
            }
            .onGloballyPositioned { coords ->
                if (onInteractiveBoundsChanged != null) {
                    val pos = coords.positionInRoot()
                    val size = coords.size
                    // Convert from composition pixels to logical units (dp / UIKit points).
                    val scale = density.density
                    onInteractiveBoundsChanged(
                        pos.x / scale,
                        pos.y / scale,
                        size.width / scale,
                        size.height / scale,
                    )
                }
            }
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .testTag("letsee_floating_button"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Quick-access mock panel — slides in from the right when a pending request is available.
        AnimatedVisibility(
            visible = showQuickAccess,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            if (quickAccessRequest != null && specificMocks.isNotEmpty()) {
                QuickAccessCard(
                    request = quickAccessRequest,
                    mocks = specificMocks,
                    onMockSelected = onMockSelected,
                )
            }
        }

        // Main FAB with pending-count badge.
        Box {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier.size(BUTTON_SIZE_DP.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp,
                ),
            ) {
                Text(
                    text = "LS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (pendingCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (pendingCount > 99) "99+" else pendingCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal composables
// ---------------------------------------------------------------------------

@Composable
private fun QuickAccessCard(
    request: RequestUIModel,
    mocks: List<Mock>,
    onMockSelected: (Request, Mock) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .widthIn(max = 280.dp)
            .testTag("letsee_quick_access_card"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = request.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("letsee_quick_access_title"),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("letsee_quick_access_mocks"),
            ) {
                items(mocks, key = { it.name }) { mock ->
                    MockPill(
                        mock = mock,
                        onClick = { onMockSelected(request.request, mock) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MockPill(mock: Mock, onClick: () -> Unit) {
    val bgColor = when (mock) {
        is Mock.SUCCESS -> MaterialTheme.colorScheme.primary
        is Mock.FAILURE -> MaterialTheme.colorScheme.error
        is Mock.ERROR -> MaterialTheme.colorScheme.error
        is Mock.LIVE -> MaterialTheme.colorScheme.tertiary
        is Mock.CANCEL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (mock) {
        is Mock.SUCCESS -> MaterialTheme.colorScheme.onPrimary
        is Mock.FAILURE -> MaterialTheme.colorScheme.onError
        is Mock.ERROR -> MaterialTheme.colorScheme.onError
        is Mock.LIVE -> MaterialTheme.colorScheme.onTertiary
        is Mock.CANCEL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier
            .height(30.dp)
            .testTag("quick_access_mock_${mock.name}")
            .semantics { contentDescription = "Quick select ${mock.displayName}" },
    ) {
        Text(
            text = mock.displayName,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
        )
    }
}
