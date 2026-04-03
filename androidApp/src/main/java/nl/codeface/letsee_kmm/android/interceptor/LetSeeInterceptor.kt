package nl.codeface.letsee_kmm.android.interceptor

import io.github.letsee.interfaces.LetSee
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp [Interceptor] that delegates request interception to the KMM [LetSee] engine.
 *
 * Add as an **application interceptor** via [OkHttpClient.Builder.addInterceptor] (or the
 * [OkHttpClient.Builder.addLetSee] convenience extension) so it runs before caching/redirects.
 *
 * @param letSee the KMM LetSee instance to delegate to
 * @param mockSelectionTimeoutMs maximum time (milliseconds) to wait for mock selection before
 *        throwing [IOException]. Defaults to 5 minutes. Set to [Long.MAX_VALUE] to wait indefinitely.
 *
 * Threading note: OkHttp interceptors are synchronous but [LetSee.addRequest] is async —
 * the user selects a mock via the KMM UI. [runBlocking] bridges the two models by blocking
 * the OkHttp call thread until the [Result] listener fires. This is safe because:
 *  - OkHttp uses its own thread pool (separate from [kotlinx.coroutines.Dispatchers.Default]).
 *  - The KMM scope uses `Dispatchers.Default.limitedParallelism(1)` — an independent pool.
 *  - No shared threads → no deadlock.
 */
class LetSeeInterceptor(
    private val letSee: LetSee,
    private val mockSelectionTimeoutMs: Long = TimeUnit.MINUTES.toMillis(5)
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()

        if (!letSee.config.value.isMockEnabled) {
            return chain.proceed(request)
        }

        val kmmRequest = request.toLetSeeRequest()

        val deferred = CompletableDeferred<Response>()

        val listener = object : Result {
            override fun success(response: Response) {
                deferred.complete(response)
            }

            override fun failure(error: Response) {
                deferred.complete(error)
            }
        }

        letSee.addRequest(kmmRequest, listener)

        val kmmResponse = runBlocking {
            try {
                withTimeout(mockSelectionTimeoutMs) { deferred.await() }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                throw IOException(
                    "LetSee mock selection timed out after ${mockSelectionTimeoutMs}ms for ${request.url}"
                )
            }
        }

        return kmmResponse.toOkHttpResponse(request)
    }
}

/**
 * Convenience extension that wires [LetSeeInterceptor] as an application interceptor.
 *
 * Usage:
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addLetSee(DefaultLetSee.letSee)
 *     .build()
 * ```
 */
fun OkHttpClient.Builder.addLetSee(letSee: LetSee): OkHttpClient.Builder =
    addInterceptor(LetSeeInterceptor(letSee))
