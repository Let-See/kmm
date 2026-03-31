package io.github.letsee

data class Configuration(
    var isMockEnabled: Boolean,
    var shouldCutBaseURLFromURLsTitle: Boolean,
    var baseURL: String
) {
    constructor(baseURL: String, isMockEnabled: Boolean, shouldCutBaseURLFromURLsTitle: Boolean) :
            this(isMockEnabled, shouldCutBaseURLFromURLsTitle, baseURL)

    companion object {
        val default = Configuration(
            "https://letsee.com",
            isMockEnabled = false,
            shouldCutBaseURLFromURLsTitle = false
        )
    }
}