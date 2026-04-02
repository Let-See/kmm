@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.implementations

import io.github.letsee.interfaces.LetSee
import io.github.letsee.interfaces.Response
import io.github.letsee.models.Request
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges the Kotlin [suspend] [LetSee.liveRequestHandler] property to a Swift-friendly
 * completion-handler API.
 *
 * Kotlin/Native exports suspend lambdas with an extra `completionHandler` parameter, but
 * *assigning* a suspend lambda property from Swift is not straightforward without SKIE.
 * This function accepts a callback-style [handler] that Swift can provide natively and
 * wraps it into the required `suspend (Request) -> Response` form.
 *
 * Swift usage:
 * ```swift
 * LiveHandlerBridgeKt.setLiveRequestHandler(letSee: DefaultLetSee.Companion.shared.letSee) {
 *     request, completion in
 *     // execute native HTTP call, then:
 *     let response = LiveHandlerBridgeKt.createLiveResponse(...)
 *     completion(response, nil)
 * }
 * ```
 */
fun setLiveRequestHandler(
    letSee: LetSee,
    handler: (Request, (Response?, NSError?) -> Unit) -> Unit
) {
    letSee.liveRequestHandler = { request ->
        suspendCancellableCoroutine { cont ->
            handler(request) { response, error ->
                if (response != null) {
                    cont.resume(response)
                } else {
                    cont.resumeWithException(
                        Exception(error?.localizedDescription ?: "Live request failed with unknown error")
                    )
                }
            }
        }
    }
}

/**
 * Factory for creating a [DefaultResponse] from Swift-native types, avoiding the need
 * for Swift code to construct `KotlinByteArray` manually.
 *
 * @param statusCode HTTP status code (e.g. 200)
 * @param headers response headers as `[String: [String]]`
 * @param bodyData response body as `NSData?` — converted to `ByteArray` internally
 * @param statusText HTTP reason phrase (e.g. "OK")
 */
fun createLiveResponse(
    statusCode: Int,
    headers: Map<String, List<String>>,
    bodyData: NSData?,
    statusText: String?
): Response = DefaultResponse(
    responseCode = statusCode.toUInt(),
    requestCode = 0u,
    byteResponse = bodyData?.toByteArray(),
    errorMessage = null,
    statusText = statusText,
    headers = headers
)

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}
