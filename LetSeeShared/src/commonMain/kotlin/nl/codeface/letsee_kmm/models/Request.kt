package nl.codeface.letsee_kmm.models

interface Request {
    val headers: Map<String, String>
    val requestMethod: String
    val uri: String
    var path: String
}

data class DefaultRequest(
    override val headers: Map<String, String>,
    override val requestMethod: String,
    override val uri: String,
    override var path: String
) : Request