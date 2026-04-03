package io.github.letsee.ui.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

private var appContext: Context? = null

fun initPlatformServices(context: Context) {
    appContext = context.applicationContext
}

actual fun copyToClipboard(text: String) {
    val context = appContext ?: return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("LetSee", text)
    clipboard.setPrimaryClip(clip)
}
