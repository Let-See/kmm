package nl.codeface.letsee_kmm.android.interceptor

import io.github.letsee.interfaces.Response
import io.github.letsee.models.DefaultRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Converts an OkHttp [okhttp3.Request] to a KMM [DefaultRequest].
 *
 * Header multi-values are joined with ", " per RFC 7230 §3.2.2.
 * The `path` field is the URL path without query parameters.
 */
fun okhttp3.Request.toLetSeeRequest(): DefaultRequest {
    val flatHeaders: Map<String, String> = headers.toMultimap()
        .mapValues { (_, values) -> values.joinToString(", ") }

    val path = url.encodedPath

    return DefaultRequest(
        headers = flatHeaders,
        requestMethod = method,
        uri = url.toString(),
        path = path
    )
}

/**
 * Converts a KMM [Response] to an [okhttp3.Response].
 *
 * - Null [Response.byteResponse] produces an empty body.
 * - Null [Response.statusText] defaults to "OK" for 2xx codes, "Error" otherwise.
 * - [Response.headers] entries with multiple values are added individually.
 */
fun Response.toOkHttpResponse(originalRequest: okhttp3.Request): okhttp3.Response {
    val code = responseCode.toInt()

    val message = statusText
        ?: if (code in 200..299) "OK" else "Error"

    val mediaType = headers["Content-Type"]
        ?.firstOrNull()
        ?.toMediaTypeOrNull()

    val body: ResponseBody = byteResponse
        ?.toResponseBody(mediaType)
        ?: ByteArray(0).toResponseBody(mediaType)

    val okHttpHeaders = okhttp3.Headers.Builder().apply {
        for ((name, values) in headers) {
            for (value in values) {
                add(name, value)
            }
        }
    }.build()

    return okhttp3.Response.Builder()
        .request(originalRequest)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(message)
        .headers(okHttpHeaders)
        .body(body)
        .build()
}
