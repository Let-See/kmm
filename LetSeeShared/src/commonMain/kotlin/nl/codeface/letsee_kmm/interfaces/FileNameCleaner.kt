package nl.codeface.letsee_kmm.interfaces

interface FileNameCleaner {
    /// Removes all the extra information and returns only the file name
    /// /some-directory/some-x/someMockFile.json -> someMockFile
    fun clean(filePath: String): String
}

