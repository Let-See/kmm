package nl.codeface.letsee_kmm.interfaces

interface DirectoryProcessor<out T> {
    /// Root/child/child2
    /// Root/Users
    /// Root/Users/Scores
    fun process(path: String): Map<String, List<T>>
}