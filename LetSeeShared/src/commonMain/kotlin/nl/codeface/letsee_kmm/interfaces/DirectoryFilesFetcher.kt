package nl.codeface.letsee_kmm.interfaces

interface DirectoryFilesFetcher {
    /// travers directory and it's children and returns all files
    fun getFiles(path: String, fileType: String): Map<String, List<String>>
}