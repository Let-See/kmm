package io.github.letsee.implementations

import android.content.Context
import android.content.res.AssetManager
import io.github.letsee.interfaces.DirectoryFilesFetcher
import java.io.IOException

actual class DefaultDirectoryFilesFetcher: DirectoryFilesFetcher {
    actual override fun getFiles(path: String, fileType: String): Map<String, List<String>> {
//        val result = mutableMapOf<String, MutableList<String>>()
//        val root = File(path)
//
//        for (file in root.walkTopDown()) {
//            if (file.isFile && file.extension == fileType) {
//                val dir = file.parentFile?.toString()?.let { dir ->
//                    val pathList = result.getOrDefault(dir, mutableListOf())
//                    pathList.add(file.toString())
//                    result[dir] = pathList
//                }
//            }
//        }
//
//        return result

        return androidContext?.let { listAssetFiles(it, path) } ?: emptyMap()
    }
    private fun listAssetFiles(context: Context, path: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        val assetManager: AssetManager = context.assets

        fun processPath(path: String) {
            val files = assetManager.list(path)
            files?.let {
                for (file in it) {
                    val newPath = "$path/$file"
                    val stream = try {
                        assetManager.open(newPath)
                    } catch (e: IOException) {
                        null
                    }
                    stream?.let {
                        // It's a file
                        println("File: $newPath")
                        it.close()
                        val dir = newPath.substringBeforeLast("/")
                        val pathList = result.getOrElse(dir) { mutableListOf() }
                        pathList.add(newPath)
                        result[dir] = pathList
                    } ?: run {
                        // It's a folder
                        processPath(newPath)
                    }
                }
            }
        }

        processPath(path)
        return result
    }

}