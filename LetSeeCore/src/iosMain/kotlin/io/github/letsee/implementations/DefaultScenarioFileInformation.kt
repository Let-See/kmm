@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.letsee.implementations

import io.github.letsee.interfaces.ScenarioFileInformationProcessor
import io.github.letsee.models.ScenarioFileInformation
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSPropertyListSerialization
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.lastPathComponent
import platform.Foundation.objectEnumerator
import platform.Foundation.valueForKey

actual class DefaultScenarioFileInformation : ScenarioFileInformationProcessor {
    actual override fun process(filePath: String): ScenarioFileInformation? {
        val displayName: String = NSURL.fileURLWithPath(filePath).lastPathComponent
            ?: return null
        val displayNameClean = displayName.removeSuffix(".plist")

        val data = NSData.dataWithContentsOfFile(filePath)
        if (data == null) {
            println("[LetSee] Warning: Could not read plist data at $filePath")
            return ScenarioFileInformation(displayNameClean, emptyList())
        }

        val plist = NSPropertyListSerialization.propertyListFromData(data, 0UL, null, null) as? NSDictionary
        if (plist == null) {
            println("[LetSee] Warning: plist at $filePath root is not NSDictionary — skipping scenario")
            return ScenarioFileInformation(displayNameClean, emptyList())
        }

        val stepsArray = plist.objectForKey("steps") as? NSArray
        if (stepsArray == null) {
            println("[LetSee] Warning: plist at $filePath is missing 'steps' array — skipping scenario")
            return ScenarioFileInformation(displayNameClean, emptyList())
        }

        val stepEnumerator = stepsArray.objectEnumerator()
        val resultSteps = mutableListOf<ScenarioFileInformation.Step>()
        var nextObj = stepEnumerator.nextObject()
        while (nextObj != null) {
            val step = nextObj as? NSDictionary
            if (step != null) {
                val folder = step.valueForKey("folder") as? String
                val fileName = step.valueForKey("fileName") as? String
                if (folder != null && fileName != null) {
                    resultSteps.add(ScenarioFileInformation.Step(folder, fileName))
                } else {
                    println("[LetSee] Warning: Skipping malformed step in plist at $filePath — missing 'folder' or 'fileName'")
                }
            } else {
                println("[LetSee] Warning: Skipping non-dictionary step entry in plist at $filePath")
            }
            nextObj = stepEnumerator.nextObject()
        }

        return ScenarioFileInformation(displayNameClean, resultSteps)
    }
}