package nl.codeface.letsee_kmm.models

interface Request {
    val headers: Map<String, String>
    val requestMethod: String
    val uri: String
    var path: String
}