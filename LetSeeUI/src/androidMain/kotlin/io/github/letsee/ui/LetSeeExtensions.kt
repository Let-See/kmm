package io.github.letsee.ui

import android.app.Activity
import io.github.letsee.ui.platform.initPlatformServices

fun Activity.initLetSee() {
    initPlatformServices(this)
}
