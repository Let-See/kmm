package io.github.letsee.ui

import androidx.compose.ui.window.ComposeUIViewController
import io.github.letsee.interfaces.LetSee
import platform.UIKit.UIViewController

fun LetSeeDebugViewController(letSee: LetSee): UIViewController =
    ComposeUIViewController { LetSeeDebugPanel(letSee) }
