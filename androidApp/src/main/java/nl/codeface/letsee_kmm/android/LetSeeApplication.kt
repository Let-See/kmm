package nl.codeface.letsee_kmm.android

import android.app.Application
import io.github.letsee.DefaultLetSee
import io.github.letsee.implementations.setLetSeeAndroidContext
import nl.codeface.letsee_kmm.android.interceptor.configureLetSeeLiveHandler

/**
 * Application subclass that initialises the LetSee Android context.
 *
 * [setLetSeeAndroidContext] must be called before [io.github.letsee.DefaultLetSee.setMocks]
 * so that [io.github.letsee.implementations.GlobalMockDirectoryConfiguration.exists] can read
 * `.ls.global.json` from Android assets.
 *
 * Registered in AndroidManifest.xml via `android:name=".LetSeeApplication"`.
 */
class LetSeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setLetSeeAndroidContext(this)
        configureLetSeeLiveHandler(DefaultLetSee.letSee)
    }
}
