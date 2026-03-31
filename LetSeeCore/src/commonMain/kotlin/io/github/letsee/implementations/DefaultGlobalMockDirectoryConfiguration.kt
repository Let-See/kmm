package io.github.letsee.implementations

import kotlinx.serialization.Serializable

@Serializable
data class DefaultGlobalMockDirectoryConfiguration(val maps: List<Map>): GlobalMockDirectoryConfiguration {
    @Serializable
    data class Map(var folder: String, var to: String) {
        init {
            this.folder = folder.lowercase()
            this.to = to.lowercase()
        }
    }

   override fun hasMap(forRelativePth: String): Map? = this.maps.firstOrNull {
       forRelativePth.startsWith(it.folder.mockKeyNormalised())
   }
}

interface GlobalMockDirectoryConfiguration {
    fun hasMap(forRelativePth: String): DefaultGlobalMockDirectoryConfiguration.Map?
    companion object {
        const val GLOBAL_CONFIG_FILE_NAME = "ls.global.json"
    }
}

expect fun GlobalMockDirectoryConfiguration.Companion.exists(inDirectory: String): DefaultGlobalMockDirectoryConfiguration?