package io.github.letsee.implementations

/** Returns the current wall-clock time in milliseconds since the Unix epoch. */
expect fun currentTimeMillis(): Long

/** Returns a human-readable timestamp string, e.g. `2026-04-01 12:34:56.789`. */
expect fun currentTimestamp(): String
