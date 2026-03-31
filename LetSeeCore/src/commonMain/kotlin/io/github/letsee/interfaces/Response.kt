package io.github.letsee.interfaces


interface Response {
    val responseCode: UInt
    val requestCode: UInt
    val byteResponse: ByteArray?
    val errorMessage: String?
    val statusText: String?
    var headers: Map<String, List<String>>
}

