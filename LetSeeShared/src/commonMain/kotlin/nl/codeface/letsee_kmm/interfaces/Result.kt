package nl.codeface.letsee_kmm.interfaces

interface Result {
    fun success(response: Response)
    fun failure(error: Response)
}