package nl.codeface.letsee_kmm.android.interceptor

import io.github.letsee.interfaces.LetSee
import io.github.letsee.interfaces.Response
import io.github.letsee.interfaces.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * OkHttp [Interceptor] that delegates request interception to the KMM [LetSee] engine.
 *
 * Add as an **application interceptor** via [OkHttpClient.Builder.addInterceptor] (or the
 * [OkHttpClient.Builder.addLetSee] convenience extension) so it runs before caching/redirects.
 *
 * Threading note: OkHttp interceptors are synchronous but [LetSee.addRequest] is async —
 * the user selects a mock via the KMM UI. [runBlocking] bridges the two models by blocking
 * the OkHttp call thread until the [Result] listener fires. This is safe because:
 *  - OkHttp uses its own thread pool (separate from [kotlinx.coroutines.Dispatchers.Default]).
 *  - The KMM scope uses `Dispatchers.Default.limitedParallelism(1)` — an independent pool.
 *  - No shared threads → no deadlock.
 */
class LetSeeInterceptor(
    private val letSee: LetSee
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()

        if (!letSee.config.value.isMockEnabled) {
            return chain.proceed(request)
        }

        val kmmRequest = request.toLetSeeRequest()

        // Each intercepted call gets its own deferred — concurrent requests block independently.
        val deferred = CompletableDeferred<Response>()

        val listener = object : Result {
            override fun success(response: Response) {
                deferred.complete(response)
            }

            override fun failure(error: Response) {
                // Covers both mock-failure responses and the CANCEL sentinel (responseCode 400).
                deferred.complete(error)
            }
        }

        letSee.addRequest(kmmRequest, listener)

        // Block the OkHttp call thread until the user resolves the request.
        val kmmResponse = runBlocking { deferred.await() }

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
