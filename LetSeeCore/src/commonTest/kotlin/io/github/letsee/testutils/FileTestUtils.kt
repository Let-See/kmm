package io.github.letsee.testutils

/** Reads the entire text content of a file at [path], or returns null if the file does not exist. */
expect fun readFileForTest(path: String): String?
