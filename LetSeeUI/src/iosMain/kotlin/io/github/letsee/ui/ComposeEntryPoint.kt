package io.github.letsee.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.github.letsee.interfaces.LetSee
import platform.UIKit.UIViewController

/**
 * Creates a [UIViewController] that hosts the full LetSee debug panel (Scaffold + tabs).
 *
 * Present this modally (e.g. as a page sheet) when the user taps the floating button.
 */
fun LetSeeDebugViewController(letSee: LetSee): UIViewController {
    var vc: UIViewController? = null
    vc = ComposeUIViewController {
        LetSeeDebugPanel(
            letSee = letSee,
            onClose = { vc?.dismissViewControllerAnimated(true, completion = null) },
        )
    }
    return vc
}

/**
 * Creates a [UIViewController] that hosts **only** the floating debug button and its
 * quick-access mock panel — no application content is wrapped.
 *
 * The caller is responsible for:
 * - Setting the view's `backgroundColor` to `.clear` and `isOpaque` to `false` so the
 *   transparent Compose layer does not obscure the app behind it.
 * - Embedding the view so that it fills the entire window / screen bounds.
 * - Using [onInteractiveBoundsChanged] to receive the current interactive area of the button
 *   (in UIKit points, relative to the view's coordinate space) and forwarding only those
 *   touches to this view controller via a custom `point(inside:with:)` override on the
 *   host [UIWindow][platform.UIKit.UIWindow].
 *
 * @param letSee                     the core SDK instance
 * @param onOpenPanel                called when the user taps the main FAB circle — the
 *                                   caller should present [LetSeeDebugViewController] here
 * @param onInteractiveBoundsChanged optional callback invoked after each Compose layout pass
 *                                   with `(x, y, width, height)` of the entire interactive
 *                                   area in UIKit-point units
 */
fun LetSeeFloatingOverlayViewController(
    letSee: LetSee,
    onOpenPanel: () -> Unit,
    onInteractiveBoundsChanged: ((x: Double, y: Double, width: Double, height: Double) -> Unit)? = null,
): UIViewController {
    return ComposeUIViewController {
        LetSeeTheme {
            LetSeeFloatingOverlay(
                letSee = letSee,
                onPanelRequested = onOpenPanel,
                onInteractiveBoundsChanged = onInteractiveBoundsChanged?.let { callback ->
                    { x, y, w, h ->
                        callback(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
                    }
                },
            )
        }
    }
}
