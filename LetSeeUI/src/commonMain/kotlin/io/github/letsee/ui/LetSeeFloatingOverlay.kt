package io.github.letsee.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.letsee.interfaces.LetSee
import io.github.letsee.ui.components.LetSeeFloatingButton

/**
 * A full-size transparent overlay that renders **only** the floating LetSee debug button and
 * its quick-access mock panel.  It does **not** wrap or clip application content — the caller
 * is responsible for layering this composable above the rest of the UI (e.g. inside a
 * transparent full-screen [UIViewController][platform.UIKit.UIViewController] on iOS, or
 * wrapping app content in a [Box] on Android via [LetSeeOverlay]).
 *
 * ### Quick-access behaviour
 * When there are pending intercepted requests **and** no scenario is active the floating button
 * expands inline to show scrollable mock pill buttons for the latest request's SPECIFIC-category
 * mocks.  Tapping a pill resolves the request immediately without opening the full panel.
 *
 * ### iOS touch passthrough
 * Use [onInteractiveBoundsChanged] to keep the host [UIWindow][platform.UIKit.UIWindow]
 * `point(inside:with:)` in sync with the actual interactive area so that touches outside
 * the button/card are passed through to the app below.  Values are reported in logical units
 * (dp on Compose ≡ UIKit points) relative to the composition root.
 *
 * @param letSee                    the core SDK instance
 * @param onPanelRequested          called when the user taps the main FAB to open the full
 *                                  debug panel
 * @param onInteractiveBoundsChanged  optional platform callback:
 *                                  `(x, y, width, height)` in logical units after each layout
 * @param modifier                  applied to the root [Box]
 */
@Composable
fun LetSeeFloatingOverlay(
    letSee: LetSee,
    onPanelRequested: () -> Unit,
    onInteractiveBoundsChanged: ((x: Float, y: Float, width: Float, height: Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val components = remember { LetSeeUIFactory.create(letSee, scope) }

    val requests by components.requestListStateHolder.requests.collectAsState()
    val activeScenario by components.scenarioListStateHolder.activeScenario.collectAsState()

    // Show quick access only when there are pending requests and no scenario is running.
    val quickAccessRequest = if (activeScenario == null) requests.firstOrNull() else null

    var buttonVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { buttonVisible = true }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        AnimatedVisibility(
            visible = buttonVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            LetSeeFloatingButton(
                pendingCount = requests.size,
                quickAccessRequest = quickAccessRequest,
                onClick = onPanelRequested,
                onMockSelected = { request, mock ->
                    components.requestListStateHolder.selectMock(request, mock)
                },
                onInteractiveBoundsChanged = onInteractiveBoundsChanged,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
