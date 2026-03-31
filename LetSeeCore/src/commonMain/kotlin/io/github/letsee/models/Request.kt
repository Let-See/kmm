package io.github.letsee.models

import kotlin.random.Random

interface Request {
    val headers: Map<String, String>
    val requestMethod: String
    val uri: String
    var path: String
    val id: Int
}

data class DefaultRequest(
    override val headers: Map<String, String>,
    override val requestMethod: String,
    override val uri: String,
    override var path: String
) : Request {
    override val id = Random.nextInt()
}