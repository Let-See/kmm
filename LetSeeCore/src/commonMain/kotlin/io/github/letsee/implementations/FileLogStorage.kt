package io.github.letsee.implementations

import io.github.letsee.interfaces.LogStorage

/**
 * A [LogStorage] implementation that persists log entries to a text file.
 *
 * Thread safety: designed for use with [DefaultLetSee.scope] which uses
 * `Dispatchers.Default.limitedParallelism(1)`, ensuring all [append] and [clear] calls
 * are serialized. This makes explicit locking unnecessary for the primary use case.
 *
 * @param filePath absolute path to the log file; defaults to `{tempDir}/letsee_traffic.log`
 * @param clearOnInit when `true` (default), deletes any existing log file on construction
 *                    so each app launch starts with a clean log
 */
class FileLogStorage(
    override val filePath: String = defaultLogDirectory() + "/letsee_traffic.log",
    clearOnInit: Boolean = true
) : LogStorage {

    init {
        if (clearOnInit) clear()
    }

    override fun append(text: String) {
        appendToFile(filePath, text)
    }

    override fun clear() {
        deleteFile(filePath)
    }
}
