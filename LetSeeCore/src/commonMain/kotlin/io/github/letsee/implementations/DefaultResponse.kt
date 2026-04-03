package io.github.letsee.implementations

import io.github.letsee.interfaces.Response

data class DefaultResponse(
    override val responseCode: UInt,
    override val requestCode: UInt,
    override val byteResponse: ByteArray?,
    override val errorMessage: String?,
    override val statusText: String?,
    override var headers: Map<String, List<String>>
) : Response {
    companion object {
        val CANCEL = DefaultResponse(400u, 400u,null,null,null, emptyMap())
    }
}

