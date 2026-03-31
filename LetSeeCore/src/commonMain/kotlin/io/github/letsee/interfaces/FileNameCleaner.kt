package io.github.letsee.interfaces

/**
 * File Name Cleaner removes all the extra information from the file path and returns the cleaned file name. In LetSee file name
 * plays a very important role, We can specify some information like the delay, status code and ... in the file name. This Interface helps
 * to extract the cleaned file name
 */
interface FileNameCleaner {
    /**
     * Removes all the extra information and returns only the file name
     * /some-directory/some-x/someMockFile.json -> someMockFile
     */
    fun clean(filePath: String): String
}

