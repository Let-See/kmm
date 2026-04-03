package io.github.letsee.interfaces
/**
 * Directory Processor gets all files in the given folder and it's sub directories and map them to a better presentation and more readable, type safe objects
 * The interface implementer needs to specified the type of the information (T). Ultimately after processing, caller receives a map of the key which usually is the
 * Folder Path or name (based on the implementation) and a list of files in <T>
 */
interface DirectoryProcessor<out T> {
    /**
     * Root/child/child2
     * Root/Users
     * Root/Users/Scores
     */
    fun process(path: String): Map<String, List<T>>
}