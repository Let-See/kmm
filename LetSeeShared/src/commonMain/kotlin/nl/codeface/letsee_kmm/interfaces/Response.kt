package nl.codeface.letsee_kmm.interfaces


interface Response {
    val responseCode: UInt
    val requestCode: UInt
    val byteResponse: ByteArray?
    val errorMessage: String?
    val statusText: String?
    var headers: Map<String, List<String>>
}

