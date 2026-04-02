package nl.codeface.letsee_kmm.android.interceptor

import io.github.letsee.implementations.DefaultResponse
import io.github.letsee.interfaces.LetSee
import io.github.letsee.interfaces.Response
import io.github.letsee.models.Request
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Configures a live request handler on the given [LetSee] instance.
 *
 * The handler uses a **plain** [OkHttpClient] without [LetSeeInterceptor], so requests
 * bypass LetSee entirely — preventing infinite interception loops.
 *
 * Only bodyless methods (GET, HEAD, DELETE) are faithfully forwarded. POST/PUT/PATCH
 * requests are forwarded without a body because the KMM [Request] model has no `body`
 * field. Adding request body support requires a future `body: ByteArray?` field on
 * [Request].
 */
fun configureLetSeeLiveHandler(letSee: LetSee) {
    val plainClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    letSee.liveRequestHandler = { request ->
        executeLiveRequest(plainClient, request)
    }
}

private suspend fun executeLiveRequest(
    client: OkHttpClient,
    request: Request
): Response {
    val bodylessMethods = setOf("GET", "HEAD", "DELETE")
    val method = request.requestMethod.uppercase()
    val body = if (method in bodylessMethods) null else ByteArray(0).toRequestBody()

    val okRequest = okhttp3.Request.Builder()
        .url(request.uri)
        .method(method, body)
        .apply {
            for ((key, value) in request.headers) {
                if (!key.equals("LETSEE-LOGGER-ID", ignoreCase = true)) {
                    addHeader(key, value)
                }
            }
        }
        .build()

    val okResponse = client.newCall(okRequest).await()
    return okResponse.use { resp ->
        DefaultResponse(
            responseCode = resp.code.toUInt(),
            requestCode = 0u,
            byteResponse = resp.body?.bytes(),
            errorMessage = if (!resp.isSuccessful) resp.message else null,
            statusText = resp.message,
            headers = resp.headers.toMultimap()
        )
    }
}

private suspend fun Call.await(): okhttp3.Response =
    suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                cont.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }
        })
        cont.invokeOnCancellation { cancel() }
    }
