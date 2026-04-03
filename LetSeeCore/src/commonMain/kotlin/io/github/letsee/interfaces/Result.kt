package io.github.letsee.interfaces

interface Result {
    fun success(response: Response)
    fun failure(error: Response)
}