package io.github.letsee

data class Configuration(
    var isMockEnabled: Boolean,
    var shouldCutBaseURLFromURLsTitle: Boolean,
    var baseURL: String
) {
    companion object {
        val default = Configuration(
            isMockEnabled = false,
            shouldCutBaseURLFromURLsTitle = false,
            baseURL = "https://letsee.com"
        )
    }
}