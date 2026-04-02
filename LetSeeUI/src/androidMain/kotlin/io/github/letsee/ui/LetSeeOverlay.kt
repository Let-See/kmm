package io.github.letsee.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetSeeOverlay(
    letSee: LetSee,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val components = remember { LetSeeUIFactory.create(letSee, scope) }
    val requests by components.requestListStateHolder.requests.collectAsState()

    var showPanel by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = modifier.fillMaxSize()) {
        content()

        LetSeeFloatingButton(
            pendingCount = requests.size,
            onClick = { showPanel = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )

        if (showPanel) {
            ModalBottomSheet(
                onDismissRequest = { showPanel = false },
                sheetState = sheetState,
            ) {
                LetSeeTheme {
                    LetSeeDebugPanel(
                        letSee = letSee,
                        onClose = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showPanel = false
                            }
                        },
                    )
                }
            }
        }
    }
}
